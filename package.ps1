#Requires -Version 5.1
<#
Builds a self-contained, JDK-free distributable of Lilith's Throne with jpackage.

Output: dist\Lilith's Throne\ containing a native launcher (Lilith's Throne.exe), a bundled
Java 17 runtime, the shaded app jar, and the res/ assets. The recipient does NOT need Java
installed — just unzip and run the exe. Launch it from its own folder (double-click), since
the game reads res/ and writes data/ relative to the working directory.

Usage:
  ./package.ps1           # reuse the existing shaded jar, produce the app-image
  ./package.ps1 -Rebuild  # clean-rebuild the jar first (recommended for a real release)
  ./package.ps1 -Zip      # also produce a single shareable .zip

macOS / Linux: jpackage cannot cross-compile. Run the same jpackage command ON that OS
(swap --add-modules stays the same; the launcher becomes a .app/.dmg on macOS, or a
directory/.deb/.rpm on Linux). See .github/instructions/build.instructions.md.
#>
param(
    [switch]$Rebuild,
    [switch]$Zip
)
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$jdk17 = "$env:USERPROFILE\scoop\apps\temurin17-jdk\current"
if (-not (Test-Path "$jdk17\bin\jpackage.exe")) { throw "jpackage (JDK 17) not found at $jdk17" }

# 1. Ensure a shaded jar (rebuild on demand or if missing).
$jar = Get-ChildItem "target\Lilith's Throne (win)\*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($Rebuild -or -not $jar) {
    & "$PSScriptRoot\build.ps1"
    if ($LASTEXITCODE -ne 0) { throw "build.ps1 failed ($LASTEXITCODE)." }
    $jar = Get-ChildItem "target\Lilith's Throne (win)\*.jar" | Select-Object -First 1
}
if (-not $jar) { throw "No shaded jar found; run ./build.ps1 first." }

# 2. Stage only the jar for jpackage --input (it copies the whole input dir into app/).
$stage = "target\jpackage-input"
Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $stage | Out-Null
Copy-Item $jar.FullName "$stage\" -Force

# 3. Build the self-contained app-image (native launcher + bundled JRE with just the needed modules).
#    Module set = jdeps output for the shaded jar, plus java.xml (JAXP), java.datatransfer
#    (clipboard) and locale/charset data that jdeps can't see (used reflectively).
$dist = "dist"
$appDir = "$dist\Lilith's Throne"
Remove-Item $appDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$modules = "java.base,java.desktop,java.datatransfer,java.logging,java.management,java.naming," +
           "java.net.http,java.prefs,java.rmi,java.scripting,java.xml,jdk.charsets,jdk.dynalink," +
           "jdk.jfr,jdk.jsobject,jdk.localedata,jdk.unsupported,jdk.xml.dom"

$env:JAVA_HOME = $jdk17
& "$jdk17\bin\jpackage.exe" `
    --type app-image `
    --name "Lilith's Throne" `
    --app-version "0.4.11" `
    --vendor "Innoxia" `
    --input $stage `
    --main-jar $jar.Name `
    --main-class com.lilithsthrone.Launcher `
    --add-modules $modules `
    --java-options "-XX:+UseParallelGC" `
    --dest $dist
if ($LASTEXITCODE -ne 0) { throw "jpackage failed ($LASTEXITCODE)." }

# 4. Bundle the external res/ assets next to the launcher (read relative to the app dir at runtime).
robocopy "res" "$appDir\res" /MIR /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy of res failed ($LASTEXITCODE)." }
$global:LASTEXITCODE = 0  # robocopy returns 1 on success (files copied); don't leak that as failure.

Write-Host "Standalone (no JDK needed) -> $appDir\Lilith's Throne.exe"

if ($Zip) {
    $version = ($jar.BaseName -replace '.*-', '')
    $zipPath = "$dist\LilithsThrone-$version-win.zip"
    Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
    Write-Host "Zipping (this can take a while)..."
    Compress-Archive -Path $appDir -DestinationPath $zipPath -CompressionLevel Optimal
    Write-Host "Shareable zip -> $zipPath"
}
