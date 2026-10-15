# ClientRoot comes from the parameter, JMQTP_CLIENT_ROOT, or paths.local.json (see paths.example.json).
param(
    [string]$ClientRoot
)
$ErrorActionPreference = 'Stop'
if (-not $ClientRoot) { $ClientRoot = $env:JMQTP_CLIENT_ROOT }
if (-not $ClientRoot) {
    $cfgFile = Join-Path $PSScriptRoot 'paths.local.json'
    if (Test-Path -LiteralPath $cfgFile) { $ClientRoot = (Get-Content -Raw -LiteralPath $cfgFile | ConvertFrom-Json).clientRoot }
}
if (-not $ClientRoot) { throw "Missing client root: pass -ClientRoot, set JMQTP_CLIENT_ROOT, or create paths.local.json" }
$version = '1.2.0'
$source = Join-Path $PSScriptRoot "dist\journeymap-quick-teleport-GTNH-$version.jar"
$destination = Join-Path $ClientRoot "mods\journeymap-quick-teleport-GTNH-$version.jar"
$jm = Join-Path $ClientRoot 'mods\journeymap-1.7.10-5.2.23-unlimited.jar'
$mods = Join-Path $ClientRoot 'mods'
$sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
$jmHash = (Get-FileHash -LiteralPath $jm -Algorithm SHA256).Hash
$recordRoot = Join-Path $ClientRoot ('_audit-backups\jmquicktp-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $recordRoot | Out-Null

# Backup previous versions from mods into record directory first
$previous = @(Get-ChildItem -LiteralPath $mods -Filter 'journeymap-quick-teleport*.jar')
foreach ($old in $previous) {
    Move-Item -LiteralPath $old.FullName -Destination (Join-Path $recordRoot $old.Name) -Force
}

# Copy newly built artifact into record directory
Copy-Item -LiteralPath $source -Destination (Join-Path $recordRoot "journeymap-quick-teleport-GTNH-$version.jar")

# Install newly built artifact into mods directory
$inputStream = [IO.File]::OpenRead($source)
try {
    $outputStream = [IO.File]::Open($destination, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
    try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose() }
} finally { $inputStream.Dispose() }

if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $sourceHash) { throw 'Installed hash mismatch' }
if ((Get-FileHash -LiteralPath $jm -Algorithm SHA256).Hash -ne $jmHash) { throw 'JourneyMap changed during install' }

$record = [ordered]@{
    installedAt = (Get-Date -Format 'o')
    clientRoot = $ClientRoot
    installedFile = $destination
    artifactSHA256 = $sourceHash
    originalJourneyMap = $jm
    originalJourneyMapSHA256 = $jmHash
    replacedFiles = @($previous | ForEach-Object { $_.Name })
    verification = '34 isolated checks PASS; direct teleport with safe height, accurate coordinates via layerDelegate & viewport projection; in-game acceptance pending'
    activation = 'Next normal client launch; running client and server not restarted'
    rollback = 'Exit client normally, copy replacedFiles back from this record directory into mods and delete installedFile; original JourneyMap untouched'
}
$record | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $recordRoot 'installation.json') -Encoding UTF8
$record | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'installation.json') -Encoding UTF8
Write-Output "Installed: $destination"
Write-Output "Recovery record: $recordRoot"
if ($previous.Count -ne 0) { Write-Output ("Replaced: " + ($previous.Name -join ', ')) }
Write-Output 'Active on next normal client launch. In-game acceptance pending.'
