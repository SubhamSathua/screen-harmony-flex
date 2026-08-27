#!/usr/bin/env bash
set -e

GRADLE_FILE="app/build.gradle.kts"

if [ ! -f "$GRADLE_FILE" ]; then
    echo "Error: Could not find $GRADLE_FILE" >&2
    exit 1
fi

CURRENT_CODE=$(grep -E '^\s*versionCode\s*=' "$GRADLE_FILE" | sed -E 's/.*=\s*([0-9]+).*/\1/')
CURRENT_NAME=$(grep -E '^\s*versionName\s*=' "$GRADLE_FILE" | sed -E 's/.*=\s*"([^"]+)".*/\1/')

if [ -z "$CURRENT_CODE" ] || [ -z "$CURRENT_NAME" ]; then
    echo "Error: Failed to parse versionCode or versionName in $GRADLE_FILE" >&2
    exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
MAJOR=${MAJOR:-0}
MINOR=${MINOR:-0}
PATCH=${PATCH:-0}

CURRENT_FORMATTED="$MAJOR.$MINOR.$PATCH"

echo "=========================================="
echo " ScreenHarmony Flex - Version Bump Script"
echo " Current Version: $CURRENT_FORMATTED (Code: $CURRENT_CODE)"
echo "=========================================="

CHOICE="$1"

if [ -z "$CHOICE" ]; then
    echo ""
    echo "Select version bump type:"
    echo "  [1] major  -> ($((MAJOR + 1)).0.0)"
    echo "  [2] minor  -> ($MAJOR.$((MINOR + 1)).0)"
    echo "  [3] patch  -> ($MAJOR.$MINOR.$((PATCH + 1)))"
    echo ""
    read -p "Enter choice [1/2/3 or major/minor/patch]: " CHOICE
fi

case "$CHOICE" in
    1|major|Major|MAJOR)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_MINOR=0
        NEW_PATCH=0
        ;;
    2|minor|Minor|MINOR)
        NEW_MAJOR=$MAJOR
        NEW_MINOR=$((MINOR + 1))
        NEW_PATCH=0
        ;;
    3|patch|Patch|PATCH)
        NEW_MAJOR=$MAJOR
        NEW_MINOR=$MINOR
        NEW_PATCH=$((PATCH + 1))
        ;;
    *)
        echo "Error: Invalid choice '$CHOICE'. Expected major, minor, or patch." >&2
        exit 1
        ;;
esac

NEW_VERSION_NAME="$NEW_MAJOR.$NEW_MINOR.$NEW_PATCH"
NEW_VERSION_CODE=$((CURRENT_CODE + 1))

# Replace in build.gradle.kts
sed -i -E "s/^([[:space:]]*versionCode[[:space:]]*=[[:space:]]*)[0-9]+/\1$NEW_VERSION_CODE/" "$GRADLE_FILE"
sed -i -E "s/^([[:space:]]*versionName[[:space:]]*=[[:space:]]*)\"[^\"]+\"/\1\"$NEW_VERSION_NAME\"/" "$GRADLE_FILE"

echo ""
echo "✅ Version successfully bumped!"
echo "   Version Name: $CURRENT_FORMATTED -> $NEW_VERSION_NAME"
echo "   Version Code: $CURRENT_CODE -> $NEW_VERSION_CODE"
echo "   Updated: $GRADLE_FILE"
