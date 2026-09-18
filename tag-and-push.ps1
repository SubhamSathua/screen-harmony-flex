[CmdletBinding()]
param (
    [Parameter(Position = 0, Mandatory = $false)]
    [Alias("t", "TagName")]
    [string]$Tag = "",

    [Parameter(Mandatory = $false)]
    [Alias("p", "Pre")]
    [switch]$PreRelease,

    [Parameter(Mandatory = $false)]
    [Alias("s", "Channel")]
    [string]$Suffix = "",

    [Parameter(Mandatory = $false)]
    [Alias("m")]
    [string]$Message = "",

    [Parameter(Mandatory = $false)]
    [switch]$Push,

    [Parameter(Mandatory = $false)]
    [switch]$CreateNotesFile,

    [Parameter(Mandatory = $false)]
    [switch]$Force
)

$ErrorActionPreference = "Stop"

try {
    $OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $scriptDir) { $scriptDir = "." }
$propsFile = Join-Path $scriptDir "version.properties"
$releaseNotesDir = Join-Path $scriptDir ".github\release-notes"

if (-not (Test-Path $propsFile)) {
    Write-Error "Could not find version.properties at $propsFile"
    exit 1
}

# 1. Read properties
$props = @{}
Get-Content -Path $propsFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $k, $v = $line.Split("=", 2)
        if ($k -and $v) { $props[$k.Trim()] = $v.Trim() }
    }
}

$major = if ($props.ContainsKey("VERSION_MAJOR")) { [int]$props["VERSION_MAJOR"] } else { 0 }
$minor = if ($props.ContainsKey("VERSION_MINOR")) { [int]$props["VERSION_MINOR"] } else { 1 }
$patch = if ($props.ContainsKey("VERSION_PATCH")) { [int]$props["VERSION_PATCH"] } else { 0 }
$code  = if ($props.ContainsKey("VERSION_CODE"))  { [int]$props["VERSION_CODE"] }  else { 1 }

$baseVersion = "$major.$minor.$patch"

# 2. Determine final tag name
if ([string]::IsNullOrWhiteSpace($Tag)) {
    $tagBase = "v$baseVersion"
    if ($PreRelease -and [string]::IsNullOrWhiteSpace($Suffix)) {
        $Suffix = "alpha"
    }
    if (-not [string]::IsNullOrWhiteSpace($Suffix)) {
        $cleanSuffix = $Suffix.TrimStart("-")
        $finalTag = "$tagBase-$cleanSuffix"
    } else {
        $finalTag = $tagBase
    }
} else {
    $finalTag = if ($Tag.StartsWith("v")) { $Tag.Trim() } else { "v$($Tag.Trim())" }
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " ScreenHarmony Flex - Tag & Push System" -ForegroundColor Cyan
Write-Host " App Version: $baseVersion (Code: $code)" -ForegroundColor Yellow
Write-Host " Target Tag : $finalTag" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

# 3. Check for release notes
if (-not (Test-Path $releaseNotesDir)) {
    New-Item -ItemType Directory -Path $releaseNotesDir -Force | Out-Null
}

$candidateFiles = @(
    (Join-Path $releaseNotesDir "$finalTag.md"),
    (Join-Path $releaseNotesDir "$baseVersion.md"),
    (Join-Path $releaseNotesDir "v$baseVersion.md"),
    (Join-Path $scriptDir "$finalTag.md")
)

$foundNotesFile = $null
foreach ($file in $candidateFiles) {
    if (Test-Path $file) {
        $foundNotesFile = $file
        break
    }
}

if ($CreateNotesFile -and (-not $foundNotesFile)) {
    $targetFile = Join-Path $releaseNotesDir "$finalTag.md"
    $template = @"
# Release $finalTag

### Highlights
- 

### Improvements
- 

### Fixes
- 
"@
    [System.IO.File]::WriteAllText($targetFile, $template, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "📝 Created release notes template at: $targetFile" -ForegroundColor Yellow
    $foundNotesFile = $targetFile
}

$tagAnnotation = ""
if (-not [string]::IsNullOrWhiteSpace($Message)) {
    $tagAnnotation = $Message
} elseif ($foundNotesFile) {
    Write-Host "📄 Using release notes from: $foundNotesFile" -ForegroundColor Cyan
    $tagAnnotation = Get-Content -Path $foundNotesFile -Raw
} else {
    Write-Host "ℹ️ No release notes file found for $finalTag (Will use default release title)." -ForegroundColor DarkGray
    $tagAnnotation = "Release $finalTag (App Version: $baseVersion)"
}

# 4. Git Tag check & creation
$existingTags = git tag -l $finalTag
if ($existingTags) {
    Write-Host "⚠️  Tag '$finalTag' already exists locally." -ForegroundColor Yellow
    if ($Force) {
        Write-Host "Overwriting existing tag..." -ForegroundColor DarkYellow
        git tag -d $finalTag | Out-Null
    } else {
        $overwrite = Read-Host "Overwrite existing tag '$finalTag'? [y/N]"
        if ($overwrite -match "^[yY](es)?$") {
            git tag -d $finalTag | Out-Null
        } else {
            Write-Host "Aborted." -ForegroundColor Red
            exit 0
        }
    }
}

Write-Host "Creating annotated tag: $finalTag..." -ForegroundColor Cyan
git tag -a $finalTag -m "$tagAnnotation"
Write-Host "✅ Tag '$finalTag' created successfully!" -ForegroundColor Green

# 5. Push tag to remote
$shouldPush = $Push
if (-not $shouldPush) {
    Write-Host ""
    $resp = Read-Host "Do you want to push tag '$finalTag' to origin? [Y/n]"
    if ([string]::IsNullOrWhiteSpace($resp) -or $resp -match "^[yY](es)?$") {
        $shouldPush = $true
    }
}

if ($shouldPush) {
    Write-Host "🚀 Pushing tag '$finalTag' to origin..." -ForegroundColor Cyan
    if ($Force) {
        git push origin $finalTag --force
    } else {
        git push origin $finalTag
    }
    Write-Host "🎉 Successfully pushed tag '$finalTag' to origin!" -ForegroundColor Green
    Write-Host "⚡ GitHub Actions Release CD workflow has been triggered automatically!" -ForegroundColor Magenta
} else {
    Write-Host "To push this tag later, run:" -ForegroundColor Yellow
    Write-Host "   git push origin $finalTag" -ForegroundColor White
}
