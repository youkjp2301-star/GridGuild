# Scans the actual Git index, reporting filenames and categories only (never secret values).
$ErrorActionPreference='Stop'
$OutputEncoding=[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false)
$root=Split-Path $PSScriptRoot
Push-Location $root
try {
    $files=@(& git -c core.quotepath=false ls-files --cached)
    $rules=@{
        'private key'='-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----'
        'GitHub token'='\b(gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})\b'
        'AWS access key'='\b(AKIA|ASIA)[A-Z0-9]{16}\b'
        'OpenAI key'='\bsk-(proj-|svcacct-)?[A-Za-z0-9_-]{35,}\b'
        'assigned credential'='(?im)^\s*(?:[\w.-]*(?:password|passwd|api[_-]?key|access[_-]?token|client[_-]?secret))\s*[=:]\s*["'']?(?!\$|<|example|replace|your_|changeme|dummy)[A-Za-z0-9_+/=-]{12,}'
    }
    $findings=@()
    foreach($file in $files) {
        if($file -match '(?i)(^|/)(local\.properties|\.env(?:\..*)?|secrets\.properties|credentials\.properties|key\.properties|keystore\.properties|signing\.properties|google-services\.json|agconnect-services\.json)$|\.(jks|keystore|p12|pfx|pem|key|apk|aab|aar)$') {
            if($file -notmatch '\.example$') { $findings+="$file : forbidden tracked filename" }
        }
        if($file -match '\.(jar|png|jpg|jpeg|gif|webp|ico|pdf|zip)$') { continue }
        $content=(& git show ":$file" | Out-String)
        if($LASTEXITCODE) { throw "Could not inspect indexed file: $file" }
        foreach($category in $rules.Keys) { if($content -match $rules[$category]) { $findings+="$file : $category" } }
    }
    if($findings.Count) { $findings | Write-Output;throw 'Potential credentials found in Git index. Review before committing.' }
    Write-Output "Secret scan passed: $($files.Count) indexed files. No credential values were printed."
} finally { Pop-Location }
