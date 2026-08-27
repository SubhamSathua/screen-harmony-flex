#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPS_FILE="$SCRIPT_DIR/version.properties"

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

CHOICE=""
CUSTOM_VERSION=""
CUSTOM_CODE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        -t|--type|-Type)
            CHOICE="$2"
            shift 2
            ;;
        -v|--version|-Version)
            CUSTOM_VERSION="$2"
            shift 2
            ;;
        -c|--code|-Code)
            CUSTOM_CODE="$2"
            shift 2
            ;;
        *)
            if [ -z "$CHOICE" ]; then
                CHOICE="$1"
            elif [ -z "$CUSTOM_VERSION" ]; then
                CUSTOM_VERSION="$1"
            fi
            shift
            ;;
    esac
done

echo "=========================================="
echo " ScreenHarmony Flex - Version Bump"
echo " Current Version: $CURRENT_FORMATTED (Code: $CODE)"
echo "=========================================="

if [ -z "$CHOICE" ]; then
    echo ""
    echo "Select version bump type:"
    echo "  [1] major  -> ($((MAJOR + 1)).0.0)"
    echo "  [2] minor  -> ($MAJOR.$((MINOR + 1)).0)"
    echo "  [3] patch  -> ($MAJOR.$MINOR.$((PATCH + 1)))"
    echo "  [4] custom -> (Specify custom version name & code)"
    echo ""
    read -p "Enter choice [1/2/3/4 or major/minor/patch/custom]: " CHOICE
fi

CHOICE=$(echo "$CHOICE" | tr '[:upper:]' '[:lower:]' | xargs)

if [[ "$CHOICE" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
    CUSTOM_VERSION="$CHOICE"
    CHOICE="custom"
fi

case "$CHOICE" in
    1|major)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_MINOR=0
        NEW_PATCH=0
        ;;
    2|minor)
        NEW_MAJOR=$MAJOR
        NEW_MINOR=$((MINOR + 1))
        NEW_PATCH=0
        ;;
    3|patch)
        NEW_MAJOR=$MAJOR
        NEW_MINOR=$MINOR
        NEW_PATCH=$((PATCH + 1))
        ;;
    4|custom)
        if [ -z "$CUSTOM_VERSION" ]; then
            echo ""
            read -p "Enter custom version name [X.Y.Z] (current: $CURRENT_FORMATTED): " CUSTOM_VERSION
            CUSTOM_VERSION=$(echo "$CUSTOM_VERSION" | xargs)
        fi

        if [[ "$CUSTOM_VERSION" =~ ^([0-9]+)\.([0-9]+)(\.([0-9]+))?$ ]]; then
            NEW_MAJOR="${BASH_REMATCH[1]}"
            NEW_MINOR="${BASH_REMATCH[2]}"
            NEW_PATCH="${BASH_REMATCH[4]:-0}"
        else
            echo "Error: Invalid version format '$CUSTOM_VERSION'. Expected format: X.Y or X.Y.Z (e.g., 2.0.0)" >&2
            exit 1
        fi

        if [ -z "$CUSTOM_CODE" ]; then
            read -p "Enter custom version code [integer] (default: $((CODE + 1))): " INPUT_CODE
            INPUT_CODE=$(echo "$INPUT_CODE" | xargs)
            if [ -n "$INPUT_CODE" ]; then
                CUSTOM_CODE="$INPUT_CODE"
            fi
        fi
        ;;
    *)
        echo "Error: Invalid choice '$CHOICE'. Expected major (1), minor (2), patch (3), or custom (4)." >&2
        exit 1
        ;;
esac

NEW_VERSION_NAME="$NEW_MAJOR.$NEW_MINOR.$NEW_PATCH"
NEW_VERSION_CODE="${CUSTOM_CODE:-$((CODE + 1))}"

if ! [[ "$NEW_VERSION_CODE" =~ ^[0-9]+$ ]]; then
    echo "Error: Invalid version code '$NEW_VERSION_CODE'. Expected a positive integer." >&2
    exit 1
fi

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
