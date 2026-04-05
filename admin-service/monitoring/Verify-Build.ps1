param(
    [Parameter(Mandatory = $true)]
    [string]$WorkingDirectory,

    [Parameter(Mandatory = $false)]
    [string]$BuildCommand = 'mvn -DskipTests compile',

    [Parameter(Mandatory = $false)]
    [string]$OutputFile = 'monitoring/reports/build-verify.txt'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $WorkingDirectory)) {
    throw "Working directory not found: $WorkingDirectory"
}

Push-Location $WorkingDirectory
try {
    $result = Invoke-Expression $BuildCommand 2>&1 | Out-String
    $result | Set-Content -Path $OutputFile -Encoding UTF8
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed. See $OutputFile"
        exit $LASTEXITCODE
    }

    Write-Host "Build passed. See $OutputFile"
    exit 0
}
finally {
    Pop-Location
}
