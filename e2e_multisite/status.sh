#!/bin/bash
# Container status and an HTTP probe of every published URL of both sites.
cd "$(dirname "$0")"
for site in site1 site2; do
    echo "== $site containers"
    docker compose -f "$site/docker-compose.yml" ps 2>/dev/null || echo "(not running)"
done
echo
echo "== HTTP probes"
services=(
    "8080:Site 1 Survey" "8081:Site 1 Admin" "8083:Site 1 Pedigree"
    "8084:Site 1 Author" "8085:Site 1 Author preview" "8180:Keycloak (shared)"
    "8025:Mailpit (shared)" "16686:Site 1 Jaeger"
    "8030:Site 2 Survey" "8031:Site 2 Admin" "8033:Site 2 Pedigree"
    "16636:Site 2 Jaeger"
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
echo "PostgreSQL: site 1 localhost:5452, site 2 localhost:5402 (user survey / admin)"
