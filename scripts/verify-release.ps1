param(
    [string]$Apk = 'apk/GridGuild_GridGuild_release.apk',
    [string]$Sdk,
    [string]$ExpectedSignerSha256
)
# Read-only checks: this script never installs, signs, modifies, or extracts the APK.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
if (![IO.Path]::IsPathRooted($Apk)) { $Apk = Join-Path $projectRoot $Apk }
$apkFile = Get-Item -LiteralPath $Apk
if ($apkFile.PSIsContainer) { throw 'The APK path must identify a file.' }
if ($apkFile.Length -gt 100MB) {
    throw "APK is $($apkFile.Length) bytes, above GitHub's 100 MiB repository limit. Use GitHub Releases instead."
}
if (!$Sdk) { $Sdk = $env:ANDROID_HOME }
if (!$Sdk) { $Sdk = $env:ANDROID_SDK_ROOT }
if (!$Sdk) { $Sdk = Join-Path $projectRoot '.tools/android-sdk' }
if (![IO.Path]::IsPathRooted($Sdk)) { $Sdk = Join-Path $projectRoot $Sdk }
$buildTools = Join-Path $Sdk 'build-tools/35.0.0'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$aapt2 = Join-Path $buildTools 'aapt2.exe'
foreach ($tool in @($apksigner, $aapt2)) {
    if (!(Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "Required Android Build Tools 35.0.0 tool is missing: $tool"
    }
}

function Invoke-CheckedTool([string]$Tool, [string[]]$Arguments) {
    $output = @(& $Tool @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "$([IO.Path]::GetFileName($Tool)) failed ($LASTEXITCODE): $($output -join [Environment]::NewLine)"
    }
    return ($output -join [Environment]::NewLine)
}

# aapt2 represents omitted booleans using Android's default (false). If a flag
# is explicitly present, accept only an unambiguous false literal; fail closed.
function Assert-FalseManifestFlag([string]$Manifest, [string]$Flag) {
    $pattern = '(?m)^\s*A:\s+[^\r\n=]*:' + [regex]::Escape($Flag) + '(?:\([^)]*\))?=([^\r\n]+)$'
    foreach ($match in [regex]::Matches($Manifest, $pattern)) {
        $value = $match.Groups[1].Value.Trim() -replace '^\(type\s+0x[0-9a-fA-F]+\)', ''
        if ($value -notmatch '^(?:false|0|0x0+)(?:\s|$)') {
            throw "Release manifest must have $Flag=false (or omit the flag)."
        }
    }
}

$previousJavaHome = $env:JAVA_HOME
try {
    if (!$env:JAVA_HOME) {
        $localJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    }
    $signature = Invoke-CheckedTool -Tool $apksigner -Arguments @('verify', '--verbose', '--print-certs', $apkFile.FullName)
    $digests = @([regex]::Matches($signature, '(?m)^Signer #\d+ certificate SHA-256 digest:\s*([a-fA-F0-9:]+)\s*$') | ForEach-Object {
        $_.Groups[1].Value.Replace(':', '').ToLowerInvariant()
    })
    if ($digests.Count -eq 0 -or @($digests | Where-Object { $_ -notmatch '^[a-f0-9]{64}$' }).Count -gt 0) {
        throw 'No valid signing certificate SHA-256 fingerprint was returned.'
    }
    if ($signature -match '(?im)^Signer #\d+ certificate DN:.*CN\s*=\s*Android Debug(?:,|\s|$)') {
        throw 'The APK uses the Android Debug certificate; a release signing key is required.'
    }
    if ($ExpectedSignerSha256) {
        $expectedDigest = ($ExpectedSignerSha256 -replace '[:\s]', '').ToLowerInvariant()
        if ($expectedDigest -notmatch '^[a-f0-9]{64}$') { throw 'ExpectedSignerSha256 must contain a SHA-256 certificate fingerprint.' }
        if ($digests.Count -ne 1 -or $digests[0] -ne $expectedDigest) { throw 'APK signing certificate does not match the expected release signer.' }
    }

    $badging = Invoke-CheckedTool -Tool $aapt2 -Arguments @('dump', 'badging', $apkFile.FullName)
    $packageMatch = [regex]::Match($badging, "(?m)^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'")
    if (!$packageMatch.Success -or $packageMatch.Groups[1].Value -ne 'com.gridguild') {
        throw 'APK package must be com.gridguild.'
    }
    if ($badging -match '(?m)^application-debuggable\b') { throw 'APK is debuggable.' }
    $manifest = Invoke-CheckedTool -Tool $aapt2 -Arguments @('dump', 'xmltree', '--file', 'AndroidManifest.xml', $apkFile.FullName)
    Assert-FalseManifestFlag -Manifest $manifest -Flag 'debuggable'
    Assert-FalseManifestFlag -Manifest $manifest -Flag 'testOnly'
    $resources = Invoke-CheckedTool -Tool $aapt2 -Arguments @('dump', 'resources', $apkFile.FullName)
} finally {
    $env:JAVA_HOME = $previousJavaHome
}

# Release resource optimization can shorten ZIP paths (for example res/7y.png).
# Resolve stable Android resource names through resources.arsc using aapt2,
# rather than assuming that the original PNG filename survives packaging.
$resourceFiles = @{}
$activeDrawable = $null
foreach ($line in ($resources -split '\r?\n')) {
    if ($line -match '^\s*resource\s+0x[0-9a-fA-F]+\s+') {
        $activeDrawable = $null
        if ($line -match '^\s*resource\s+0x[0-9a-fA-F]+\s+drawable/([A-Za-z0-9_]+)(?:\s|$)') {
            $activeDrawable = $Matches[1]
        }
    } elseif ($activeDrawable -and $line -match '^\s*\([^)]*\)\s+\(file\)\s+(res/\S+)\s+type=PNG(?:\s|$)') {
        $resourceFiles[$activeDrawable] = @($resourceFiles[$activeDrawable]) + $Matches[1]
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($apkFile.FullName)
try {
    $duplicateNames = @($archive.Entries | Group-Object FullName | Where-Object { $_.Count -gt 1 })
    if ($duplicateNames.Count -gt 0) { throw 'APK contains duplicate ZIP entry names.' }
    foreach ($entryName in @('AndroidManifest.xml', 'classes.dex', 'resources.arsc')) {
        if (!$archive.GetEntry($entryName)) { throw "APK is missing required entry: $entryName" }
    }
    $requiredTextures = @('city_medieval_v4.png', 'city_modern_v4.png', 'city_future_v4.png', 'city_loading_v2.png')
    foreach ($city in @('gwangalli', 'centum')) {
        foreach ($level in 1..12) { $requiredTextures += ('driver_{0}_lv{1:00}_v2.png' -f $city, $level) }
    }
    $largestDecodedTexture = [uint64]0
    foreach ($texture in $requiredTextures) {
        $resourceName = [IO.Path]::GetFileNameWithoutExtension($texture)
        $paths = @($resourceFiles[$resourceName] | Where-Object { $_ } | Select-Object -Unique)
        if ($paths.Count -ne 1) { throw "APK resource table must retain exactly one active PNG: drawable/$resourceName" }
        $entry = $archive.GetEntry($paths[0])
        if (!$entry) { throw "APK is missing the packaged texture for drawable/$resourceName" }
        if ($entry.Length -gt 16MB) { throw "Encoded PNG is unexpectedly large: $texture" }
        $stream = $entry.Open()
        try {
            $header = New-Object byte[] 24
            $read = 0
            while ($read -lt $header.Length) {
                $count = $stream.Read($header, $read, $header.Length - $read)
                if ($count -eq 0) { throw "PNG header is truncated: $texture" }
                $read += $count
            }
            if ([BitConverter]::ToString($header, 0, 8) -ne '89-50-4E-47-0D-0A-1A-0A' -or [Text.Encoding]::ASCII.GetString($header, 12, 4) -ne 'IHDR') {
                throw "Active texture is not a valid PNG: $texture"
            }
            $width = ([uint64]$header[16] -shl 24) -bor ([uint64]$header[17] -shl 16) -bor ([uint64]$header[18] -shl 8) -bor [uint64]$header[19]
            $height = ([uint64]$header[20] -shl 24) -bor ([uint64]$header[21] -shl 16) -bor ([uint64]$header[22] -shl 8) -bor [uint64]$header[23]
            $decodedBytes = $width * $height * 4
            # Current active images decode to about 6 MiB each. Keep a 16 MiB
            # RGBA upper bound to catch accidental oversized replacements.
            if ($width -eq 0 -or $height -eq 0 -or $width -gt 4096 -or $height -gt 4096 -or $decodedBytes -gt 16MB) {
                throw "Active texture exceeds the 4096 px / 16 MiB RGBA budget: $texture"
            }
            if ($decodedBytes -gt $largestDecodedTexture) { $largestDecodedTexture = $decodedBytes }
        } finally { $stream.Dispose() }
    }
} finally { $archive.Dispose() }

[pscustomobject]@{
    Apk = $apkFile.FullName
    SizeBytes = $apkFile.Length
    Sha256 = (Get-FileHash -LiteralPath $apkFile.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    Package = $packageMatch.Groups[1].Value
    VersionCode = $packageMatch.Groups[2].Value
    VersionName = $packageMatch.Groups[3].Value
    Debuggable = $false
    TestOnly = $false
    SignerSha256 = $digests -join ','
    RetainedTextures = $requiredTextures.Count
    LargestTextureRgbaBytes = $largestDecodedTexture
}
