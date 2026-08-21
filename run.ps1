#Requires -Version 5.1
# Launches the most recently built Lilith's Throne JAR with JDK 25.
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$jdk = "$env:USERPROFILE\scoop\apps\temurin25-jdk\current"
if (-not (Test-Path "$jdk\bin\javaw.exe")) { throw "JDK 25 not found at $jdk" }

$jar = Get-ChildItem -Path 'target' -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "No built JAR found under 'target'. Run build.ps1 first." }

Start-Process -FilePath "$jdk\bin\javaw.exe" `
    -ArgumentList '--enable-native-access=ALL-UNNAMED', '-jar', "`"$($jar.FullName)`"" `
    -WorkingDirectory $jar.Directory.FullName
Write-Host "Launched: $($jar.Name)"
