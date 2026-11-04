param([string]$Serial='emulator-5554')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot
$adb=Join-Path $projectRoot '.tools\android-sdk\platform-tools\adb.exe'
if (!(Test-Path $adb)) { $adb='adb' }
$booted=$false
for($attempt=0;$attempt -lt 120;$attempt++) {
    $boot=(& $adb -s $Serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
    if($boot -eq '1') { $booted=$true;break }
    Start-Sleep -Seconds 2
}
if(!$booted) { throw 'Emulator did not finish booting within 4 minutes' }
& $adb -s $Serial shell settings put global window_animation_scale 0
& $adb -s $Serial shell settings put global transition_animation_scale 0
& $adb -s $Serial shell settings put global animator_duration_scale 0
& $adb -s $Serial install -r "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
if($LASTEXITCODE) { throw 'App installation failed' }
& $adb -s $Serial install -r "$projectRoot\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
if($LASTEXITCODE) { throw 'Test APK installation failed' }
& $adb -s $Serial shell am instrument -w com.gridguild.test/androidx.test.runner.AndroidJUnitRunner
if($LASTEXITCODE) { throw 'Instrumentation failed' }
