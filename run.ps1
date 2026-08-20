#Requires -Version 5.1
# Launches the most recently built Lilith's Throne JAR with JDK 17.
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$jdk17 = "$env:USERPROFILE\scoop\apps\temurin17-jdk\current"
if (-not (Test-Path "$jdk17\bin\javaw.exe")) { throw "JDK 17 not found at $jdk17" }

$jar = Get-ChildItem -Path 'target' -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "No built JAR found under 'target'. Run build.ps1 first." }

Start-Process -FilePath "$jdk17\bin\javaw.exe" `
    -ArgumentList '-jar', "`"$($jar.FullName)`"" `
    -WorkingDirectory $jar.Directory.FullName
Write-Host "Launched: $($jar.Name)"
