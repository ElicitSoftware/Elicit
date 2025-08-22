#!/bin/bash
# Export the 'elicit' realm from Keycloak 26.2.3 using the H2 database files from the running container
# This script copies the H2 files from the running container, prompts to stop the container, and then exports the realm.

set -e

CONTAINER_NAME="elicit-keycloak-1"

# Set export directory and filename with date
EXPORT_DIR="$(pwd)"
H2_DIR="$(pwd)/db"
REALM_NAME="elicit"
DATE_STR=$(date +"%m_%d_%Y")
EXPORT_FILE="$EXPORT_DIR/${REALM_NAME}_${DATE_STR}.json"
mkdir -p "$EXPORT_DIR"
mkdir -p "$H2_DIR"

echo "Copying H2 database files from running container ($CONTAINER_NAME)..."
docker cp "$CONTAINER_NAME":/opt/keycloak/data/h2/keycloakdb.mv.db "$H2_DIR/keycloakdb.mv.db"
docker cp "$CONTAINER_NAME":/opt/keycloak/data/h2/keycloakdb.trace.db "$H2_DIR/keycloakdb.trace.db" 2>/dev/null || true

echo "Please stop the Keycloak container ($CONTAINER_NAME) now to ensure database consistency, then press Enter to continue."
read -r

docker run --rm \
	-v "$H2_DIR:/h2" \
	-v "$EXPORT_DIR:/opt/keycloak/data/export" \
	quay.io/keycloak/keycloak:26.2.3 \
	export \
		--db-url="jdbc:h2:file:/h2/keycloakdb;NON_KEYWORDS=VALUE" \
		--realm "$REALM_NAME" \
		--users same_file \
		--file "/opt/keycloak/data/export/${REALM_NAME}_${DATE_STR}.json"