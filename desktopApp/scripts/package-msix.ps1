[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Version
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Convert-ToStoreVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    if ($Value -notmatch "^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$") {
        throw "Desktop version must use MAJOR.MINOR.PATCH format: $Value"
    }

    $parts = @(
        [int]$Matches[1],
        [int]$Matches[2],
        [int]$Matches[3],
        0
    )
    foreach ($part in $parts) {
        if ($part -lt 0 -or $part -gt 65535) {
            throw "Each MSIX version component must be between 0 and 65535: $Value"
        }
    }

    return $parts -join "."
}

function Find-MakeAppx {
    $command = Get-Command "makeappx.exe" -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $windowsKitsRoot = Join-Path "${env:ProgramFiles(x86)}" "Windows Kits\10\bin"
    $candidates = @(
        Get-ChildItem `
            -Path (Join-Path $windowsKitsRoot "*\x64\makeappx.exe") `
            -File `
            -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending
    )
    if ($candidates.Count -eq 0) {
        throw "makeappx.exe was not found. Install the Windows SDK before packaging."
    }

    return $candidates[0].FullName
}

function Find-MakePri {
    $command = Get-Command "makepri.exe" -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $windowsKitsRoot = Join-Path "${env:ProgramFiles(x86)}" "Windows Kits\10\bin"
    $candidates = @(
        Get-ChildItem `
            -Path (Join-Path $windowsKitsRoot "*\x64\makepri.exe") `
            -File `
            -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending
    )
    if ($candidates.Count -eq 0) {
        throw "makepri.exe was not found. Install the Windows SDK before packaging."
    }

    return $candidates[0].FullName
}

function Write-SquarePng {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,
        [Parameter(Mandatory = $true)]
        [string]$DestinationPath,
        [Parameter(Mandatory = $true)]
        [int]$Size
    )

    Add-Type -AssemblyName System.Drawing

    $source = [System.Drawing.Image]::FromFile($SourcePath)
    try {
        $bitmap = [System.Drawing.Bitmap]::new(
            $Size,
            $Size,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.DrawImage($source, 0, 0, $Size, $Size)
            } finally {
                $graphics.Dispose()
            }

            $bitmap.Save(
                $DestinationPath,
                [System.Drawing.Imaging.ImageFormat]::Png
            )
        } finally {
            $bitmap.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}

if ([System.Environment]::OSVersion.Platform -ne [System.PlatformID]::Win32NT) {
    throw "Store MSIX packaging must run on Windows."
}

$storeVersion = Convert-ToStoreVersion -Value $Version
$desktopAppDirectory = Split-Path -Parent $PSScriptRoot
$appImageRoot = Join-Path $desktopAppDirectory "build\compose\binaries\main\app"
$expectedExecutable = Join-Path $appImageRoot "SSUTime\SSUTime.exe"

if (-not (Test-Path -LiteralPath $expectedExecutable -PathType Leaf)) {
    $candidates = @(
        Get-ChildItem `
            -Path $appImageRoot `
            -Filter "SSUTime.exe" `
            -File `
            -Recurse `
            -ErrorAction SilentlyContinue
    )
    if ($candidates.Count -ne 1) {
        throw "Expected one SSUTime.exe under $appImageRoot, found $($candidates.Count). Run :desktopApp:createDistributable first."
    }
    $expectedExecutable = $candidates[0].FullName
}

$appImageDirectory = Split-Path -Parent $expectedExecutable
$msixBuildDirectory = Join-Path $desktopAppDirectory "build\msix"
$stagingDirectory = Join-Path $msixBuildDirectory "staging"
$verificationDirectory = Join-Path $msixBuildDirectory "verification"
$outputPath = Join-Path $msixBuildDirectory "SSUTime-$Version-windows-x64.msix"

foreach ($directory in @($stagingDirectory, $verificationDirectory)) {
    if (Test-Path -LiteralPath $directory) {
        Remove-Item -LiteralPath $directory -Recurse -Force
    }
}
if (Test-Path -LiteralPath $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

New-Item -ItemType Directory -Path $stagingDirectory | Out-Null
New-Item -ItemType Directory -Path (Join-Path $stagingDirectory "Assets") | Out-Null
Copy-Item `
    -LiteralPath $appImageDirectory `
    -Destination (Join-Path $stagingDirectory "App") `
    -Recurse

$sourceIcon = Join-Path $desktopAppDirectory "src\main\resources\icons\checkbox.png"
Write-SquarePng `
    -SourcePath $sourceIcon `
    -DestinationPath (Join-Path $stagingDirectory "Assets\StoreLogo.png") `
    -Size 50
Write-SquarePng `
    -SourcePath $sourceIcon `
    -DestinationPath (Join-Path $stagingDirectory "Assets\Square44x44Logo.png") `
    -Size 44
Write-SquarePng `
    -SourcePath $sourceIcon `
    -DestinationPath (Join-Path $stagingDirectory "Assets\Square150x150Logo.png") `
    -Size 150

$appListIconSizes = @(16, 20, 24, 30, 32, 36, 40, 48, 60, 64, 72, 80, 96, 256)
$appListIconVariants = @(
    "",
    "_altform-unplated",
    "_altform-lightunplated"
)
foreach ($size in $appListIconSizes) {
    foreach ($variant in $appListIconVariants) {
        Write-SquarePng `
            -SourcePath $sourceIcon `
            -DestinationPath (Join-Path $stagingDirectory "Assets\Square44x44Logo.targetsize-$size$variant.png") `
            -Size $size
    }
}

$manifestTemplatePath = Join-Path $desktopAppDirectory "src\main\msix\AppxManifest.xml"
$manifestOutputPath = Join-Path $stagingDirectory "AppxManifest.xml"
$manifest = [System.IO.File]::ReadAllText($manifestTemplatePath)
$manifest = $manifest.Replace("@PACKAGE_VERSION@", $storeVersion)
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($manifestOutputPath, $manifest, $utf8WithoutBom)

$priConfigTemplatePath = Join-Path $desktopAppDirectory "src\main\msix\priconfig.xml"
$priConfigOutputPath = Join-Path $stagingDirectory "priconfig.xml"
Copy-Item -LiteralPath $priConfigTemplatePath -Destination $priConfigOutputPath

$makePri = Find-MakePri
$priOutputPath = Join-Path $stagingDirectory "resources.pri"
& $makePri new /pr $stagingDirectory /cf $priConfigOutputPath /mn $manifestOutputPath /of $priOutputPath /o
if ($LASTEXITCODE -ne 0) {
    throw "MakePri failed to index package resources."
}
Remove-Item -LiteralPath $priConfigOutputPath -Force

$makeAppx = Find-MakeAppx
New-Item -ItemType Directory -Path $msixBuildDirectory -Force | Out-Null

& $makeAppx pack /o /h SHA256 /d $stagingDirectory /p $outputPath
if ($LASTEXITCODE -ne 0) {
    throw "MakeAppx failed to create the MSIX package."
}

& $makeAppx unpack /o /p $outputPath /d $verificationDirectory
if ($LASTEXITCODE -ne 0) {
    throw "MakeAppx failed to validate the generated MSIX package."
}

$verifiedPriPath = Join-Path $verificationDirectory "resources.pri"
if (-not (Test-Path -LiteralPath $verifiedPriPath -PathType Leaf)) {
    throw "The package resource index file (resources.pri) is missing from the MSIX package."
}

[xml]$packagedManifest = Get-Content `
    -LiteralPath (Join-Path $verificationDirectory "AppxManifest.xml") `
    -Raw
$identity = $packagedManifest.Package.Identity
$identityName = $identity.GetAttribute("Name")
$identityPublisher = $identity.GetAttribute("Publisher")
$identityVersion = $identity.GetAttribute("Version")
$application = $packagedManifest.Package.Applications.Application
$applicationId = $application.GetAttribute("Id")
$backgroundColor = $application.VisualElements.GetAttribute("BackgroundColor")
if ($identityName -ne "Campo.1711AB9C2595") {
    throw "Unexpected MSIX Identity Name: $identityName"
}
if ($identityPublisher -ne "CN=BC44C2C8-25C2-4313-917E-619FF08BC787") {
    throw "Unexpected MSIX Publisher: $identityPublisher"
}
if ($identityVersion -ne $storeVersion) {
    throw "Unexpected MSIX version: $identityVersion"
}
if ($applicationId -ne "SSUTime") {
    throw "Unexpected MSIX application ID: $applicationId"
}
if ($backgroundColor -ne "transparent") {
    throw "The MSIX app icon background must be transparent: $backgroundColor"
}
foreach ($size in $appListIconSizes) {
    foreach ($variant in $appListIconVariants) {
        $assetName = "Square44x44Logo.targetsize-$size$variant.png"
        $assetPath = Join-Path $verificationDirectory "Assets\$assetName"
        if (-not (Test-Path -LiteralPath $assetPath -PathType Leaf)) {
            throw "The transparent app icon asset is missing: $assetName"
        }
    }
}

Remove-Item -LiteralPath $verificationDirectory -Recurse -Force

Write-Output "Created Microsoft Store package: $outputPath"
Write-Output "Package version: $storeVersion"
