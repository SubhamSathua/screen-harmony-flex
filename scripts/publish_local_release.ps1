<#
.SYNOPSIS
    Builds signed APKs locally with release.jks and uploads them to GitHub Releases.
.DESCRIPTION
    Builds assembleAlphaRelease or assembleProdRelease locally using the developer
    keystore (preventing Play Protect warnings from ephemeral CI certificates),
    reads release notes from .github/release-notes/<tag>.md, and creates a GitHub Release.
.EXAMPLE
    .\scripts\publish_local_release.ps1 -Tag "v2.10.1-alpha"
#>
param (
    [string]$Tag = ""
)

$ErrorActionPreference = "Stop"

# 1. Determine tag from version.properties if not provided
if ([string]::IsNullOrWhiteSpace($Tag)) {
    if (Test-Path "version.properties") {
        $props = Get-Content "version.properties" | ConvertFrom-StringData
        $major = $props.VERSION_MAJOR
        $minor = $props.VERSION_MINOR
        $patch = $props.VERSION_PATCH
        $Tag = "v$major.$minor.$patch"
    } else {
        Write-Error "No Tag specified and version.properties not found."
    }
}

$isAlpha = $Tag -match "(alpha|beta|rc|dev|snapshot)"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host " Publishing Local Release for Tag: $Tag" -ForegroundColor Green
Write-Host " Pre-release / Alpha mode: $isAlpha" -ForegroundColor Yellow
Write-Host "====================================================" -ForegroundColor Cyan

# 2. Build signed APKs locally
if ($isAlpha) {
    Write-Host "Building Alpha Release APKs..." -ForegroundColor Cyan
    ./gradlew assembleAlphaRelease
    $apkDir = "app/build/outputs/apk/alpha/release"
} else {
    Write-Host "Building Production Release APKs..." -ForegroundColor Cyan
    ./gradlew assembleProdRelease
    $apkDir = "app/build/outputs/apk/prod/release"
}

if (!(Test-Path $apkDir)) {
    Write-Error "APK directory not found: $apkDir"
}

$apkFiles = Get-ChildItem -Path $apkDir -Filter "*.apk"
if ($apkFiles.Count -eq 0) {
    Write-Error "No APK files found in $apkDir"
}

Write-Host "Found $($apkFiles.Count) APK files." -ForegroundColor Green

# 3. Locate release notes
$notesFile = ""
$candidateNotes = @(
    ".github/release-notes/$Tag.md",
    ".github/releases/$Tag.md",
    ".github/release-notes/$($Tag.TrimStart('v')).md",
    ".github/releases/$($Tag.TrimStart('v')).md"
)

foreach ($candidate in $candidateNotes) {
    if (Test-Path $candidate) {
        $notesFile = $candidate
        break
    }
}

if ([string]::IsNullOrWhiteSpace($notesFile)) {
    Write-Warning "No release notes file found for $Tag. Using standard tag title."
} else {
    Write-Host "Using release notes: $notesFile" -ForegroundColor Cyan
}

# 4. Create or recreate GitHub release via gh CLI
Write-Host "Uploading release $Tag to GitHub..." -ForegroundColor Cyan

# Delete existing release if it exists to allow clean re-upload
gh release delete $Tag -y --cleanup-tag=false 2>$null

$ghArgs = @(
    "release", "create", $Tag
)

foreach ($file in $apkFiles) {
    $ghArgs += $file.FullName
}

$ghArgs += @("--title", "ScreenHarmony Flex $Tag")

if (![string]::IsNullOrWhiteSpace($notesFile)) {
    $ghArgs += @("--notes-file", $notesFile)
}

if ($isAlpha) {
    $ghArgs += "--prerelease"
}

& gh @ghArgs

Write-Host "Successfully published $Tag to GitHub Releases!" -ForegroundColor Green
