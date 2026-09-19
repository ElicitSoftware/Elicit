# Elicit Superset image: the official Apache Superset release plus what the Elicit
# stack needs on top of it.
#
#   - Authlib           Keycloak single sign-on (AUTH_OAUTH in superset_config.py)
#   - Pillow            PDF output for Alerts & Reports
#   - Playwright        headless Chromium for server-side chart and dashboard screenshots
#   - psycopg2-binary   PostgreSQL driver for the metadata database and the reporting schema
#
# Configuration lives in /app/pythonpath (superset_config.py, elicit_security.py) and
# the Elicit bootstrap and init scripts in /app/elicit. Dashboard export ZIPs placed
# under /app/elicit/assets are imported by init.sh. See README.md in this directory.
FROM apache/superset:6.1.0

USER root

ENV PLAYWRIGHT_BROWSERS_PATH=/usr/local/share/playwright-browsers \
    PYTHONPATH=/app/pythonpath \
    SUPERSET_CONFIG_PATH=/app/pythonpath/superset_config.py

RUN . /app/.venv/bin/activate && \
    uv pip install \
        psycopg2-binary \
        Authlib \
        Pillow \
        playwright \
    && playwright install-deps \
    && PLAYWRIGHT_BROWSERS_PATH=/usr/local/share/playwright-browsers playwright install chromium \
    && chown -R superset:superset /usr/local/share/playwright-browsers

COPY --chown=superset:superset superset_config.py elicit_security.py /app/pythonpath/
COPY --chown=superset:superset bootstrap.py init.sh /app/elicit/
COPY --chown=superset:superset assets/ /app/elicit/assets/
RUN chmod +x /app/elicit/init.sh

USER superset

CMD ["/app/docker/entrypoints/run-server.sh"]
