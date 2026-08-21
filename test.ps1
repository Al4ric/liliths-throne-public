#Requires -Version 5.1
<#
Runs the JUnit test suite with JDK 17, in-workspace via the Maven Wrapper (./mvnw).
No worktree needed: the source compiles cleanly on JDK 17 (org.openjdk.nashorn import committed).
#>
# Continue (not Stop): native tools (mvn/java) print benign warnings to stderr; we check $LASTEXITCODE explicitly.
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$jdk17 = "$env:USERPROFILE\scoop\apps\temurin17-jdk\current"
if (-not (Test-Path "$jdk17\bin\javac.exe")) { throw "JDK 17 not found at $jdk17" }

$env:JAVA_HOME = $jdk17
$env:Path = "$jdk17\bin;$env:Path"

& "$PSScriptRoot\mvnw.cmd" test @args
if ($LASTEXITCODE -ne 0) { throw "Tests failed ($LASTEXITCODE)." }
Write-Host "Tests OK"
