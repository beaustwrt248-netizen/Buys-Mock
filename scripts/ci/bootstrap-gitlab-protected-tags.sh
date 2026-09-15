#!/usr/bin/env bash
set -euo pipefail

test -n "${GITLAB_PROJECT_PATH:-}" || { echo 'GITLAB_PROJECT_PATH is required'; exit 1; }
test -n "${GITLAB_MIGRATION_TOKEN:-}" || { echo 'GITLAB_MIGRATION_TOKEN is not configured'; exit 1; }

project_id="$(python3 - <<'PY'
import os, urllib.parse
print(urllib.parse.quote(os.environ['GITLAB_PROJECT_PATH'], safe=''))
PY
)"
api="https://gitlab.com/api/v4/projects/${project_id}"
auth_header="PRIVATE-TOKEN: ${GITLAB_MIGRATION_TOKEN}"

cleanup() {
  rm -f /tmp/gitlab-protected-tag-response.json
}
trap cleanup EXIT

ensure_protected_tag() {
  local pattern="$1"
  local encoded_pattern
  encoded_pattern="$(python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$pattern")"
  local status
  status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' --header "$auth_header" "$api/protected_tags/$encoded_pattern")"

  if [[ "$status" == "200" ]]; then
    echo "Protected tag rule already exists: $pattern"
    return 0
  fi
  if [[ "$status" == "401" || "$status" == "403" ]]; then
    echo 'Protected-tag write access is not available on GITLAB_MIGRATION_TOKEN'
    exit 1
  fi
  if [[ "$status" != "404" ]]; then
    echo "GitLab protected-tag lookup failed for $pattern with HTTP $status"
    exit 1
  fi

  local write_status
  write_status="$(curl --silent --show-error --output /tmp/gitlab-protected-tag-response.json --write-out '%{http_code}' \
    --request POST --header "$auth_header" \
    --form "name=$pattern" --form 'create_access_level=40' \
    "$api/protected_tags")"
  if [[ "$write_status" == "401" || "$write_status" == "403" ]]; then
    echo 'Protected-tag write access is not available on GITLAB_MIGRATION_TOKEN'
    exit 1
  fi
  [[ "$write_status" =~ ^2 ]] || { echo "GitLab protected-tag create failed for $pattern with HTTP $write_status"; exit 1; }
  rm -f /tmp/gitlab-protected-tag-response.json
  echo "Created protected tag rule: $pattern"
}

ensure_protected_tag 'v*'
ensure_protected_tag 'admin-v*'
ensure_protected_tag 'nova-v*'
