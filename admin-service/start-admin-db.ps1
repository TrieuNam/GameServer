# Start Admin Database Container
# Simple and clean PowerShell script

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  ADMIN DATABASE STARTER" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$CONTAINER = "local-admindb"
$PROMETHEUS_CONTAINER = "local-prometheus"
$GRAFANA_CONTAINER = "local-grafana"
$DOCKER_DIR = Join-Path $PSScriptRoot "..\docker"
$MONITORING_DIR = Join-Path $PSScriptRoot "monitoring"

# Check Docker
Write-Host "[1/7] Checking Docker..." -ForegroundColor Yellow
$null = docker ps 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Docker not running!" -ForegroundColor Red
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

# Remove old container if exists, then always create fresh
Write-Host ""
Write-Host "[2/7] Setting up Admin DB container..." -ForegroundColor Yellow
$oldContainer = docker ps -a --format "{{.Names}}" | Select-String -Pattern "^${CONTAINER}$"
if ($oldContainer) {
    Write-Host "  Removing old container..." -ForegroundColor Yellow
    docker stop $CONTAINER 2>&1 | Out-Null
    docker rm $CONTAINER 2>&1 | Out-Null
    Write-Host "  Old container removed" -ForegroundColor Green
}
Write-Host "  Creating container with init script..." -ForegroundColor Yellow
Push-Location $DOCKER_DIR
docker-compose -f docker-compose.local-full.yml up -d admindb
Pop-Location
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Failed to create container" -ForegroundColor Red
    exit 1
}
Write-Host "  OK" -ForegroundColor Green

# Start container
Write-Host ""
Write-Host "[3/7] Starting Admin DB container..." -ForegroundColor Yellow
$running = docker ps --format "{{.Names}}" | Select-String -Pattern "^${CONTAINER}$"
if (-not $running) {
    docker start $CONTAINER | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ERROR: Failed to start" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  OK" -ForegroundColor Green

# Wait for MySQL
Write-Host ""
Write-Host "[4/7] Waiting for MySQL..." -ForegroundColor Yellow
$ready = $false
for ($i = 1; $i -le 15; $i++) {
    Start-Sleep -Seconds 2
    docker exec $CONTAINER mysqladmin ping -h localhost -uroot -proot123 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $ready = $true
        Write-Host "  OK Ready! (${i}/15)" -ForegroundColor Green
        break
    }
    Write-Host "  Waiting (${i}/15)..." -ForegroundColor Gray
}

if (-not $ready) {
    Write-Host "  ERROR: Timeout" -ForegroundColor Red
    exit 1
}

# Ensure user permissions
Write-Host ""
Write-Host "[5/7] Setting up user permissions..." -ForegroundColor Yellow
$sql = @"
CREATE DATABASE IF NOT EXISTS game_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';
GRANT ALL PRIVILEGES ON game_admin.* TO 'tpnam'@'%';
FLUSH PRIVILEGES;
"@

docker exec $CONTAINER mysql -uroot -proot123 -e $sql 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  OK User permissions granted" -ForegroundColor Green
} else {
    Write-Host "  WARNING: Could not set permissions, but continuing..." -ForegroundColor Yellow
}


# Start Prometheus
Write-Host ""
Write-Host "[6/7] Starting Prometheus..." -ForegroundColor Yellow
$prometheusRunning = docker ps --format "{{.Names}}" | Select-String -Pattern "^${PROMETHEUS_CONTAINER}$"
if (-not $prometheusRunning) {
    # Create prometheus data directory
    $prometheusData = Join-Path $MONITORING_DIR "prometheus-data"
    if (-not (Test-Path $prometheusData)) {
        New-Item -ItemType Directory -Path $prometheusData -Force | Out-Null
    }
    
    # Get absolute path for volume mount
    $prometheusConfig = Join-Path $MONITORING_DIR "prometheus.yml"
    
    # Start Prometheus
    docker run -d --name $PROMETHEUS_CONTAINER `
        -p 9090:9090 `
        -v "${prometheusConfig}:/etc/prometheus/prometheus.yml" `
        -v "${prometheusData}:/prometheus" `
        --restart unless-stopped `
        prom/prometheus:latest `
        --config.file=/etc/prometheus/prometheus.yml `
        --storage.tsdb.path=/prometheus | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Prometheus started" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Failed to start Prometheus" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✅ Prometheus already running" -ForegroundColor Green
}

# Start Grafana
Write-Host ""
Write-Host "[7/7] Starting Grafana..." -ForegroundColor Yellow
$grafanaRunning = docker ps --format "{{.Names}}" | Select-String -Pattern "^${GRAFANA_CONTAINER}$"
if (-not $grafanaRunning) {
    # Create grafana data directory
    $grafanaData = Join-Path $MONITORING_DIR "grafana-data"
    if (-not (Test-Path $grafanaData)) {
        New-Item -ItemType Directory -Path $grafanaData -Force | Out-Null
    }
    
    # Start Grafana
    docker run -d --name $GRAFANA_CONTAINER `
        -p 3000:3000 `
        -v "${grafanaData}:/var/lib/grafana" `
        -e "GF_SECURITY_ADMIN_USER=admin" `
        -e "GF_SECURITY_ADMIN_PASSWORD=admin123" `
        -e "GF_USERS_ALLOW_SIGN_UP=false" `
        --restart unless-stopped `
        grafana/grafana:latest | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Grafana started" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Failed to start Grafana" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✅ Grafana already running" -ForegroundColor Green
}

# Success
Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  ADMIN SERVICES READY!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Admin Database:" -ForegroundColor White
Write-Host "  Container: $CONTAINER" -ForegroundColor Gray
Write-Host "  Database:  game_admin" -ForegroundColor Gray
Write-Host "  Port:      localhost:9088" -ForegroundColor Gray
Write-Host "  Root:      root / root123" -ForegroundColor Gray
Write-Host "  User:      tpnam / 121831" -ForegroundColor Gray
Write-Host ""
Write-Host "📈 Monitoring Stack:" -ForegroundColor White
Write-Host "  Prometheus: http://localhost:9090" -ForegroundColor Cyan
Write-Host "  Grafana:    http://localhost:3000 (admin/admin123)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Commands:" -ForegroundColor Yellow
Write-Host "  docker logs $CONTAINER" -ForegroundColor Gray
Write-Host "  docker logs $PROMETHEUS_CONTAINER" -ForegroundColor Gray
Write-Host "  docker logs $GRAFANA_CONTAINER" -ForegroundColor Gray
Write-Host ""
