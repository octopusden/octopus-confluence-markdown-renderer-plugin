#!/usr/bin/env bash
#
# Scenario tests for .github/scripts/release-jar.sh with a stubbed gh: release state in, verdict out.
#
# Usage: bash .github/scripts/test/release-jar-scenarios.sh   (from any directory)
set -uo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/release-jar.sh"
work="$(mktemp -d)"
trap 'rm -rf "${work}"' EXIT
pass=0; fail=0

mkdir -p "${work}/bin"
# The stub answers "release view" from FAKE_VIEW / FAKE_VIEW_RC and logs every call.
cat > "${work}/bin/gh" <<'EOF'
#!/usr/bin/env bash
echo "$*" >> "${FAKE_LOG}"
if [ "$1 $2" = "release view" ]; then printf '%s\n' "${FAKE_VIEW}"; exit "${FAKE_VIEW_RC:-0}"; fi
exit 0
EOF
chmod +x "${work}/bin/gh"

jar="${work}/markdown-renderer-plugin-2.1.0.jar"
echo "this build" > "${jar}"
ours="sha256:$(shasum -a 256 "${jar}" | cut -d' ' -f1)"
asset() { printf '{"assets":[{"name":"markdown-renderer-plugin-2.1.0.jar","state":"%s","digest":"%s"}]}' "$1" "$2"; }

# check <name> <mode> <gh view output> <gh view rc> <expected: ok|fail> [<gh call that must appear>]
check() {
  local name="$1" mode="$2" view="$3" rc="$4" want="$5" call="${6:-}" got=ok
  : > "${work}/log"
  PATH="${work}/bin:${PATH}" FAKE_LOG="${work}/log" FAKE_VIEW="${view}" FAKE_VIEW_RC="${rc}" \
    MODE="${mode}" TAG=v2.1.0 JAR="${jar}" bash "${script}" > "${work}/out" 2>&1 || got=fail
  if [ -n "${call}" ] && ! grep -qF -- "${call}" "${work}/log"; then got="${got}, no '${call}'"; fi
  if [ -z "${call}" ] && grep -q delete-asset "${work}/log"; then got="${got}, unexpected delete"; fi
  if [ "${got}" = "${want}" ]; then pass=$((pass + 1)); echo "ok   ${name}"
  else fail=$((fail + 1)); echo "FAIL ${name}: expected ${want}, got ${got}"; sed 's/^/    | /' "${work}/out"; fi
}

check "prepare: no release yet"                    prepare "release not found" 1 ok
check "prepare: release lookup error fails closed" prepare "HTTP 502: Bad Gateway" 1 fail
check "prepare: release without the jar"           prepare '{"assets":[]}' 0 ok
check "prepare: the same jar (retry of this run)"  prepare "$(asset uploaded "${ours}")" 0 ok
check "prepare: a jar from another build"          prepare "$(asset uploaded sha256:other)" 0 fail
check "prepare: an empty starter asset is deleted" prepare "$(asset starter "")" 0 ok "release delete-asset v2.1.0 markdown-renderer-plugin-2.1.0.jar --yes"
check "verify: the same jar"                       verify "$(asset uploaded "${ours}")" 0 ok
check "verify: no jar"                             verify '{"assets":[]}' 0 fail
check "verify: a starter asset"                    verify "$(asset starter "")" 0 fail
check "verify: a jar from another build"           verify "$(asset uploaded sha256:other)" 0 fail
check "verify: no release"                         verify "release not found" 1 fail

echo
echo "release-jar scenarios: ${pass} passed, ${fail} failed"
[ "${fail}" -eq 0 ]
