param (
    [Parameter(Mandatory = $false, Position = 0)]
    [ValidateSet("major", "minor", "patch", "1", "2", "3", "")]
    [string]$BumpType = ""
)

$ErrorActionPreference = "Stop"

$propsFile = Join-Path $PSScriptRoot "version.properties"

if (-not (Test-Path $propsFile)) {
    Write-Error "Could not find version.properties at $propsFile"
    exit 1
}

# Read existing properties
$props = @{}
Get-Content -Path $propsFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $key, $value = $line.Split("=", 2)
        if ($key -and $value) {
            $props[$key.Trim()] = $value.Trim()
        }
    }
}

$major = if ($props.ContainsKey("VERSION_MAJOR")) { [int]$props["VERSION_MAJOR"] } else { 0 }
$minor = if ($props.ContainsKey("VERSION_MINOR")) { [int]$props["VERSION_MINOR"] } else { 1 }
$patch = if ($props.ContainsKey("VERSION_PATCH")) { [int]$props["VERSION_PATCH"] } else { 0 }
$code  = if ($props.ContainsKey("VERSION_CODE"))  { [int]$props["VERSION_CODE"] }  else { 1 }

$currentFormatted = "$major.$minor.$patch"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " ScreenHarmony Flex - Version Bump" -ForegroundColor Cyan
Write-Host " Current Version: $currentFormatted (Code: $code)" -ForegroundColor Yellow
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
$newVersionCode = $code + 1

# Write updated properties to version.properties (No Gradle Sync needed!)
$newContent = @"
VERSION_MAJOR=$newMajor
VERSION_MINOR=$newMinor
VERSION_PATCH=$newPatch
VERSION_CODE=$newVersionCode
"@

Set-Content -Path $propsFile -Value $newContent -NoNewline

Write-Host ""
Write-Host "✅ Version successfully bumped without Gradle script changes!" -ForegroundColor Green
Write-Host "   Version Name: $currentFormatted -> $newVersionName" -ForegroundColor Green
Write-Host "   Version Code: $code -> $newVersionCode" -ForegroundColor Green
Write-Host "   Target: version.properties (Zero Gradle Sync required)" -ForegroundColor DarkGray
