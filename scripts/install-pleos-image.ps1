$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot
$toolRoot=Join-Path $projectRoot '.tools'
$sdkRoot=Join-Path $toolRoot 'android-sdk'
$archivePath=Join-Path $toolRoot 'pleos-image.zip'
if (!(Test-Path $archivePath)) {
    curl.exe --fail -L 'https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp_car_emulator.zip' -o $archivePath
    if ($LASTEXITCODE) { throw 'Pleos image download failed' }
}
$target=Join-Path $sdkRoot 'system-images\android-34\pleos_car_emulator\x86_64'
if (!(Test-Path $target)) {
    Expand-Archive -LiteralPath $archivePath -DestinationPath "$toolRoot\pleos-unpack" -Force
    $source=(Resolve-Path -LiteralPath "$toolRoot\pleos-unpack\crp_avd_out_x86_64").Path
    $resolvedTarget=[IO.Path]::GetFullPath($target)
    if (!$source.StartsWith($toolRoot,[StringComparison]::OrdinalIgnoreCase) -or !$resolvedTarget.StartsWith($sdkRoot,[StringComparison]::OrdinalIgnoreCase)) { throw 'Image paths must remain within the local tool directory' }
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    Move-Item -LiteralPath $source -Destination $resolvedTarget
}
Write-Output 'Pleos image extracted'
Get-Content "$target\source.properties"
