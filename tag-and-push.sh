#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROPS_FILE="$SCRIPT_DIR/version.properties"
RELEASE_NOTES_DIR="$SCRIPT_DIR/.github/release-notes"

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

BASE_VERSION="$MAJOR.$MINOR.$PATCH"
DEFAULT_TAG="v$BASE_VERSION"

TAG=""
SUFFIX=""
PRERELEASE=false
MESSAGE=""
AUTO_PUSH=false
FORCE=false
CREATE_NOTES=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -t|--tag|-Tag)
            TAG="$2"
            shift 2
            ;;
        -p|--prerelease|--pre|-PreRelease)
            PRERELEASE=true
            shift
            ;;
        -s|--suffix|-Suffix)
            SUFFIX="$2"
            shift 2
            ;;
        -m|--message|-Message)
            MESSAGE="$2"
            shift 2
            ;;
        --push|-Push)
            AUTO_PUSH=true
            shift
            ;;
        -f|--force|-Force)
            FORCE=true
            shift
            ;;
        --create-notes|-CreateNotesFile)
            CREATE_NOTES=true
            shift
            ;;
        *)
            if [ -z "$TAG" ]; then
                TAG="$1"
            fi
            shift
            ;;
    esac
done

if [ -z "$TAG" ]; then
    if [ "$PRERELEASE" = true ] && [ -z "$SUFFIX" ]; then
        SUFFIX="alpha"
    fi
    if [ -n "$SUFFIX" ]; then
        CLEAN_SUFFIX="${SUFFIX#-}"
        FINAL_TAG="${DEFAULT_TAG}-${CLEAN_SUFFIX}"
    else
        FINAL_TAG="$DEFAULT_TAG"
    fi
else
    if [[ "$TAG" != v* ]]; then
        FINAL_TAG="v$TAG"
    else
        FINAL_TAG="$TAG"
    fi
fi

echo "=========================================="
echo " ScreenHarmony Flex - Tag & Push System"
echo " App Version: $BASE_VERSION (Code: $CODE)"
echo " Target Tag : $FINAL_TAG"
echo "=========================================="

mkdir -p "$RELEASE_NOTES_DIR"

NOTES_FILE=""
if [ -f "$RELEASE_NOTES_DIR/${FINAL_TAG}.md" ]; then
    NOTES_FILE="$RELEASE_NOTES_DIR/${FINAL_TAG}.md"
elif [ -f "$RELEASE_NOTES_DIR/${BASE_VERSION}.md" ]; then
    NOTES_FILE="$RELEASE_NOTES_DIR/${BASE_VERSION}.md"
elif [ -f "$RELEASE_NOTES_DIR/v${BASE_VERSION}.md" ]; then
    NOTES_FILE="$RELEASE_NOTES_DIR/v${BASE_VERSION}.md"
elif [ -f "$SCRIPT_DIR/${FINAL_TAG}.md" ]; then
    NOTES_FILE="$SCRIPT_DIR/${FINAL_TAG}.md"
fi

if [ "$CREATE_NOTES" = true ] && [ -z "$NOTES_FILE" ]; then
    TARGET_NOTE="$RELEASE_NOTES_DIR/${FINAL_TAG}.md"
    cat << EOF > "$TARGET_NOTE"
# Release $FINAL_TAG

### Highlights
- 

### Improvements
- 

### Fixes
- 
EOF
    echo "📝 Created release notes template at: $TARGET_NOTE"
    NOTES_FILE="$TARGET_NOTE"
fi

if [ -n "$MESSAGE" ]; then
    ANNOTATION="$MESSAGE"
elif [ -n "$NOTES_FILE" ]; then
    echo "📄 Using release notes from: $NOTES_FILE"
    ANNOTATION=$(cat "$NOTES_FILE")
else
    echo "ℹ️ No release notes file found for $FINAL_TAG (Will use default release title)."
    ANNOTATION="Release $FINAL_TAG (App Version: $BASE_VERSION)"
fi

if git rev-parse "$FINAL_TAG" >/dev/null 2>&1; then
    echo "⚠️ Tag '$FINAL_TAG' already exists locally."
    if [ "$FORCE" = true ]; then
        git tag -d "$FINAL_TAG"
    else
        read -p "Overwrite existing tag '$FINAL_TAG'? [y/N]: " OVERWRITE
        if [[ "$OVERWRITE" =~ ^[yY](es)?$ ]]; then
            git tag -d "$FINAL_TAG"
        else
            echo "Aborted."
            exit 0
        fi
    fi
fi

echo "Creating annotated tag: $FINAL_TAG..."
git tag -a "$FINAL_TAG" -m "$ANNOTATION"
echo "✅ Tag '$FINAL_TAG' created successfully!"

SHOULD_PUSH="$AUTO_PUSH"
if [ "$SHOULD_PUSH" = false ]; then
    echo ""
    read -p "Do you want to push tag '$FINAL_TAG' to origin? [Y/n]: " RESP
    if [ -z "$RESP" ] || [[ "$RESP" =~ ^[yY](es)?$ ]]; then
        SHOULD_PUSH=true
    fi
fi

if [ "$SHOULD_PUSH" = true ]; then
    echo "🚀 Pushing tag '$FINAL_TAG' to origin..."
    if [ "$FORCE" = true ]; then
        git push origin "$FINAL_TAG" --force
    else
        git push origin "$FINAL_TAG"
    fi
    echo "🎉 Successfully pushed tag '$FINAL_TAG' to origin!"
    echo "⚡ GitHub Actions Release CD workflow has been triggered automatically!"
else
    echo "To push this tag later, run:"
    echo "   git push origin $FINAL_TAG"
fi
