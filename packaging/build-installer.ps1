# Builds the Windows installer (dist\RobocodeChampManager-<version>.exe).
# Needs JDK 21 (jpackage, jdeps), Maven and WiX Toolset 3 (jpackage uses it for the .exe).
# Usage (in the project folder): powershell -ExecutionPolicy Bypass -File packaging\build-installer.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

foreach ($tool in 'java', 'jpackage', 'jlink', 'jdeps', 'mvn') {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "$tool not found, install a JDK 21, Maven and WiX Toolset 3" }
}

[xml]$pom = Get-Content pom.xml
$version = $pom.project.version
$jar = "target\RobocodeChampManager-$version-all.jar"

Write-Host "Building $jar..."
mvn -B -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven failed" }

# The modules the program needs, found by jdeps, plus the ones it loads by reflection
$found = (& jdeps --print-module-deps --ignore-missing-deps $jar).Trim()
$modules = @($found.Split(',')) + 'java.desktop', 'java.naming', 'java.management', 'java.sql', 'java.xml', 'java.prefs', 'java.logging', 'java.net.http', 'jdk.crypto.ec', 'jdk.unsupported'
$modules = ($modules | Where-Object { $_ } | Sort-Object -Unique) -join ','

$stage = "packaging\build\input"
$runtime = "packaging\build\runtime"
Remove-Item -Recurse -Force packaging\build, dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $stage, dist | Out-Null
Copy-Item $jar "$stage\RobocodeChampManager.jar"

# A runtime with the java launcher too: the battles run java from JAVA_HOME (see RobocodeStarter\start.bat)
jlink --add-modules $modules --strip-debug --no-header-files --no-man-pages --output $runtime
if ($LASTEXITCODE -ne 0) { throw "jlink failed" }

jpackage --type exe `
    --name RobocodeChampManager `
    --app-version $version `
    --vendor "André Furlan" `
    --description "Gerenciador de campeonatos de robocode" `
    --input $stage `
    --main-jar RobocodeChampManager.jar `
    --main-class Launcher `
    --runtime-image $runtime `
    --app-content RobocodeStarter `
    --icon packaging\icons\app.ico `
    --dest dist `
    --win-menu --win-shortcut --win-dir-chooser `
    --win-upgrade-uuid 429BCC16-F699-4DFA-9C1A-21CFFD9EFAAC
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Write-Host "Done. Output in dist:"
Get-ChildItem dist
