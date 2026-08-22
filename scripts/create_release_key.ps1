param(
    [string]$OutputDirectory = (Join-Path $env:USERPROFILE "GOOLVPN-signing")
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$workspaceRoot = Split-Path $repoRoot -Parent
$keytoolPath = Get-ChildItem (Join-Path $workspaceRoot ".android-tools\jdk") -Recurse -Filter "keytool.exe" |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $keytoolPath) {
    throw "keytool.exe was not found in $workspaceRoot\.android-tools\jdk."
}

$keystorePath = Join-Path $OutputDirectory "goolvpn-release.jks"
$propertiesPath = Join-Path $OutputDirectory "signing.properties"

if ((Test-Path $keystorePath) -or (Test-Path $propertiesPath)) {
    throw "Release signing files already exist in $OutputDirectory. Nothing was overwritten."
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$randomBytes = New-Object byte[] 32
$randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $randomGenerator.GetBytes($randomBytes)
} finally {
    $randomGenerator.Dispose()
}
$password = ([BitConverter]::ToString($randomBytes) -replace "-", "").ToLowerInvariant()

& $keytoolPath `
    -genkeypair `
    -noprompt `
    -keystore $keystorePath `
    -storetype PKCS12 `
    -storepass $password `
    -keypass $password `
    -alias "goolvpn-release" `
    -keyalg RSA `
    -keysize 4096 `
    -validity 36500 `
    -dname "CN=GOOLVPN Connect, OU=Mobile, O=GOOLVPN, C=RU"

if ($LASTEXITCODE -ne 0) {
    throw "keytool failed with exit code $LASTEXITCODE."
}

$normalizedKeystorePath = $keystorePath.Replace("\", "/")
$properties = @(
    "KEYSTORE_FILE=$normalizedKeystorePath"
    "KEYSTORE_PASS=$password"
    "ALIAS_NAME=goolvpn-release"
    "ALIAS_PASS=$password"
) -join "`n"

[IO.File]::WriteAllText(
    $propertiesPath,
    "$properties`n",
    [Text.UTF8Encoding]::new($false)
)

$icacls = Join-Path $env:SystemRoot "System32\icacls.exe"
$currentUser = "$env:USERDOMAIN\$env:USERNAME"

& $icacls $OutputDirectory "/inheritance:r" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Failed to disable inherited permissions on $OutputDirectory."
}

& $icacls $OutputDirectory "/remove:g" "*S-1-1-0" "*S-1-5-11" "*S-1-5-32-545" "$env:USERDOMAIN\CodexSandboxUsers" | Out-Null
& $icacls $OutputDirectory "/grant:r" `
    "${currentUser}:(OI)(CI)F" `
    "*S-1-5-18:(OI)(CI)F" `
    "*S-1-5-32-544:(OI)(CI)F" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Failed to restrict permissions on $OutputDirectory."
}

Write-Host "GOOLVPN release signing key created."
Write-Host "Keystore: $keystorePath"
Write-Host "Credentials: $propertiesPath"
Write-Host "Back up both files together in encrypted storage."
