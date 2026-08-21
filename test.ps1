#Requires -Version 5.1
<#
Runs the JUnit test suite with JDK 25, in-workspace via the Maven Wrapper (./mvnw).
No worktree needed: the source compiles cleanly on JDK 25 (org.openjdk.nashorn import committed).
#>
# Continue (not Stop): native tools (mvn/java) print benign warnings to stderr; we check $LASTEXITCODE explicitly.
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$jdk = "$env:USERPROFILE\scoop\apps\temurin25-jdk\current"
if (-not (Test-Path "$jdk\bin\javac.exe")) { throw "JDK 25 not found at $jdk" }

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

& "$PSScriptRoot\mvnw.cmd" test @args
if ($LASTEXITCODE -ne 0) { throw "Tests failed ($LASTEXITCODE)." }
Write-Host "Tests OK"
