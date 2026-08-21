#Requires -Version 5.1
<#
Builds the shaded release JAR with JDK 25, in-workspace via the Maven Wrapper (./mvnw).

No worktree needed anymore: the Nashorn import is committed as org.openjdk.nashorn and the
antrun import-swap is gone, so the source compiles cleanly on JDK 25 (VS Code's Java language
server no longer writes a broken "poison" class).
#>
# Continue (not Stop): native tools (mvn/java) print benign warnings to stderr; we check $LASTEXITCODE explicitly.
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$jdk = "$env:USERPROFILE\scoop\apps\temurin25-jdk\current"
if (-not (Test-Path "$jdk\bin\javac.exe")) { throw "JDK 25 not found at $jdk" }

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

& "$PSScriptRoot\mvnw.cmd" clean package -DskipTests @args
if ($LASTEXITCODE -ne 0) { throw "Maven build failed ($LASTEXITCODE)." }

$outDir = Get-ChildItem "target" -Directory | Where-Object { $_.Name -like 'Lilith*(*)' } | Select-Object -First 1
$jar    = Get-ChildItem "$($outDir.FullName)\*.jar" | Select-Object -First 1
Write-Host "Build OK -> $($jar.FullName)"
