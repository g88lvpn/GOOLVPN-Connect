param(
    [Parameter(Mandatory = $true)]
    [string]$Version,

    [string]$Server = "root@89.208.106.150",

    [switch]$Publish
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE."
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$workspaceRoot = Split-Path $repoRoot -Parent
$apkName = "GOOLVPN-Connect-$Version-arm64-v8a.apk"
$manifestName = "app_release-$Version-test.json"
$apkPath = Join-Path $repoRoot "dist\$apkName"
$manifestPath = Join-Path $repoRoot "dist\$manifestName"
$activationPage = Join-Path (Split-Path $workspaceRoot -Parent) "nodegate-site\app-activate.html"

foreach ($path in @($apkPath, $manifestPath, $activationPage)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required file is missing: $path"
    }
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
$localHash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
$localSize = (Get-Item -LiteralPath $apkPath).Length

if ([string]$manifest.version_name -ne $Version) {
    throw "Manifest version_name does not match $Version."
}
if ([string]$manifest.sha256 -ne $localHash) {
    throw "Manifest SHA-256 does not match the APK."
}
if ([long]$manifest.file_size -ne $localSize) {
    throw "Manifest file_size does not match the APK."
}
if ([string]$manifest.download_url -ne "https://goolv.site/downloads/$apkName") {
    throw "Manifest download_url does not match the expected public URL."
}

$remoteApk = "/var/www/nodegate/downloads/$apkName"
$remoteApkUpload = "$remoteApk.upload"
$remotePage = "/var/www/nodegate/app-activate.html"
$remotePageUpload = "$remotePage.upload"
$remoteManifest = "/opt/vpn_bot/update-staging/$manifestName"

Write-Host "Uploading GOOLVPN Connect $Version to $Server..."
Invoke-Checked ssh @(
    $Server,
    "mkdir -p /var/www/nodegate/downloads /opt/vpn_bot/update-staging"
)
Invoke-Checked scp @($activationPage, "${Server}:$remotePageUpload")
Invoke-Checked scp @($apkPath, "${Server}:$remoteApkUpload")
Invoke-Checked scp @($manifestPath, "${Server}:$remoteManifest")

$remoteVerify = @"
set -eu
test "`$(sha256sum '$remoteApkUpload' | awk '{print `$1}')" = '$localHash'
python3 -m json.tool '$remoteManifest' >/dev/null
mv '$remotePageUpload' '$remotePage'
mv '$remoteApkUpload' '$remoteApk'
test "`$(curl -fsSL 'https://goolv.site/downloads/$apkName' | sha256sum | awk '{print `$1}')" = '$localHash'
"@
Invoke-Checked ssh @($Server, $remoteVerify)

if ($Publish) {
    Invoke-Checked ssh @(
        $Server,
        "cp '$remoteManifest' /opt/vpn_bot/app_release.json"
    )
    Write-Host "Published $Version. No service restart is required."
} else {
    Write-Host "Files uploaded and verified, but the update is not published."
    Write-Host "Publish after testing with:"
    Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\deploy_update.ps1 -Version $Version -Publish"
}

Write-Host "SHA-256: $localHash"
