#!/usr/bin/env bash
# Smoke-tests an elicit_keycloak image before it is published (.github/workflows/release.yml),
# and can be run by hand the same way:
#
#   keycloak/smoke.sh elicitsoftware/elicit_keycloak:candidate
#
# Starts the image on a spare port, waits for the elicit realm to answer its OIDC discovery,
# then asks the admin API for the clients the applications sign in through. Anything missing
# fails the run. The bootstrap admin is this script's own throwaway, not a secret.
set -euo pipefail

image=${1:?usage: smoke.sh <image>}
port=${SMOKE_PORT:-18180}
name=elicit-keycloak-smoke-$$
base=http://localhost:$port

docker run -d --rm --name "$name" -p "$port:8080" \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=smoke -e KC_BOOTSTRAP_ADMIN_PASSWORD=smoke \
  "$image" >/dev/null
trap 'docker rm -f "$name" >/dev/null 2>&1 || true' EXIT

ready=
for _ in $(seq 1 60); do
  if curl -fsS "$base/realms/elicit/.well-known/openid-configuration" >/dev/null 2>&1; then
    ready=1; break
  fi
  sleep 3
done
if [ -z "$ready" ]; then
  echo "FAIL: the elicit realm did not answer within 3 minutes" >&2
  docker logs "$name" 2>&1 | tail -40 >&2
  exit 1
fi
echo "ok: realm elicit is served"

token=$(curl -fsS "$base/realms/master/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=admin-cli -d username=smoke -d password=smoke |
  python3 -I -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

status=0
for client in elicit-admin elicit-author; do
  found=$(curl -fsS -H "Authorization: Bearer $token" \
    "$base/admin/realms/elicit/clients?clientId=$client" |
    python3 -I -c 'import json,sys; print(len(json.load(sys.stdin)))')
  if [ "$found" = 1 ]; then
    echo "ok: client $client"
  else
    echo "FAIL: client $client is missing from the realm" >&2
    status=1
  fi
done
exit $status
