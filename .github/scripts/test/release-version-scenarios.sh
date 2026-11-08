#!/usr/bin/env bash
#
# Scenario tests for .github/scripts/release-version.sh: tags and a bump level in, the released version out.
#
# Usage: bash .github/scripts/test/release-version-scenarios.sh   (from any directory)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/release-version.sh"
work="$(mktemp -d)"
trap 'rm -rf "${work}"' EXIT
pass=0; fail=0

# check <name> <tags> <level> <expected version, or "fail">
check() {
  local name="$1" tags="$2" level="$3" want="$4" repo got rc=0
  repo="$(mktemp -d "${work}/repo.XXXX")"
  git -C "${repo}" init -q
  git -C "${repo}" -c user.name=test -c user.email=test@example.com commit -q --allow-empty -m init
  for t in ${tags}; do git -C "${repo}" tag "${t}"; done

  (cd "${repo}" && LEVEL="${level}" VERSION_FLOOR=2.1.0 GITHUB_OUTPUT="${repo}/out" bash "${script}") >"${repo}/log" 2>&1 || rc=$?
  if [ "${want}" = "fail" ]; then
    [ "${rc}" -ne 0 ] && got=fail || got="$(sed -n 's/^version=//p' "${repo}/out")"
  else
    got="$(sed -n 's/^version=//p' "${repo}/out" 2>/dev/null)"
    [ "${rc}" -eq 0 ] || got="fail (rc=${rc})"
    # The released commit must be the checked-out one.
    if [ "${rc}" -eq 0 ] && ! grep -qx "sha=$(git -C "${repo}" rev-parse HEAD)" "${repo}/out"; then got="${got}, wrong sha"; fi
  fi

  if [ "${got}" = "${want}" ]; then pass=$((pass + 1)); echo "ok   ${name}"
  else fail=$((fail + 1)); echo "FAIL ${name}: expected ${want}, got ${got}"; sed 's/^/    | /' "${repo}/log"; fi
}

check "old plugin tags, patch: floor wins"     "v2.0.0 v2.0.1"          patch 2.1.0
check "old plugin tags, minor: equals floor"   "v2.0.0 v2.0.1"          minor 2.1.0
check "old plugin tags, major: above floor"    "v2.0.0 v2.0.1"          major 3.0.0
check "no tags: floor"                         ""                       patch 2.1.0
check "after first release, patch"             "v2.0.1 v2.1.0"          patch 2.1.1
check "after first release, minor"             "v2.0.1 v2.1.0"          minor 2.2.0
check "numeric, not lexical, ordering"         "v2.1.0 v2.1.9 v2.1.10"  patch 2.1.11
check "non-release tags are ignored"           "v2.1.0 v2.2 v2.5.0-rc1 x9.0.0 v09.0.0" patch 2.1.1
check "unknown level fails"                    "v2.1.0"                 hotfix fail

echo
echo "release-version scenarios: ${pass} passed, ${fail} failed"
[ "${fail}" -eq 0 ]
