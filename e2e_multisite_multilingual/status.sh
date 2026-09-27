#!/bin/bash
# Container status and an HTTP probe of every published URL of all three sites.
cd "$(dirname "$0")"
for site in usa mexico arabia; do
    echo "== $site containers"
    docker compose -f "$site/docker-compose.yml" ps 2>/dev/null || echo "(not running)"
done
echo
echo "== HTTP probes"
services=(
    "8080:USA Survey (English only)" "8081:USA Admin" "8084:USA Author" "8085:USA Author preview"
    "8180:Keycloak (shared)" "8025:Mailpit (shared)"
    "8030:Mexico Survey (es-419)" "8031:Mexico Admin"
    "7980:Arabia Survey (ar)" "7981:Arabia Admin"
)
for service in "${services[@]}"; do
    port=${service%%:*}
    name=${service#*:}
    response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port" 2>/dev/null || echo "000")
    if [[ "$response" =~ ^(200|302|401)$ ]]; then
        echo "✅ $name: http://localhost:$port"
    elif [[ "$response" == "000" ]]; then
        echo "❌ $name: not accessible on port $port"
    else
        echo "⚠️  $name: HTTP $response on port $port"
    fi
done
echo
echo "PostgreSQL: USA localhost:5452, Mexico localhost:5402, Arabia localhost:5352 (user survey / admin)"
echo "Translations mounts: i18n/usa (none), i18n/mexico (es-419), i18n/arabia (ar)"
