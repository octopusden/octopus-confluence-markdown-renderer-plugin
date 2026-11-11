#!/usr/bin/env bash
#
# Guard the plugin jar on the GitHub Release around octopus-base's tag-and-release.sh, which accepts
# any asset whose name matches and only warns when an upload fails. The jar is compared by content
# (the asset's sha256 digest), not by name.
#
# Inputs (env):
#   MODE     prepare: before tag-and-release.sh. A release without the jar is fine; an empty asset left
#                     by a failed upload (state "starter") is deleted so the jar is uploaded again;
#                     a jar with other content (a draft adopted from another build) fails the job.
#            verify:  after tag-and-release.sh. The release must hold this exact jar, fully uploaded.
#   TAG      the release tag, e.g. v2.1.0
#   JAR      path to the jar this run built
#   GH_TOKEN, GH_REPO  as for gh
#
# Tests: .github/scripts/test/release-jar-scenarios.sh
set -euo pipefail

MODE="${MODE:?MODE is required}"
TAG="${TAG:?TAG is required}"
JAR="${JAR:?JAR is required}"
name="$(basename "${JAR}")"
want="sha256:$(shasum -a 256 "${JAR}" | cut -d' ' -f1)"

lookup=0
out="$(gh release view "${TAG}" --json assets 2>&1)" || lookup=$?
if [ "${lookup}" -ne 0 ]; then
  # Fail closed: gh also exits non-zero on auth errors, rate limits and 5xx.
  if [ "${MODE}" = prepare ] && grep -q 'release not found' <<<"${out}"; then
    echo "Release ${TAG} does not exist yet."
    exit 0
  fi
  printf '%s\n' "${out}" >&2
  echo "::error::Could not read release ${TAG}." >&2
  exit 1
fi

asset="$(jq -c --arg n "${name}" '.assets[] | select(.name == $n)' <<<"${out}")"
state="$(jq -r '.state // empty' <<<"${asset:-null}")"
digest="$(jq -r '.digest // empty' <<<"${asset:-null}")"

case "${MODE}" in
  prepare)
    if [ -z "${asset}" ]; then
      echo "Release ${TAG} has no ${name} yet."
    elif [ "${state}" != uploaded ]; then
      echo "Release ${TAG} has an incomplete ${name} (state ${state}); deleting it so it is uploaded again."
      gh release delete-asset "${TAG}" "${name}" --yes
    elif [ "${digest}" != "${want}" ]; then
      echo "::error::Release ${TAG} already has a ${name} with other content (${digest:-no digest}, this run built ${want}). It was built from another commit; delete the release and its tag, or release a new version." >&2
      exit 1
    else
      echo "Release ${TAG} already has this run's ${name}."
    fi
    ;;
  verify)
    if [ -z "${asset}" ] || [ "${state}" != uploaded ] || [ "${digest}" != "${want}" ]; then
      echo "::error::Release ${TAG} does not hold this run's ${name} (state ${state:-missing}, ${digest:-no digest}, expected ${want}). Re-run the failed jobs to attach it." >&2
      exit 1
    fi
    echo "Release ${TAG} holds ${name} (${want})."
    ;;
  *)
    echo "::error::Unknown MODE '${MODE}'." >&2
    exit 1
    ;;
esac
