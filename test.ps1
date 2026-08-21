#Requires -Version 5.1
<#
Runs the JUnit test suite with JDK 17 in an isolated worktree.

Same isolation rationale as build.ps1: VS Code's Eclipse JDT compiles UtilText.java
(JDK-8-only `jdk.nashorn` import) into target/classes and the antrun plugin rewrites that
import in place. Building/testing in a separate worktree keeps the workspace sources clean
and avoids the JDT "poison class" problem.
#>
$ErrorActionPreference = 'Stop'
$repo  = $PSScriptRoot
$build = "$env:USERPROFILE\lt-build"        # isolated build/test dir, outside the workspace
$jdk17 = "$env:USERPROFILE\scoop\apps\temurin17-jdk\current"
$mvn   = "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"

if (-not (Test-Path "$jdk17\bin\javac.exe")) { throw "JDK 17 not found at $jdk17" }
if (-not (Test-Path $mvn))                    { throw "Maven not found at $mvn" }

# 1. Ensure an isolated worktree of the repo exists.
if (-not (Test-Path "$build\pom.xml")) {
    git -C $repo worktree add --detach $build HEAD
}

# 2. Sync current sources + tests + resources into the worktree (fast incremental mirror).
foreach ($dir in 'src', 'test', 'res') {
    if (Test-Path "$repo\$dir") {
        robocopy "$repo\$dir" "$build\$dir" /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
    }
}
Copy-Item "$repo\pom.xml" "$build\pom.xml" -Force

# 3. Run tests with JDK 17 (JavaFX + Nashorn come from Maven Central).
$env:JAVA_HOME = $jdk17
$env:Path = "$jdk17\bin;" + (Split-Path $mvn) + ";$env:Path"
Push-Location $build
try { & $mvn test @args } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw "Tests failed ($LASTEXITCODE)." }
Write-Host "Tests OK"
