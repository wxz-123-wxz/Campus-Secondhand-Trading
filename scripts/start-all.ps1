param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root 'logs'

if (!(Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

function Stop-PortProcess {
    param([int[]]$Ports)
    $pids = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $Ports -contains $_.LocalPort } |
        Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($pidValue in $pids) {
        $proc = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($proc) {
            Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped old process: PID=$pidValue Name=$($proc.ProcessName)"
        }
    }
}

function Test-BootJar {
    param([string]$JarPath)

    if (!(Test-Path $JarPath)) {
        return $false
    }

    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
        try {
            $manifestEntry = $zip.GetEntry('META-INF/MANIFEST.MF')
            if (!$manifestEntry) {
                return $false
            }
            $reader = New-Object System.IO.StreamReader($manifestEntry.Open())
            try {
                $manifest = $reader.ReadToEnd()
                return ($manifest -match 'Start-Class:' -and $manifest -match 'Main-Class:')
            } finally {
                $reader.Dispose()
            }
        } finally {
            $zip.Dispose()
        }
    } catch {
        return $false
    }
}

function Get-ServiceJar {
    param([string]$Service)

    return Get-ChildItem -Path (Join-Path $root "$Service\target") -Filter "$Service-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Ensure-ServiceJar {
    param([string]$Service)

    $jar = Get-ServiceJar -Service $Service
    if (!$jar -or !(Test-BootJar -JarPath $jar.FullName)) {
        Write-Host "Packaging $Service because its jar is missing or not executable..."
        mvn -q -pl $Service -am -DskipTests package
        $jar = Get-ServiceJar -Service $Service
    }

    if (!$jar -or !(Test-BootJar -JarPath $jar.FullName)) {
        throw "Executable jar still not found for $Service. Please check Maven build output."
    }

    return $jar
}

function Wait-Port {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn) {
            Write-Host "Port $Port is ready."
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Port $Port did not start within $TimeoutSeconds seconds. Check logs folder."
}

function Wait-GatewayApi {
    param([int]$TimeoutSeconds = 120)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/products' -TimeoutSec 8
            if ($response.StatusCode -eq 200) {
                $body = '{"username":"admin","password":"123456"}'
                $login = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/auth/login' -Method POST -Body $body -ContentType 'application/json' -TimeoutSec 8
                if ($login.StatusCode -eq 200) {
                    Write-Host 'Gateway product and login APIs are ready.'
                    return
                }
            }
        } catch {
        }
        Start-Sleep -Seconds 3
    }
    throw 'Gateway APIs are not ready. Check logs folder.'
}

function Wait-NacosReady {
    param([int]$TimeoutSeconds = 180)

    Wait-Port -Port 8848 -TimeoutSeconds $TimeoutSeconds
    Wait-Port -Port 9848 -TimeoutSeconds $TimeoutSeconds

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8848/nacos/v1/console/health/readiness' -TimeoutSec 8
            if ($response.StatusCode -eq 200 -and $response.Content.Trim() -eq 'OK') {
                Write-Host 'Nacos is ready.'
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 3
    }
    throw 'Nacos is not ready. Please check Docker Desktop and the campus-nacos container.'
}

function Assert-PortsRunning {
    param([int[]]$Ports)

    $missing = @()
    foreach ($port in $Ports) {
        $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if (!$conn) {
            $missing += $port
        }
    }

    if ($missing.Count -gt 0) {
        throw ('These ports are not running: ' + ($missing -join ', ') + '. Check logs folder.')
    }
}

function Get-PythonCommand {
    $candidates = @(
        'D:\anaconda3\python.exe',
        'python'
    )

    foreach ($candidate in $candidates) {
        try {
            $result = & $candidate --version 2>&1
            if ($LASTEXITCODE -eq 0 -and $result -match 'Python') {
                return $candidate
            }
        } catch {
        }
    }

    throw 'Python was not found. Please install Python or Anaconda and add it to PATH.'
}

function Start-ServiceJar {
    param(
        [string]$Service,
        [int]$Port
    )

    $jar = Ensure-ServiceJar -Service $Service

    Start-Process -FilePath 'java' `
        -ArgumentList ('-jar "' + $jar.FullName + '"') `
        -WorkingDirectory $root `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir "$Service.out.log") `
        -RedirectStandardError (Join-Path $logDir "$Service.err.log")

    Write-Host "Started $Service on port $Port"
}

Set-Location $root

Write-Host 'Stopping old project processes...'
Stop-PortProcess -Ports 3000,8080,8081,8082,8083,8084

Write-Host 'Starting Redis, RabbitMQ, and Nacos...'
docker-compose up -d redis rabbitmq nacos

if ($Build) {
    Write-Host 'Building backend services...'
    mvn -q -DskipTests package
}

$env:MYSQL_HOST = 'localhost'
$env:MYSQL_PORT = '3306'
$env:MYSQL_USER = 'root'

$localEnvFile = Join-Path $root '.env.local'
if (Test-Path $localEnvFile) {
    . $localEnvFile
}

if (!$env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = '123456' }
$env:REDIS_HOST = 'localhost'
$env:RABBITMQ_HOST = 'localhost'
$env:NACOS_HOST = 'localhost'

Write-Host 'Waiting for infrastructure...'
Wait-Port -Port 3306 -TimeoutSeconds 60
Wait-Port -Port 6379 -TimeoutSeconds 120
Wait-Port -Port 5672 -TimeoutSeconds 120
Wait-NacosReady -TimeoutSeconds 180

$services = @(
    @{ Name = 'campus-trade-user'; Port = 8081 },
    @{ Name = 'campus-trade-product'; Port = 8082 },
    @{ Name = 'campus-trade-order'; Port = 8083 },
    @{ Name = 'campus-trade-payment'; Port = 8084 },
    @{ Name = 'campus-trade-gateway'; Port = 8080 }
)

foreach ($svc in $services) {
    Start-ServiceJar -Service $svc.Name -Port $svc.Port
    Start-Sleep -Seconds 2
}

Write-Host 'Starting frontend static server on port 3000...'
$pythonCommand = Get-PythonCommand
Start-Process -FilePath $pythonCommand `
    -ArgumentList '-m http.server 3000' `
    -WorkingDirectory (Join-Path $root 'campus-trade-frontend') `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir 'frontend.out.log') `
    -RedirectStandardError (Join-Path $logDir 'frontend.err.log')

Write-Host ''
Write-Host 'Waiting for all ports and gateway API...'
foreach ($svc in $services) {
    Wait-Port -Port $svc.Port
}
Wait-Port -Port 3000
Assert-PortsRunning -Ports 3000,8080,8081,8082,8083,8084
Wait-GatewayApi

Write-Host ''
Write-Host 'All services are ready.'
Write-Host 'Website:  http://localhost:3000/index.html'
Write-Host 'Nacos:    http://localhost:8848/nacos'
Write-Host 'RabbitMQ: http://localhost:15672'
Write-Host ''
Write-Host 'Check status: scripts\status.ps1'
