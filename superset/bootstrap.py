"""Idempotent Elicit bootstrap, run by init.sh before and after the asset import.

- Creates or updates the "Elicit Reporting" database connection (read-only
  surveyreport_user, fixed UUID so exported datasets can reference it).
- Creates the ElicitAnalyst role (Gamma without SQL Lab) and the ElicitGuest role
  (what guest tokens from the Admin Analytics view run as).
- Grants both roles datasource access on every dataset of the reporting connection.
"""
import os
import uuid

from superset.app import create_app

REPORTING_DB_NAME = "Elicit Reporting"
REPORTING_DB_UUID = uuid.UUID("7e1a1c2e-6b1d-4f7e-9a3f-0e2b9c1a5d10")
SQL_LAB_MENUS = {"SQL Lab", "SQLLab", "SQL Editor", "Saved Queries", "Query Search", "SavedQuery", "Query",
                 "TabStateView", "TableSchemaView", "SqlLabRestApi", "SQL Lab (Dev)"}


def ensure_reporting_database(db, uri: str):
    from superset.models.core import Database

    database = db.session.query(Database).filter_by(database_name=REPORTING_DB_NAME).one_or_none()
    if database is None:
        database = Database(database_name=REPORTING_DB_NAME)
        db.session.add(database)
    database.uuid = REPORTING_DB_UUID
    database.set_sqlalchemy_uri(uri)
    database.expose_in_sqllab = False
    database.allow_dml = False
    database.allow_ctas = False
    database.allow_cvas = False
    database.allow_file_upload = False
    db.session.commit()
    return database


def ensure_role(sm, name: str, drop_sql_lab: bool):
    role = sm.find_role(name)
    if role is None:
        sm.copy_role("Gamma", name, merge=True)
        role = sm.find_role(name)
    if drop_sql_lab:
        role.permissions = [
            pvm for pvm in role.permissions
            if pvm.view_menu.name not in SQL_LAB_MENUS and pvm.permission.name != "can_sqllab"
        ]
    return role


def grant_datasource_access(db, sm, role, database):
    from superset.connectors.sqla.models import SqlaTable

    for dataset in db.session.query(SqlaTable).filter_by(database_id=database.id).all():
        pvm = sm.find_permission_view_menu("datasource_access", dataset.perm)
        if pvm is None:
            pvm = sm.add_permission_view_menu("datasource_access", dataset.perm)
        if pvm not in role.permissions:
            role.permissions.append(pvm)
    schema_pvm = sm.find_permission_view_menu("schema_access", f"[{database.database_name}].[surveyreport]")
    if schema_pvm is not None and schema_pvm not in role.permissions:
        role.permissions.append(schema_pvm)


EMBEDDED_DASHBOARDS = {
    # dashboard slug -> fixed embedded uuid; Admin's SUPERSET_DASHBOARD_ID must match.
    "survey-operations": uuid.UUID("2c8d5a4e-3f6b-4c1d-8e9a-1b2c3d4e5f60"),
}


def ensure_embedded(db, allowed_domains):
    """Give each shipped dashboard a stable embedded id (the export ZIP does not carry it)."""
    from superset.models.dashboard import Dashboard
    from superset.models.embedded_dashboard import EmbeddedDashboard

    for slug, embedded_uuid in EMBEDDED_DASHBOARDS.items():
        dashboard = db.session.query(Dashboard).filter_by(slug=slug).one_or_none()
        if dashboard is None:
            print(f"bootstrap: dashboard '{slug}' not imported yet; embedding deferred")
            continue
        embedded = db.session.query(EmbeddedDashboard).filter_by(dashboard_id=dashboard.id).one_or_none()
        if embedded is None:
            embedded = EmbeddedDashboard(dashboard_id=dashboard.id)
            db.session.add(embedded)
        embedded.uuid = embedded_uuid
        embedded.allow_domain_list = ",".join(allowed_domains)   # the model exposes allowed_domains read-only
        print(f"bootstrap: dashboard '{slug}' embeddable as {embedded_uuid} for {allowed_domains}")


def share_with_role(db, sm, role):
    """DASHBOARD_RBAC: every shipped dashboard is visible to the analyst role."""
    from superset.models.dashboard import Dashboard

    for slug in EMBEDDED_DASHBOARDS:
        dashboard = db.session.query(Dashboard).filter_by(slug=slug).one_or_none()
        if dashboard is not None and role not in dashboard.roles:
            dashboard.roles.append(role)


def main():
    app = create_app()
    with app.app_context():
        from superset.extensions import db, security_manager as sm

        database = ensure_reporting_database(db, os.environ["SUPERSET_REPORTING_DB_URI"])
        analyst = ensure_role(sm, "ElicitAnalyst", drop_sql_lab=True)
        guest = ensure_role(sm, "ElicitGuest", drop_sql_lab=True)
        for role in (analyst, guest):
            grant_datasource_access(db, sm, role, database)
        share_with_role(db, sm, analyst)
        allowed = [d.strip().replace("http://", "").replace("https://", "")
                   for d in os.environ.get("SUPERSET_FRAME_ANCESTORS", "http://localhost:8081").split(",") if d.strip()]
        ensure_embedded(db, allowed)
        db.session.commit()
        print(f"bootstrap: reporting database id={database.id} uuid={database.uuid}; "
              f"ElicitAnalyst perms={len(analyst.permissions)} ElicitGuest perms={len(guest.permissions)}")


if __name__ == "__main__":
    main()
