param (
    [Parameter(Mandatory = $false, Position = 0)]
    [ValidateSet("major", "minor", "patch", "1", "2", "3", "")]
    [string]$BumpType = ""
)

$ErrorActionPreference = "Stop"

$gradleFile = Join-Path $PSScriptRoot "app/build.gradle.kts"

if (-not (Test-Path $gradleFile)) {
    Write-Error "Could not find app/build.gradle.kts at $gradleFile"
    exit 1
}

$content = Get-Content -Path $gradleFile -Raw

# Match versionCode and versionName
$codeMatch = [regex]::Match($content, '(?m)^\s*versionCode\s*=\s*(\d+)')
$nameMatch = [regex]::Match($content, '(?m)^\s*versionName\s*=\s*"([^"]+)"')

if (-not $codeMatch.Success -or -not $nameMatch.Success) {
    Write-Error "Failed to parse versionCode or versionName in $gradleFile"
    exit 1
}

$currentCode = [int]$codeMatch.Groups[1].Value
$currentName = $nameMatch.Groups[1].Value

# Parse semver (xx.xx.xx)
$parts = $currentName.Split('.')
$major = if ($parts.Length -ge 1) { [int]$parts[0] } else { 0 }
$minor = if ($parts.Length -ge 2) { [int]$parts[1] } else { 0 }
$patch = if ($parts.Length -ge 3) { [int]$parts[2] } else { 0 }

$currentFormatted = "$major.$minor.$patch"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " ScreenHarmony Flex - Version Bump Script" -ForegroundColor Cyan
Write-Host " Current Version: $currentFormatted (Code: $currentCode)" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($BumpType)) {
    Write-Host ""
    Write-Host "Select version bump type:"
    Write-Host "  [1] major  -> ($($major + 1).0.0)"
    Write-Host "  [2] minor  -> ($major.$($minor + 1).0)"
    Write-Host "  [3] patch  -> ($major.$minor.$($patch + 1))"
    Write-Host ""
    $choice = Read-Host "Enter choice [1/2/3 or major/minor/patch]"
} else {
    $choice = $BumpType
}

switch -Regex ($choice.Trim().ToLower()) {
    "^(1|major)$" {
        $newMajor = $major + 1
        $newMinor = 0
        $newPatch = 0
    }
    "^(2|minor)$" {
        $newMajor = $major
        $newMinor = $minor + 1
        $newPatch = 0
    }
    "^(3|patch)$" {
        $newMajor = $major
        $newMinor = $minor
        $newPatch = $patch + 1
    }
    Default {
        Write-Error "Invalid choice: '$choice'. Please select major, minor, or patch."
        exit 1
    }
}

$newVersionName = "$newMajor.$newMinor.$newPatch"
$newVersionCode = $currentCode + 1

# Replace in build.gradle.kts
$updatedContent = [regex]::Replace($content, '(?m)^\s*versionCode\s*=\s*\d+', "        versionCode = $newVersionCode")
$updatedContent = [regex]::Replace($updatedContent, '(?m)^\s*versionName\s*=\s*"[^"]+"', "        versionName = `"$newVersionName`"")

Set-Content -Path $gradleFile -Value $updatedContent -NoNewline

Write-Host ""
Write-Host "✅ Version successfully bumped!" -ForegroundColor Green
Write-Host "   Version Name: $currentFormatted -> $newVersionName" -ForegroundColor Green
Write-Host "   Version Code: $currentCode -> $newVersionCode" -ForegroundColor Green
Write-Host "   Updated: $gradleFile" -ForegroundColor DarkGray
