# Installs, if missing, the tools packaging\build-installer.ps1 needs, as portable copies in C:\rcm-tools:
# JDK 21 (Temurin), Maven and WiX Toolset 3. Nothing is installed in Windows itself; after it, put these folders in the PATH:
# C:\rcm-tools\jdk\bin, C:\rcm-tools\maven\bin and C:\rcm-tools\wix (and set JAVA_HOME to C:\rcm-tools\jdk).
# Usage: powershell -ExecutionPolicy Bypass -File packaging\setup-windows-tools.ps1
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$tools = 'C:\rcm-tools'
New-Item -ItemType Directory -Force $tools | Out-Null

# Downloads the zip and leaves its content in $tools\$name; the zip may have one folder around everything
function Install-Tool($name, $check, $url, [switch]$flat) {
    if (Test-Path "$tools\$name\$check") { Write-Host "$name already there"; return }
    Write-Host "Downloading $name..."
    $zip = "$tools\$name.zip"
    $part = "$tools\$name.part"
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zip
    Remove-Item -Recurse -Force $part, "$tools\$name" -ErrorAction SilentlyContinue
    Expand-Archive -Path $zip -DestinationPath $part -Force
    $inner = @(Get-ChildItem $part)
    if (-not $flat -and $inner.Count -eq 1 -and $inner[0].PSIsContainer) {
        Move-Item $inner[0].FullName "$tools\$name"
        Remove-Item -Recurse -Force $part
    } else {
        Move-Item $part "$tools\$name"
    }
    Remove-Item $zip
    if (-not (Test-Path "$tools\$name\$check")) { throw "$name was not installed: $check not found" }
}

Install-Tool 'jdk' 'bin\jpackage.exe' 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'
Install-Tool 'maven' 'bin\mvn.cmd' 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip'
Install-Tool 'wix' 'candle.exe' 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip' -flat
Write-Host 'Tools ready in C:\rcm-tools'
