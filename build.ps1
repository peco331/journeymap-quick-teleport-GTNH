# Paths come from paths.local.json (copy paths.example.json) or the JMQTP_* environment variables.
param(
    [string]$Jdk,
    [string]$ServerRoot,
    [string]$LibraryRoot
)
$ErrorActionPreference = 'Stop'
$cfgFile = Join-Path $PSScriptRoot 'paths.local.json'
$cfg = if (Test-Path -LiteralPath $cfgFile) { Get-Content -Raw -LiteralPath $cfgFile | ConvertFrom-Json } else { $null }
function Resolve-Path-Setting($value, $env, $key) {
    if ($value) { return $value }
    $fromEnv = [Environment]::GetEnvironmentVariable($env)
    if ($fromEnv) { return $fromEnv }
    if ($cfg -and $cfg.$key) { return $cfg.$key }
    throw "Missing setting '$key': pass a parameter, set $env, or create paths.local.json (see paths.example.json)"
}
$Jdk = Resolve-Path-Setting $Jdk 'JMQTP_JDK' 'jdk'
$ServerRoot = Resolve-Path-Setting $ServerRoot 'JMQTP_SERVER_ROOT' 'serverRoot'
$LibraryRoot = Resolve-Path-Setting $LibraryRoot 'JMQTP_LIBRARY_ROOT' 'libraryRoot'
$forge = Join-Path $ServerRoot 'forge-1.7.10-10.13.4.1614-1.7.10-universal.jar'
$launch = Join-Path $ServerRoot 'libraries\net\minecraft\launchwrapper\1.15\launchwrapper-1.15.jar'
$asm = Join-Path $LibraryRoot 'org\ow2\asm\asm\9.7.1\asm-9.7.1.jar'
$tree = Join-Path $LibraryRoot 'org\ow2\asm\asm-tree\9.7.1\asm-tree-9.7.1.jar'
$cp = "$forge;$launch;$asm;$tree"
$classes = Join-Path $PSScriptRoot 'build\classes'
$dist = Join-Path $PSScriptRoot 'dist'
# Rebuild from scratch so classes of deleted sources cannot leak into the jar.
if (Test-Path -LiteralPath $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
New-Item -ItemType Directory -Force $classes,$dist | Out-Null
$sources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& (Join-Path $Jdk 'bin\javac.exe') --release 8 -encoding UTF-8 -cp $cp -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$output = Join-Path $dist 'journeymap-quick-teleport-GTNH-1.2.0.jar'
& (Join-Path $Jdk 'bin\jar.exe') cfm $output (Join-Path $PSScriptRoot 'manifest.mf') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Get-FileHash -LiteralPath $output -Algorithm SHA256
