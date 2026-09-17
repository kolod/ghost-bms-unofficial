<#
.SYNOPSIS
    Builds the debug APK and serves it over plain HTTP so it can be downloaded
    directly on a phone that isn't plugged into this PC (e.g. via kolod.mooo.com).

.PARAMETER Port
    Local/public port to listen on. Must match whatever your router forwards to
    this machine for kolod.mooo.com. Defaults to 80.

.PARAMETER SkipBuild
    Skip the Gradle build and just (re-)serve whatever APK is already in the
    output directory.

.NOTES
    Serves ONLY a single copied file from a throwaway directory (%TEMP%\bms-apk-serve),
    never the project tree. Press Ctrl+C to stop the server when the download is done —
    it stays publicly reachable on the configured port for as long as it runs.
#>
param(
    [int]$Port = 80,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if (-not $SkipBuild) {
    & "$root\gradlew.bat" ":app:assembleDebug"
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)" }
}

$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "APK not found at $apk -- build it first (omit -SkipBuild)" }

$serveDir = Join-Path $env:TEMP "bms-apk-serve"
New-Item -ItemType Directory -Force -Path $serveDir | Out-Null
Get-ChildItem $serveDir -Filter "*.apk" | Remove-Item -Force
$dest = Join-Path $serveDir "bms-monitor.apk"
Copy-Item $apk $dest -Force

Write-Host "Serving $dest"
Write-Host "Phone URL: http://kolod.mooo.com/bms-monitor.apk (if router forwards port $Port here)"
Write-Host "Ctrl+C to stop."

python -m http.server $Port --directory $serveDir --bind 0.0.0.0
