$ports = 3000,8080,8081,8082,8083,8084

Write-Host 'Port status:'
foreach ($port in $ports) {
    $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        Write-Host ("  {0}: RUNNING PID={1} Name={2}" -f $port, $conn.OwningProcess, $proc.ProcessName)
    } else {
        Write-Host ("  {0}: STOPPED" -f $port)
    }
}

Write-Host ''
Write-Host 'Docker container status:'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
docker-compose ps redis rabbitmq nacos
