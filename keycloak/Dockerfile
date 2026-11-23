# The development Keycloak with the Elicit realm built in, so the standalone compose file
# (docker-compose.standalone.yml) runs without this repository. Evaluation only (umbrella
# C-018): the realm carries the published client secret and the seeded `admin` and `user`
# accounts. Development keeps mounting keycloak/ in docker-compose.yml.
#
# Published by .github/workflows/release.yml on each release tag, under the platform's version
# (the same number as every other Elicit image), after keycloak/smoke.sh passes. To try a
# change locally:
#
#   docker build -t elicitsoftware/elicit_keycloak:candidate keycloak/
#   keycloak/smoke.sh elicitsoftware/elicit_keycloak:candidate
#
# The realm is imported on first start; a realm that already exists is left alone.
FROM quay.io/keycloak/keycloak:26.2.3

COPY elicit-realm.json /opt/keycloak/data/import/elicit-realm.json

CMD ["start-dev", "--import-realm"]
