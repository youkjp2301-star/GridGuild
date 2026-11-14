param(
    [string[]]$Tasks = @(':app:assembleRelease', ':core:test', ':app:lintRelease'),
    [switch]$InitializeSigning
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$signingDirectory = Join-Path $env:USERPROFILE '.gridguild/signing'
$store = Join-Path $signingDirectory 'gridguild-release.p12'
$encryptedPassword = Join-Path $signingDirectory 'password.dpapi'
$localJdk = Get-ChildItem "$root/.tools/jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if (!$env:JAVA_HOME -and $localJdk) { $env:JAVA_HOME = $localJdk.FullName }
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17.' }
$keytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
if ($InitializeSigning) {
    if ((Test-Path $store) -or (Test-Path $encryptedPassword)) {
        throw 'Signing material already exists. It will not be overwritten.'
    }
    New-Item -ItemType Directory -Force $signingDirectory | Out-Null
    # Keep the private signing material outside the clone and restrict inherited access.
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $signingDirectory /inheritance:r /grant:r "${identity}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE) { throw 'Could not protect signing directory.' }
    $random = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($random) } finally { $rng.Dispose() }
    $secure = ConvertTo-SecureString ([Convert]::ToBase64String($random)) -AsPlainText -Force
    $secure | ConvertFrom-SecureString | Set-Content -LiteralPath $encryptedPassword -Encoding ascii
    $env:GRIDGUILD_SIGNING_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
    try {
        & $keytool -genkeypair -noprompt -keystore $store -storetype PKCS12 -storepass:env GRIDGUILD_SIGNING_PASSWORD -keypass:env GRIDGUILD_SIGNING_PASSWORD -alias gridguild -keyalg RSA -keysize 4096 -validity 10000 -dname 'CN=GridGuild Release, O=HU:MOTION, C=KR'
        if ($LASTEXITCODE) { throw 'Signing key generation failed. Preserve the directory for diagnosis.' }
    } finally { Remove-Item Env:GRIDGUILD_SIGNING_PASSWORD -ErrorAction SilentlyContinue }
    Write-Output "Created persistent release key outside Git: $store"
}
if (!(Test-Path $store) -or !(Test-Path $encryptedPassword)) {
    throw 'Release signing is not initialized on this PC. Read README.md; use -InitializeSigning only for the first release key.'
}
$previousStore = $env:GRIDGUILD_SIGNING_STORE
$previousPassword = $env:GRIDGUILD_SIGNING_PASSWORD
try {
    $secure = (Get-Content -LiteralPath $encryptedPassword -Raw).Trim() | ConvertTo-SecureString
    $env:GRIDGUILD_SIGNING_STORE = $store
    $env:GRIDGUILD_SIGNING_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
    $buildTasks = @($Tasks)
    & "$PSScriptRoot/build.ps1" -Tasks $buildTasks
    if ($Tasks -contains ':app:assembleRelease') {
        New-Item -ItemType Directory -Force "$root/apk" | Out-Null
        Copy-Item -LiteralPath "$root/app/build/outputs/apk/release/app-release.apk" -Destination "$root/apk/HU-MOTION_GridGuild_release.apk"
        Get-FileHash "$root/apk/HU-MOTION_GridGuild_release.apk" -Algorithm SHA256 | Select-Object Hash,Path
    }
} finally {
    $env:GRIDGUILD_SIGNING_STORE = $previousStore
    $env:GRIDGUILD_SIGNING_PASSWORD = $previousPassword
}
