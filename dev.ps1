#Requires -Version 5.1
<#
Fast dev inner-loop for Lilith's Throne. Incrementally compiles ONLY changed sources and runs
the game straight from classes — no clean, no res/ copy, no shaded fat JAR. Runs in-workspace
via the Maven Wrapper (./mvnw); no worktree, since the source compiles cleanly on JDK 25.

Speed tricks:
  - OFFLINE compile (-o) avoids ~50s of remote metadata re-checks.
  - `compiler:compile` only (skips process-resources' big res/ copy and the shade step).
  - <useIncrementalCompilation>false</> in pom => per-file staleness (only changed files recompile).
  - Classpath is served from {target/classes, src, deps}: `src` provides internal fxml/css/svg,
    and the workspace root provides external res/ (the working directory), so nothing is copied.

Usage:
  ./dev.ps1            # incremental compile, launch (console logs via -Ddebug=true)
  ./dev.ps1 -NoLaunch  # compile only (no game window) — handy for timing
#>
param(
    [switch]$NoLaunch
)
# Continue (not Stop): native tools (mvn/java) print benign warnings to stderr; we check $LASTEXITCODE explicitly.
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$jdk = "$env:USERPROFILE\scoop\apps\temurin25-jdk\current"
if (-not (Test-Path "$jdk\bin\javac.exe")) { throw "JDK 25 not found at $jdk" }

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
$mvnw = "$PSScriptRoot\mvnw.cmd"

# 1. Cache the dependency classpath under target/; regenerate only when pom.xml changes.
$cpFile = "target\dev-classpath.txt"
$needCp = -not (Test-Path $cpFile) -or ((Get-Item 'pom.xml').LastWriteTime -gt (Get-Item $cpFile).LastWriteTime)
if ($needCp) {
    Write-Host "Resolving dependency classpath..."
    & $mvnw -q dependency:build-classpath "-Dmdep.outputFile=$cpFile" -DincludeScope=runtime
    if ($LASTEXITCODE -ne 0) { throw "Failed to resolve classpath ($LASTEXITCODE)." }
}
$deps = (Get-Content $cpFile -Raw).Trim()

# 2. Incremental offline compile. Falls back to online if offline fails (e.g. new dependency).
$sw = [System.Diagnostics.Stopwatch]::StartNew()
& $mvnw -o -q compiler:compile
if ($LASTEXITCODE -ne 0) {
    Write-Host "Offline compile failed; retrying online (dependencies may have changed)..."
    & $mvnw -q compiler:compile
    if ($LASTEXITCODE -ne 0) { throw "Compile failed ($LASTEXITCODE)." }
}
$sw.Stop()
Write-Host ("Incremental compile: {0:N1}s" -f $sw.Elapsed.TotalSeconds)

if ($NoLaunch) { return }

# 3. Run from classes. `src` serves internal resources; workspace root serves external res/.
& "$jdk\bin\java.exe" --enable-native-access=ALL-UNNAMED -Ddebug=true -cp "target\classes;src;$deps" com.lilithsthrone.Launcher
