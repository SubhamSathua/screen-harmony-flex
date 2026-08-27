#!/usr/bin/env bash
set -e

PROPS_FILE="version.properties"

if [ ! -f "$PROPS_FILE" ]; then
    echo "Error: Could not find $PROPS_FILE" >&2
    exit 1
fi

get_prop() {
    grep "^$1=" "$PROPS_FILE" | cut -d'=' -f2 | tr -d ' \r\n'
}

MAJOR=$(get_prop "VERSION_MAJOR")
MINOR=$(get_prop "VERSION_MINOR")
PATCH=$(get_prop "VERSION_PATCH")
CODE=$(get_prop "VERSION_CODE")

MAJOR=${MAJOR:-0}
MINOR=${MINOR:-1}
PATCH=${PATCH:-0}
CODE=${CODE:-1}

CURRENT_FORMATTED="$MAJOR.$MINOR.$PATCH"

echo "=========================================="
echo " ScreenHarmony Flex - Version Bump"
echo " Current Version: $CURRENT_FORMATTED (Code: $CODE)"
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
NEW_VERSION_CODE=$((CODE + 1))

# Write to version.properties (No Gradle Sync needed!)
cat <<EOF > "$PROPS_FILE"
VERSION_MAJOR=$NEW_MAJOR
VERSION_MINOR=$NEW_MINOR
VERSION_PATCH=$NEW_PATCH
VERSION_CODE=$NEW_VERSION_CODE
EOF

echo ""
echo "✅ Version successfully bumped without Gradle script changes!"
echo "   Version Name: $CURRENT_FORMATTED -> $NEW_VERSION_NAME"
echo "   Version Code: $CODE -> $NEW_VERSION_CODE"
echo "   Target: version.properties (Zero Gradle Sync required)"
