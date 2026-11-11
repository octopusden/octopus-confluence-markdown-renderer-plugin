#!/usr/bin/env bash
#
# Compute the next release version from the vX.Y.Z tags of the repository in the current directory.
#
# Inputs (env):
#   LEVEL          patch | minor | major: what to bump the latest tag by
#   VERSION_FLOOR  X.Y.Z: the lowest version to release; wins over a bumped tag that is lower
#   GITHUB_OUTPUT  file that receives version=X.Y.Z and sha=<HEAD>
#
# Tests: .github/scripts/test/release-version-scenarios.sh
set -euo pipefail

LEVEL="${LEVEL:?LEVEL is required}"
VERSION_FLOOR="${VERSION_FLOOR:?VERSION_FLOOR is required}"
GITHUB_OUTPUT="${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

latest="$(git tag --list 'v*' | grep -E '^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$' | sort -V | tail -n 1 || true)"
latest="${latest:-v0.0.0}"
IFS=. read -r major minor patch <<<"${latest#v}"
case "${LEVEL}" in
  major) major=$((major + 1)); minor=0; patch=0 ;;
  minor) minor=$((minor + 1)); patch=0 ;;
  patch) patch=$((patch + 1)) ;;
  *) echo "::error::Unknown increment level '${LEVEL}'." >&2; exit 1 ;;
esac
version="${major}.${minor}.${patch}"
if [ "$(printf '%s\n' "${version}" "${VERSION_FLOOR}" | sort -V | tail -n 1)" != "${version}" ]; then
  version="${VERSION_FLOOR}"
fi
if git rev-parse -q --verify "refs/tags/v${version}" >/dev/null; then
  echo "::error::Tag v${version} already exists." >&2
  exit 1
fi
echo "Latest tag ${latest}, releasing ${version}"
{
  echo "version=${version}"
  echo "sha=$(git rev-parse HEAD)"
} >> "${GITHUB_OUTPUT}"
