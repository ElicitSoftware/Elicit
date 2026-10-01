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

# Check each service
services=("8080:Survey Platform" "8081:Admin Dashboard" "8082:Family History" "8180:Keycloak Auth" "8025:Email Testing")

for service in "${services[@]}"; do
    port=${service%%:*}
    name=${service##*:}
    
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port 2>/dev/null || echo "000")
    
    if [[ "$response" =~ ^(200|302|401)$ ]]; then
        echo -e "✅ $name: http://localhost:$port"
    elif [[ "$response" == "000" ]]; then
        echo -e "❌ $name: Not accessible on port $port"
    else
        echo -e "⚠️  $name: Responding with HTTP $response on port $port"
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