#!/bin/bash

# Elicit Platform Status Check (Docker Compose)

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=========================================="
echo "📊 ELICIT PLATFORM STATUS"
echo "=========================================="
echo ""

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running${NC}"
    exit 1
fi

# Show container status
echo -e "${BLUE}Container Status:${NC}"
echo "=================="
docker compose ps

echo ""
echo -e "${BLUE}Service Health Check:${NC}"
echo "===================="

# Check each service where it reports its health (port|name|path|kind):
#   quarkus  Survey, Admin, FHHS, Author and its preview: /q/health/ready. A 503 names the checks that are down and why,
#            e.g. FHHS before the Family History Survey is imported and built.
#   ok       Pedigree: /healthz answers 200 when the R service is up.
#   page     Keycloak, Mailpit: the front page; a redirect or sign-in prompt is a live service.
services=(
    "8080|Survey Platform|/q/health/ready|quarkus"
    "8081|Admin Dashboard|/q/health/ready|quarkus"
    "8082|Family History|/q/health/ready|quarkus"
    "8083|Pedigree|/healthz|ok"
    "8084|Author|/q/health/ready|quarkus"
    "8085|Author Preview|/q/health/ready|quarkus"
    "8180|Keycloak Auth|/|page"
    "8025|Email Testing|/|page"
)

# The DOWN checks of a SmallRye Health body, one per line, with the reason when one is given.
down_checks() {
    command -v python3 >/dev/null 2>&1 || return 0
    python3 -c '
import json, sys
try:
    body = json.load(sys.stdin)
except ValueError:
    sys.exit(0)
for check in body.get("checks", []):
    if check.get("status") != "UP":
        reason = (check.get("data") or {}).get("reason")
        print("     - " + check.get("name", "?") + (": " + str(reason) if reason else ""))
'
}

for service in "${services[@]}"; do
    IFS='|' read -r port name path kind <<< "$service"
    body=$(curl -s --max-time 5 -w $'\n%{http_code}' "http://localhost:$port$path" 2>/dev/null)
    response=${body##*$'\n'}
    body=${body%$'\n'*}
    [ -n "$response" ] || response=000

    if [[ "$response" == "000" ]]; then
        echo -e "❌ $name: Not accessible on port $port"
    elif [[ "$kind" == "page" && "$response" =~ ^(200|302|401)$ ]] || [[ "$kind" != "page" && "$response" == "200" ]]; then
        echo -e "✅ $name: http://localhost:$port"
    elif [[ "$kind" == "quarkus" && "$response" == "503" ]]; then
        echo -e "⚠️  $name: running but not ready (http://localhost:$port$path)"
        echo "$body" | down_checks
    else
        echo -e "⚠️  $name: Responding with HTTP $response on http://localhost:$port$path"
    fi
done

echo ""
echo -e "${BLUE}Quick Commands:${NC}"
echo "==============="
echo "• View logs:     docker compose logs -f [service-name]"
echo "• Restart all:   docker compose restart"
echo "• Stop all:      docker compose down"
echo "• Rebuild:       ./buildDockerImages.sh && docker compose up -d --build"

# Show recent logs for failed services
failed_services=$(docker compose ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null)
if [ ! -z "$failed_services" ]; then
    echo ""
    echo -e "${RED}Failed Services (recent logs):${NC}"
    echo "=============================="
    for service in $failed_services; do
        echo -e "${YELLOW}--- $service ---${NC}"
        docker compose logs --tail=5 $service 2>/dev/null
        echo ""
    done
fi