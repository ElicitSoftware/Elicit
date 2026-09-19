# Elicit configuration for Apache Superset 6.1.
#
# Everything deployment-specific comes from the environment (see docker-compose.yml in
# the umbrella repo). The analytics design is documented in
# Admin/docs/research/Superset.md and Admin/docs/plan/superset-analytics-implementation.md.
import os

from celery.schedules import crontab
from flask_appbuilder.security.manager import AUTH_OAUTH

from elicit_security import ElicitSecurityManager, OIDC_CLIENT_ID


def _require(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(f"Environment variable {name} is required")
    return value


# --- Core -------------------------------------------------------------------------
SECRET_KEY = _require("SUPERSET_SECRET_KEY")
SQLALCHEMY_DATABASE_URI = _require("SUPERSET_DB_URI")
REDIS_URL = os.environ.get("SUPERSET_REDIS_URL", "redis://redis:6379/0")
PUBLIC_URL = os.environ.get("SUPERSET_PUBLIC_URL", "http://localhost:8088").rstrip("/")

ENABLE_PROXY_FIX = True
ROW_LIMIT = 5000
SUPERSET_WEBSERVER_TIMEOUT = 120
# Jinja in SQL/RLS stays off (research section 9); SQL Lab is not granted to analysts.
ENABLE_TEMPLATE_PROCESSING = False

FEATURE_FLAGS = {
    "EMBEDDED_SUPERSET": True,              # guest tokens for the Admin Analytics view
    "ALERT_REPORTS": True,                  # server-side PNG/PDF via Celery + Playwright
    "THUMBNAILS": True,
    "PLAYWRIGHT_REPORTS_AND_THUMBNAILS": True,
    "DASHBOARD_RBAC": True,                 # dashboards can be shared with roles
}

# --- Caches (Redis) -----------------------------------------------------------------
def _redis_cache(prefix: str, timeout: int = 300) -> dict:
    return {
        "CACHE_TYPE": "RedisCache",
        "CACHE_DEFAULT_TIMEOUT": timeout,
        "CACHE_KEY_PREFIX": prefix,
        "CACHE_REDIS_URL": REDIS_URL,
    }


CACHE_CONFIG = _redis_cache("superset_")
DATA_CACHE_CONFIG = _redis_cache("superset_data_")
FILTER_STATE_CACHE_CONFIG = _redis_cache("superset_filter_", 86400)
EXPLORE_FORM_DATA_CACHE_CONFIG = _redis_cache("superset_explore_", 86400)
THUMBNAIL_CACHE_CONFIG = _redis_cache("superset_thumb_", 86400)


# --- Celery (worker + beat) ---------------------------------------------------------
class CeleryConfig:
    broker_url = REDIS_URL
    result_backend = REDIS_URL
    broker_connection_retry_on_startup = True
    imports = (
        "superset.sql_lab",
        "superset.tasks.scheduler",
        "superset.tasks.thumbnails",
        "superset.tasks.cache",
    )
    worker_prefetch_multiplier = 10
    task_acks_late = True
    beat_schedule = {
        "reports.scheduler": {"task": "reports.scheduler", "schedule": crontab(minute="*", hour="*")},
        "reports.prune_log": {"task": "reports.prune_log", "schedule": crontab(minute=0, hour=0)},
    }


CELERY_CONFIG = CeleryConfig

# --- Screenshots and reports --------------------------------------------------------
WEBDRIVER_TYPE = "playwright"
# The worker reaches the web app on the compose network; recipients see the public URL.
WEBDRIVER_BASEURL = os.environ.get("SUPERSET_INTERNAL_URL", "http://superset:8088/")
WEBDRIVER_BASEURL_USER_FRIENDLY = PUBLIC_URL + "/"
WEBDRIVER_WINDOW = {"dashboard": (1600, 2000), "slice": (800, 600)}
SCREENSHOT_LOCATE_WAIT = 100
SCREENSHOT_LOAD_WAIT = 600

SMTP_HOST = os.environ.get("SUPERSET_SMTP_HOST", "mailpit")
SMTP_PORT = int(os.environ.get("SUPERSET_SMTP_PORT", "1025"))
SMTP_STARTTLS = False
SMTP_SSL = False
SMTP_USER = ""
SMTP_PASSWORD = ""
SMTP_MAIL_FROM = os.environ.get("SUPERSET_MAIL_FROM", "analytics@localhost")
EMAIL_REPORTS_SUBJECT_PREFIX = "[Elicit Analytics] "

# --- Embedding (Admin Analytics view, UC-020) ---------------------------------------
GUEST_ROLE_NAME = "ElicitGuest"                       # created by bootstrap.py
GUEST_TOKEN_JWT_SECRET = _require("SUPERSET_GUEST_TOKEN_SECRET")
GUEST_TOKEN_JWT_ALGO = "HS256"
GUEST_TOKEN_JWT_EXP_SECONDS = int(os.environ.get("SUPERSET_GUEST_TOKEN_EXP_SECONDS", "300"))
GUEST_TOKEN_JWT_AUDIENCE = os.environ.get("SUPERSET_GUEST_TOKEN_AUDIENCE") or None
GUEST_TOKEN_HEADER_NAME = "X-GuestToken"

_frame_ancestors = [o.strip() for o in os.environ.get("SUPERSET_FRAME_ANCESTORS", "http://localhost:8081").split(",") if o.strip()]
TALISMAN_ENABLED = True
TALISMAN_CONFIG = {
    "content_security_policy": {
        "base-uri": ["'self'"],
        "default-src": ["'self'"],
        "img-src": ["'self'", "blob:", "data:"],
        "worker-src": ["'self'", "blob:"],
        "connect-src": ["'self'"],
        "object-src": "'none'",
        "style-src": ["'self'", "'unsafe-inline'"],
        "script-src": ["'self'", "'strict-dynamic'"],
        "frame-ancestors": ["'self'"] + _frame_ancestors,
    },
    "content_security_policy_nonce_in": ["script-src"],
    "force_https": False,
    "session_cookie_secure": PUBLIC_URL.startswith("https://"),
}

# --- Keycloak single sign-on --------------------------------------------------------
_oidc_server = _require("OIDC_SERVER_URL").rstrip("/")   # e.g. http://host.docker.internal:8180/realms/elicit
AUTH_TYPE = AUTH_OAUTH
OAUTH_PROVIDERS = [
    {
        "name": "keycloak",
        "icon": "fa-key",
        "token_key": "access_token",
        "remote_app": {
            "client_id": OIDC_CLIENT_ID,
            "client_secret": _require("OIDC_CLIENT_SECRET"),
            "server_metadata_url": f"{_oidc_server}/.well-known/openid-configuration",
            "api_base_url": f"{_oidc_server}/protocol/",
            "client_kwargs": {"scope": "openid email profile"},
        },
    }
]
AUTH_USER_REGISTRATION = True             # first Keycloak login creates the Superset user
AUTH_USER_REGISTRATION_ROLE = "Public"    # no data access unless a mapped role applies
AUTH_ROLES_SYNC_AT_LOGIN = True           # re-read Keycloak roles on every login
AUTH_ROLES_MAPPING = {
    "elicit_analytics": ["ElicitAnalyst"],
    "elicit_superset_admin": ["Admin"],
}
CUSTOM_SECURITY_MANAGER = ElicitSecurityManager

# --- Brand ---------------------------------------------------------------------------
THEME_DEFAULT = {
    "token": {
        "brandAppName": "Elicit Analytics",
        "brandLogoAlt": "Elicit",
    }
}
