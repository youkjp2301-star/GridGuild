param([string[]]$Tasks = @(':app:assembleDebug', ':core:test', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
$projectRoot=Split-Path $PSScriptRoot
Push-Location $projectRoot
try {
    if (!$env:JAVA_HOME) {
        $localJdk=Get-ChildItem "$projectRoot\.tools\jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($localJdk) { $env:JAVA_HOME=$localJdk.FullName }
    }
    if (!$env:ANDROID_HOME -and (Test-Path "$projectRoot\.tools\android-sdk")) { $env:ANDROID_HOME="$projectRoot\.tools\android-sdk" }
    if ($env:ANDROID_HOME) { "sdk.dir=$($env:ANDROID_HOME.Replace('\','/'))" | Set-Content -Encoding ascii local.properties }
    if (!(Test-Path gradlew.bat)) { throw 'Gradle wrapper is missing' }
    & .\gradlew.bat --console=plain @Tasks
    if ($LASTEXITCODE) { throw "Gradle failed ($LASTEXITCODE)" }
} finally { Pop-Location }
