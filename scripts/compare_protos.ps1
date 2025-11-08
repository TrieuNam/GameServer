<#
PowerShell script to compare .proto files between two folders (common-lib and common/proto)

Usage examples:
.
# Dry run report
PS> .\scripts\compare_protos.ps1 -LibPath .\common-lib\src\main\proto -CommonProtoPath .\common\proto\src\main\proto -ReportPath docs\proto-compare-report.csv

# Replace differing/missing files (use --Replace to actually copy files)
PS> .\scripts\compare_protos.ps1 -LibPath .\common-lib\src\main\proto -CommonProtoPath .\common\proto\src\main\proto -ReportPath docs\proto-compare-report.csv -Replace
#>
param(
    [Parameter(Mandatory=$false)] [string]$LibPath = ".\common-lib\src\main\proto",
    [Parameter(Mandatory=$false)] [string]$CommonProtoPath = ".\common\proto\src\main\proto",
    [Parameter(Mandatory=$false)] [string]$ReportPath = ".\docs\proto-compare-report.csv",
    [switch]$Replace
)

function Resolve-FullPath([string]$p) {
    return (Resolve-Path -LiteralPath $p -ErrorAction SilentlyContinue).ProviderPath
}

$libDir = Resolve-FullPath $LibPath
$commonDir = Resolve-FullPath $CommonProtoPath

if (-not $libDir) { Write-Error "LibPath '$LibPath' not found."; exit 2 }
if (-not $commonDir) { Write-Error "CommonProtoPath '$CommonProtoPath' not found."; exit 2 }

Write-Host "Lib path: $libDir"
Write-Host "Common proto path: $commonDir"

# gather files
$libFiles = Get-ChildItem -Path $libDir -Recurse -Filter *.proto | Where-Object { -not $_.PSIsContainer }
$commonFiles = Get-ChildItem -Path $commonDir -Recurse -Filter *.proto | Where-Object { -not $_.PSIsContainer }

# build lookup by filename
$commonIndex = @{}
foreach ($f in $commonFiles) { $commonIndex[$f.Name] = $f }

# prepare output
$report = @()
$diffDir = Join-Path $env:TEMP "proto-diffs"
if (Test-Path $diffDir) { Remove-Item -Recurse -Force $diffDir }
New-Item -ItemType Directory -Force -Path $diffDir | Out-Null

foreach ($lf in $libFiles) {
    $name = $lf.Name
    $entry = [PSCustomObject]@{
        FileName = $name
        LibPath = $lf.FullName
        CommonPath = $null
        Status = "Missing"
        LibHash = ""
        CommonHash = ""
        DiffFile = ""
    }

    $libHash = (Get-FileHash -Path $lf.FullName -Algorithm SHA1).Hash
    $entry.LibHash = $libHash

    if ($commonIndex.ContainsKey($name)) {
        $cf = $commonIndex[$name]
        $entry.CommonPath = $cf.FullName
        $commonHash = (Get-FileHash -Path $cf.FullName -Algorithm SHA1).Hash
        $entry.CommonHash = $commonHash

        if ($libHash -eq $commonHash) {
            $entry.Status = "Identical"
        }
        else {
            $entry.Status = "Different"
            # produce diff using git if available, else use fc
            $diffOut = Join-Path $diffDir ($name + ".diff.txt")
            $git = Get-Command git -ErrorAction SilentlyContinue
            if ($git) {
                & git --no-pager diff --no-index -- "$($cf.FullName)" "$($lf.FullName)" > $diffOut 2>&1
            }
            else {
                # fallback to fc.exe (Windows)
                & fc "$($cf.FullName)" "$($lf.FullName)" > $diffOut 2>&1
            }
            $entry.DiffFile = $diffOut

            if ($Replace.IsPresent) {
                # backup existing common file before replace
                $backupDir = Join-Path $commonDir "backup_$(Get-Date -Format yyyyMMddHHmmss)"
                if (-not (Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir | Out-Null }
                Copy-Item -Path $cf.FullName -Destination $backupDir -Force
                Copy-Item -Path $lf.FullName -Destination $cf.FullName -Force
                $entry.Status = "Replaced"
            }
        }
    }
    else {
        # missing in common, produce note; optionally copy
        if ($Replace.IsPresent) {
            Copy-Item -Path $lf.FullName -Destination (Join-Path $commonDir $name) -Force
            $entry.CommonPath = Join-Path $commonDir $name
            $entry.Status = "Copied"
        }
    }

    $report += $entry
}

# also detect files in common not present in lib
$libNames = $libFiles.Name
foreach ($cf in $commonFiles) {
    if (-not ($libNames -contains $cf.Name)) {
        $report += [PSCustomObject]@{
            FileName = $cf.Name
            LibPath = ""
            CommonPath = $cf.FullName
            Status = "CommonOnly"
            LibHash = ""
            CommonHash = (Get-FileHash -Path $cf.FullName -Algorithm SHA1).Hash
            DiffFile = ""
        }
    }
}

# export CSV
$report | Export-Csv -Path $ReportPath -NoTypeInformation -Encoding UTF8

Write-Host "Report written to: $ReportPath"
Write-Host "Diffs (if any) are in: $diffDir"

# summary
$summary = $report | Group-Object -Property Status | ForEach-Object { "$($_.Name): $($_.Count)" }
Write-Host "Summary:`n" ($summary -join "`n")

if ($Replace.IsPresent) { Write-Host "Replace mode: changes applied to $commonDir (backups stored). Remember to review and commit." }
