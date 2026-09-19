"""Keycloak-aware security manager for the Elicit Superset deployment.

Flask-AppBuilder's built-in Keycloak handling reads roles from a ``groups`` claim. The
Elicit realm carries them as client roles of the ``elicit-superset`` client instead
(``resource_access.elicit-superset.roles`` in the userinfo response), so this manager
maps those to ``role_keys`` for ``AUTH_ROLES_MAPPING``.
"""
import logging
import os

from superset.security import SupersetSecurityManager

log = logging.getLogger(__name__)

OIDC_CLIENT_ID = os.environ.get("OIDC_CLIENT_ID", "elicit-superset")


class ElicitSecurityManager(SupersetSecurityManager):
    def oauth_user_info(self, provider, response=None):
        if provider != "keycloak":
            return super().oauth_user_info(provider, response)
        me = self.appbuilder.sm.oauth_remotes[provider].get("openid-connect/userinfo")
        me.raise_for_status()
        data = me.json()
        roles = data.get("resource_access", {}).get(OIDC_CLIENT_ID, {}).get("roles", [])
        log.debug("Keycloak userinfo for %s: roles=%s", data.get("preferred_username"), roles)
        return {
            "username": data.get("preferred_username", ""),
            "first_name": data.get("given_name", ""),
            "last_name": data.get("family_name", ""),
            "email": data.get("email", ""),
            "role_keys": roles,
        }
