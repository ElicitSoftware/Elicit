#!/bin/bash
# Builds elicitsoftware/elicit_keycloak:latest, the development and evaluation Keycloak with
# elicit-realm.json built in, and smoke-tests it. docker-compose.yml runs this image at
# ELICIT_TAG, so with ELICIT_TAG=latest the local stack signs in against this build. Run by
# ../buildDockerImages.sh (target Keycloak); the published image comes from
# .github/workflows/release.yml.
set -euo pipefail
cd "$(dirname "$0")"

docker build -t elicitsoftware/elicit_keycloak:latest .
./smoke.sh elicitsoftware/elicit_keycloak:latest
