$ErrorActionPreference = 'Stop'
$toolRoot = Join-Path (Split-Path $PSScriptRoot) '.tools'
New-Item -ItemType Directory -Force $toolRoot | Out-Null
curl.exe --fail -L -s 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -o "$toolRoot\jdk.zip"
if ($LASTEXITCODE) { throw 'JDK download failed' }
Expand-Archive -LiteralPath "$toolRoot\jdk.zip" -DestinationPath "$toolRoot\jdk" -Force
curl.exe --fail -L -s 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' -o "$toolRoot\android-cli.zip"
if ($LASTEXITCODE) { throw 'Android tools download failed' }
Expand-Archive -LiteralPath "$toolRoot\android-cli.zip" -DestinationPath "$toolRoot\android-unpack" -Force
New-Item -ItemType Directory -Force "$toolRoot\android-sdk\cmdline-tools" | Out-Null
if (!(Test-Path "$toolRoot\android-sdk\cmdline-tools\latest")) { Move-Item -LiteralPath "$toolRoot\android-unpack\cmdline-tools" -Destination "$toolRoot\android-sdk\cmdline-tools\latest" }
Write-Output 'JDK and Android command line tools ready'
