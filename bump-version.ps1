[CmdletBinding()]
param (
    [Parameter(Position = 0, Mandatory = $false)]
    [Alias("Type", "t")]
    [string]$BumpType = "",

    [Parameter(Mandatory = $false)]
    [Alias("v", "VersionName", "Version")]
    [string]$CustomVersion = "",

    [Parameter(Mandatory = $false)]
    [Alias("c", "VersionCode", "Code")]
    [Nullable[int]]$CustomCode = $null
)

$ErrorActionPreference = "Stop"

# Set console output encoding to UTF-8 for clean emoji rendering
try {
    $OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $scriptDir) { $scriptDir = "." }
$propsFile = Join-Path $scriptDir "version.properties"

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

$choice = $BumpType

if ([string]::IsNullOrWhiteSpace($choice)) {
    Write-Host ""
    Write-Host "Select version bump type:"
    Write-Host "  [1] major  -> ($($major + 1).0.0)"
    Write-Host "  [2] minor  -> ($major.$($minor + 1).0)"
    Write-Host "  [3] patch  -> ($major.$minor.$($patch + 1))"
    Write-Host "  [4] custom -> (Specify custom version name & code)"
    Write-Host ""
    $choice = Read-Host "Enter choice [1/2/3/4 or major/minor/patch/custom]"
}

$choice = $choice.Trim()

if ($choice -match "^(\d+)\.(\d+)(?:\.(\d+))?$") {
    $CustomVersion = $choice
    $choice = "custom"
}

$newMajor = $major
$newMinor = $minor
$newPatch = $patch
$newVersionCode = if ($CustomCode -ne $null) { [int]$CustomCode } else { $code + 1 }

switch -Regex ($choice.ToLower()) {
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
    "^(4|custom)$" {
        if ([string]::IsNullOrWhiteSpace($CustomVersion)) {
            Write-Host ""
            $CustomVersion = Read-Host "Enter custom version name [X.Y.Z] (current: $currentFormatted)"
        }

        $CustomVersion = $CustomVersion.Trim()
        if ($CustomVersion -match "^(\d+)\.(\d+)(?:\.(\d+))?$") {
            $newMajor = [int]$Matches[1]
            $newMinor = [int]$Matches[2]
            $newPatch = if ($Matches[3]) { [int]$Matches[3] } else { 0 }
        } else {
            Write-Error "Invalid version format '$CustomVersion'. Expected format: X.Y or X.Y.Z (e.g., 2.0.0)"
            exit 1
        }

        if ($CustomCode -eq $null) {
            $inputCode = Read-Host "Enter custom version code [integer] (default: $($code + 1))"
            if (-not [string]::IsNullOrWhiteSpace($inputCode)) {
                if ($inputCode -match "^\d+$") {
                    $newVersionCode = [int]$inputCode
                } else {
                    Write-Error "Invalid version code '$inputCode'. Expected a positive integer."
                    exit 1
                }
            }
        }
    }
    Default {
        Write-Error "Invalid choice: '$choice'. Please select major (1), minor (2), patch (3), or custom (4)."
        exit 1
    }
}

$newVersionName = "$newMajor.$newMinor.$newPatch"

# Write updated properties to version.properties (UTF-8 without BOM / clean format)
$newContent = @"
VERSION_MAJOR=$newMajor
VERSION_MINOR=$newMinor
VERSION_PATCH=$newPatch
VERSION_CODE=$newVersionCode
"@

[System.IO.File]::WriteAllText($propsFile, $newContent + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding $false))

Write-Host ""
Write-Host "✅ Version successfully bumped without Gradle script changes!" -ForegroundColor Green
Write-Host "   Version Name: $currentFormatted -> $newVersionName" -ForegroundColor Green
Write-Host "   Version Code: $code -> $newVersionCode" -ForegroundColor Green
Write-Host "   Target: version.properties (Zero Gradle Sync required)" -ForegroundColor DarkGray
