#!/usr/bin/env bash
# Applies Atlassian's public 3-hour "10 user Confluence Data Center" timebomb license
# to the local test Confluence from docker-compose.yml. Re-run every 3 hours (or after a restart).
# Key source: https://developer.atlassian.com/platform/marketplace/timebomb-licenses-for-testing-server-apps/
#
# Admin credentials: CONFLUENCE_ADMIN_USER / CONFLUENCE_ADMIN_PASSWORD, or a git-ignored
# confluence-test-admin.env in the repo root with the same two variables.
# Fresh instance (setup wizard still open): run with --print-key and paste the key into the wizard.
set -euo pipefail

BASE_URL="${CONFLUENCE_URL:-http://localhost:8090}"

KEY='AAABtQ0ODAoPeNp9kV9v0zAUxd/9Ka7EWyWnTmESqxSJNQlbxdJUTbLBgAfXuV0NqR3ZTqHfHjdpY
VSCB7/4/jm/e86rR6wh4wdgE2Bsyq6nLITbrIQJC9+SRbdbo8k3lUVjo5CRWCvHhVvwHUZ1y42Rd
vuOu4ZbK7kKhN4RodUm8D1yj5EzHZJlZ8SWW0y4w+i4lrIrykJyLwUqi+WhxX5fnGdZuornN/fnU
vqzlebQzy1f353F04zL5l/qBZo9mnkSzW6vS/qxenhDPzw93dEZCx8HtBeyLyX7mpfiMSqHZkAvu
rUVRrZOajX8jEajRV7S9/mKLld5UsXlPF/Qqkh9IYoNetYa1gdwW4STEqRK6BoNtEZ/Q+Hg89a59
st0PH7WwV/042aYoDhMfA0g0aC0g1paZ+S6c+g3SwtOg+is0zufS0C8IZ5ZcSUuLfNU8Sq9KdOEz
j4dEf8XWuG4+X36Cd47WanvSv9QpEgXkX/0ijGSm2eupOW9MQnusdGtv7BE685nk94NX7/M/TKFy
/BPJjz4047bJyTBPyH0CqcO2GgDvG2hPgNYku550w1YG954il/X0fxXMC0CFQCRUd9kwqDYeFIFJ
yQmlQPeMMYDLQIUYpH3kyyXea6e1PzAN2rpSuuUl4M=X02l1'

if [[ "${1:-}" == "--print-key" ]]; then printf '%s\n' "$KEY"; exit 0; fi

ENV_FILE="$(dirname "$0")/../confluence-test-admin.env"
if [[ -f "$ENV_FILE" ]]; then set -a; . "$ENV_FILE"; set +a; fi
: "${CONFLUENCE_ADMIN_USER:?set CONFLUENCE_ADMIN_USER (or create confluence-test-admin.env)}"
: "${CONFLUENCE_ADMIN_PASSWORD:?set CONFLUENCE_ADMIN_PASSWORD (or create confluence-test-admin.env)}"

die() { echo "ERROR: $*" >&2; exit 1; }
state() { curl -fsS "$BASE_URL/status" 2>/dev/null | sed -n 's/.*"state":"\([A-Z_]*\)".*/\1/p' || true; }

echo "Waiting for Confluence at $BASE_URL ..."
for _ in $(seq 1 120); do
  s="$(state)"
  [[ "$s" == "RUNNING" || "$s" == "FIRST_RUN" ]] && break
  sleep 5
done
[[ "$s" == "RUNNING" ]] || die "state is '${s:-unreachable}'. If the setup wizard is open, paste the output of '$0 --print-key' into it; then re-run."

JAR="$(mktemp)"; PAGE="$(mktemp)"; trap 'rm -f "$JAR" "$PAGE"' EXIT
curl_c() { curl -sS -c "$JAR" -b "$JAR" "$@"; }
token() { sed -n 's/.*name="atl_token" value="\([^"]*\)".*/\1/p' "$1" | head -1; }

curl_c -o /dev/null "$BASE_URL/login.action"
code="$(curl_c -o /dev/null -w '%{http_code}' \
  --data-urlencode "os_username=$CONFLUENCE_ADMIN_USER" --data-urlencode "os_password=$CONFLUENCE_ADMIN_PASSWORD" \
  --data-urlencode "login=Log in" --data-urlencode "os_destination=/admin/license.action" "$BASE_URL/dologin.action")"
[[ "$code" == 302 ]] || die "login failed (HTTP $code), check admin credentials"

# websudo: the license page asks to re-confirm the admin password
url="$(curl_c -L -o "$PAGE" -w '%{url_effective}' "$BASE_URL/admin/license.action")"
if [[ "$url" == *authenticate.action* ]]; then
  curl_c -o /dev/null --data-urlencode "atl_token=$(token "$PAGE")" --data-urlencode "password=$CONFLUENCE_ADMIN_PASSWORD" \
    --data-urlencode "authenticate=Confirm" --data-urlencode "destination=/admin/license.action" "$BASE_URL/doauthenticate.action"
  curl_c -L -o "$PAGE" "$BASE_URL/admin/license.action"
fi

printf '%s' "$KEY" | curl_c -L -o "$PAGE" \
  --data-urlencode "atl_token=$(token "$PAGE")" --data-urlencode "licenseString@-" --data-urlencode "update=Save" \
  "$BASE_URL/admin/doupdatelicense.action"

if grep -q 'expired on' "$PAGE" || ! grep -q 'expires on' "$PAGE"; then die "license was not applied"; fi
echo "OK: timebomb license applied, valid ~3 hours (re-run after that)."
