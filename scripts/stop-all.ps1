$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$ports = 3000,8080,8081,8082,8083,8084
$pids = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $ports -contains $_.LocalPort } |
    Select-Object -ExpandProperty OwningProcess -Unique

foreach ($pidValue in $pids) {
    $proc = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($proc) {
        Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped process: PID=$pidValue Name=$($proc.ProcessName)"
    }
}

Write-Host 'Stopping Redis, RabbitMQ, and Nacos...'
docker-compose stop redis rabbitmq nacos

Write-Host 'Project services stopped. MySQL was not stopped.'
