# Release Notes Directory

This directory stores version-specific release notes consumed by the **GitHub Actions Release CD Pipeline** and the **`tag-and-push` scripts**.

### 📁 Naming Conventions
Place a markdown file named according to the version or tag:
- `v2.8.5.md` (Standard tag format: `vX.Y.Z.md`)
- `2.8.5.md` (Semantic version format: `X.Y.Z.md`)
- `v2.8.6-alpha.md` (Pre-release format: `vX.Y.Z-<suffix>.md`)

### 🤖 CI/CD Integration & Fallback Behavior
- When a release or tag workflow runs, it searches for `.github/release-notes/<tag>.md` or `<version>.md`.
- **No Error if Missing**: If a release note file does not exist for a tag, the pipeline will **NOT fail**. It will gracefully fall back to a default release summary and generate GitHub commit notes automatically.
- Pre-releases are automatically flagged in GitHub Releases whenever the tag contains `-alpha`, `-beta`, `-rc`, `-preview`, `-dev`, or when manually triggered with `is_prerelease: true`.
