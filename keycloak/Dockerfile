# The development and evaluation Keycloak with the Elicit realm built in, so docker-compose.yml
# runs with nothing beside it. Evaluation only (umbrella C-018): the realm carries the published
# client secret and the seeded `admin`, `user` and `author` accounts. The multi-site suites
# still mount keycloak/ into the stock image.
#
# Published by .github/workflows/release.yml on each release tag, under the platform's version
# (the same number as every other Elicit image), after keycloak/smoke.sh passes. Locally,
# ../buildDockerImages.sh Keycloak (or ./buildDockerImage.sh here) builds and smoke-tests it as
# `latest`, which the stack runs with ELICIT_TAG=latest.
#
# The realm is imported on first start; a realm that already exists is left alone.
FROM quay.io/keycloak/keycloak:26.2.3

COPY elicit-realm.json /opt/keycloak/data/import/elicit-realm.json

CMD ["start-dev", "--import-realm"]
