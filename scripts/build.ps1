param(
    [switch]$Clean,
    [switch]$RebuildLibbox,
    [switch]$Release,
    [switch]$Test,
    [switch]$Lint
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$workspaceRoot = Split-Path $repoRoot -Parent
$driveName = @("V:", "W:", "X:", "Y:", "Z:") |
    Where-Object { -not (Test-Path "$_\") } |
    Select-Object -First 1
if (-not $driveName) {
    throw "No temporary drive letter is available."
}
$driveRoot = "$driveName\"

if (-not ("GoolvpnDosDevice" -as [type])) {
    Add-Type -TypeDefinition @"
using System.Runtime.InteropServices;

public static class GoolvpnDosDevice {
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern bool DefineDosDevice(
        uint flags,
        string deviceName,
        string targetPath
    );
}
"@
}

$removeDefinition = 0x2
$exactMatch = 0x4
$localProperties = $null
$localPropertiesExisted = $false
$localPropertiesContent = $null

if (-not [GoolvpnDosDevice]::DefineDosDevice(0, $driveName, $workspaceRoot)) {
    $errorCode = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
    throw "Could not map $driveName to the workspace (Win32 error $errorCode)."
}

try {
    $asciiRepo = Join-Path $driveRoot "goolvpn-android"
    $toolsRoot = Join-Path $driveRoot ".android-tools"
    $androidHome = Join-Path $toolsRoot "android-sdk"
    $jdkHome = Get-ChildItem (Join-Path $toolsRoot "jdk") -Directory |
        Select-Object -First 1 -ExpandProperty FullName

    if (-not $jdkHome -or -not (Test-Path $androidHome)) {
        throw "Android SDK or JDK is missing under $toolsRoot."
    }

    $env:JAVA_HOME = $jdkHome
    $env:ANDROID_HOME = $androidHome
    $env:ANDROID_SDK_ROOT = $androidHome
    $env:GRADLE_USER_HOME = Join-Path $toolsRoot "gradle-home"
    $env:PATH = "$jdkHome\bin;$androidHome\platform-tools;$env:PATH"

    $localProperties = Join-Path $asciiRepo "local.properties"
    $localPropertiesExisted = Test-Path -LiteralPath $localProperties
    if ($localPropertiesExisted) {
        $localPropertiesContent = [IO.File]::ReadAllBytes($localProperties)
    }
    [IO.File]::WriteAllText(
        $localProperties,
        "sdk.dir=$($driveName.Replace(':', '\:'))/.android-tools/android-sdk`n"
    )

    if ($RebuildLibbox) {
        $goRoot = Join-Path $toolsRoot "go-full\go"
        $goPath = Join-Path $toolsRoot "gopath"
        $coreRoot = Join-Path $asciiRepo "third_party\sing-box-core"

        $env:GOROOT = $goRoot
        $env:GOPATH = $goPath
        $env:ANDROID_NDK_HOME = Join-Path $androidHome "ndk\28.0.13004108"
        $env:PATH = "$goRoot\bin;$goPath\bin;$env:PATH"

        Push-Location $coreRoot
        try {
            & "$goRoot\bin\go.exe" run ./cmd/internal/build_libbox -target android
            if ($LASTEXITCODE -ne 0) {
                throw "libbox build failed with exit code $LASTEXITCODE."
            }
        }
        finally {
            Pop-Location
        }

        $libsDir = Join-Path $asciiRepo "app\libs"
        New-Item -ItemType Directory -Path $libsDir -Force | Out-Null
        Copy-Item (Join-Path $coreRoot "libbox.aar") $libsDir -Force
        Copy-Item (Join-Path $coreRoot "libbox-legacy.aar") $libsDir -Force
    }

    if (-not (Test-Path (Join-Path $asciiRepo "app\libs\libbox.aar"))) {
        throw "app/libs/libbox.aar is missing. Run this script with -RebuildLibbox first."
    }

    Push-Location $asciiRepo
    try {
        $gradleTasks = @()
        if ($Clean) {
            $gradleTasks += "clean"
        }
        if ($Release) {
            $signingProperties = if ($env:GOOLVPN_SIGNING_PROPERTIES) {
                $env:GOOLVPN_SIGNING_PROPERTIES
            } else {
                Join-Path $env:USERPROFILE "GOOLVPN-signing\signing.properties"
            }
            if (-not (Test-Path $signingProperties)) {
                throw "GOOLVPN release signing is missing. Run scripts/create_release_key.ps1 first."
            }
            $env:GOOLVPN_SIGNING_PROPERTIES = $signingProperties
            $gradleTasks += ":app:assembleOtherRelease"
        } else {
            $gradleTasks += ":app:assembleOtherDebug"
        }
        if ($Test) {
            $gradleTasks += ":app:testOtherDebugUnitTest"
        }
        if ($Lint) {
            $gradleTasks += if ($Release) { ":app:lintOtherRelease" } else { ":app:lintOtherDebug" }
        }

        & ".\gradlew.bat" @gradleTasks --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE."
        }

        $variantDir = if ($Release) { "release" } else { "debug" }
        $distDir = Join-Path $asciiRepo "dist"
        New-Item -ItemType Directory -Path $distDir -Force | Out-Null
        Get-ChildItem (Join-Path $asciiRepo "app\build\outputs\apk\other\$variantDir") -Filter "*.apk" |
            Where-Object { $_.Name -like "*arm64-v8a*" -or $_.Name -like "*universal*" } |
            Copy-Item -Destination $distDir -Force
    }
    finally {
        Pop-Location
    }
}
finally {
    if ($localProperties) {
        if ($localPropertiesExisted) {
            [IO.File]::WriteAllBytes($localProperties, $localPropertiesContent)
        } elseif (Test-Path -LiteralPath $localProperties) {
            Remove-Item -LiteralPath $localProperties -Force
        }
    }
    [void][GoolvpnDosDevice]::DefineDosDevice(
        $removeDefinition -bor $exactMatch,
        $driveName,
        $workspaceRoot
    )
}
