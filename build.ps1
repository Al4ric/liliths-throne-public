#Requires -Version 5.1
<#
Builds Lilith's Throne reliably with JDK 17.

Why the isolation: VS Code's Java language server (Eclipse JDT) compiles
UtilText.java (which imports the JDK-8-only `jdk.nashorn`) into target/classes.
On JDK 17 that import can't resolve, so JDT writes a broken "poison" class that
Maven's shade step then packages -> the game builds but crashes at startup.
Building in a separate worktree that VS Code isn't watching avoids this entirely.
#>
$ErrorActionPreference = 'Stop'
$repo  = $PSScriptRoot
$build = "$env:USERPROFILE\lt-build"        # isolated build dir, outside the workspace
$jdk17 = "$env:USERPROFILE\scoop\apps\temurin17-jdk\current"
$mvn   = "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"

if (-not (Test-Path "$jdk17\bin\javac.exe")) { throw "JDK 17 not found at $jdk17" }
if (-not (Test-Path $mvn))                    { throw "Maven not found at $mvn" }

# 1. Ensure an isolated worktree of the repo exists.
if (-not (Test-Path "$build\pom.xml")) {
    git -C $repo worktree add --detach $build HEAD
}

# 2. Sync current sources + resources into the worktree (fast incremental mirror).
foreach ($dir in 'src', 'res') {
    robocopy "$repo\$dir" "$build\$dir" /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
}
Copy-Item "$repo\pom.xml" "$build\pom.xml" -Force
if (Test-Path "$repo\svgoConfig.yml") { Copy-Item "$repo\svgoConfig.yml" "$build\svgoConfig.yml" -Force }

# 3. Build with JDK 17 (JavaFX + Nashorn come from Maven Central).
$env:JAVA_HOME = $jdk17
$env:Path = "$jdk17\bin;" + (Split-Path $mvn) + ";$env:Path"
Push-Location $build
try { & $mvn clean package -DskipTests @args } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw "Maven build failed ($LASTEXITCODE)." }

# 4. Copy the freshly built JAR next to the workspace's res folder.
$outDir  = Get-ChildItem "$build\target" -Directory | Where-Object { $_.Name -like 'Lilith*(*)' } | Select-Object -First 1
$jar     = Get-ChildItem "$($outDir.FullName)\*.jar" | Select-Object -First 1
$destDir = "$repo\target\$($outDir.Name)"
New-Item -ItemType Directory -Force -Path $destDir | Out-Null
Copy-Item $jar.FullName -Destination $destDir -Force
Write-Host "Build OK -> $destDir\$($jar.Name)"
