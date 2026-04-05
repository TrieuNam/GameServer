param(
    [Parameter(Mandatory = $true)]
    [string]$PromptFile,

    [Parameter(Mandatory = $false)]
    [string]$OutputFile = "monitoring/reports/copilot-cli-output.txt"
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $PromptFile)) {
    throw "Prompt file not found: $PromptFile"
}

$promptText = Get-Content $PromptFile -Raw

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    "GitHub CLI (gh) is not installed. Prompt prepared only.`n`nPrompt file: $PromptFile" | Set-Content -Path $OutputFile -Encoding UTF8
    Write-Host "gh not found. Prompt saved for manual use."
    exit 2
}

$result = & gh copilot -p $promptText 2>&1 | Out-String
$result | Set-Content -Path $OutputFile -Encoding UTF8

if ($LASTEXITCODE -ne 0) {
    Write-Host "Copilot CLI command failed. See $OutputFile"
    exit $LASTEXITCODE
}

Write-Host "Copilot output saved to $OutputFile"
exit 0
