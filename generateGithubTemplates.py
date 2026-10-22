#!/usr/bin/env python3
"""Generates .github/pull_request_template.md and .github/ISSUE_TEMPLATE/* for every Elicit repo.

The six repositories share one shape (issue, change, AIUP block, checklist; bug and feature
forms that ask for the affected FR/UC/BR IDs) and differ in their own checks, so the files are
generated from this one table rather than edited six times. Run it from the umbrella, where
the module clones sit beside it, and commit the result in each repository:

    ./generateGithubTemplates.py            # writes <umbrella>/<Repo>/.github/... for all six
    ./generateGithubTemplates.py <root>     # the same layout under another root (worktrees)

The umbrella's own files are written to ./.github. Pedigree has no AIUP artifacts, so its
template and forms leave that block out.
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
ALL = ["Elicit", "Survey", "Admin", "FHHS", "Pedigree", "Author"]
NAMES = {"Elicit": "Elicit (umbrella)", "Survey": "Survey", "Admin": "Admin", "FHHS": "FHHS",
         "Pedigree": "Pedigree", "Author": "Author"}

REPOS = {
    "Elicit": dict(
        base="V3", aiup=True,
        aiup_extra=["`docs/manual/` updated when installation, configuration, branding or verification "
                    "changed, and `docs/manual/check-properties.sh` still passes"],
        checks=["`docker compose config` and `status.sh` still work when `docker-compose.yml` or a script changed",
                "`checkLanguages.sh` still passes when anything about languages changed",
                "A change to `samples/census-household-survey.elicit` was made in the generator, appended at "
                "the end, and the twin in Author's multilingual e2e suite refreshed"],
    ),
    "Survey": dict(
        base="V3", aiup=True, aiup_extra=[],
        checks=["Flyway migration added to **both** `db/migration` and `db/migration-v3`, and the brownfield "
                "track verified with `resetDatabase.sh V2` from the umbrella",
                "A reporting (star schema) change keeps `report_<slug>` building from the apply, and FHHS still reads it",
                I18N := "New user-facing text has its key in `translations.properties`, "
                "`i18n/translations_es_419.properties` and `i18n/translations_ar.properties`, a context note, "
                "and `i18n/TRANSLATION_REQUEST.md` regenerated",
                "Respondent credential is called the **access code**, never a token"],
    ),
    "Admin": dict(
        base="V3", aiup=True,
        aiup_extra=["The administrator's manual under `docs/manual/` updated when a screen changed"],
        checks=["Flyway migration added under `db/migration` (Admin's tables only; the `survey` schema is Survey's)",
                I18N,
                "Respondent credential is called the **access code**, never a token"],
    ),
    "FHHS": dict(
        base="V3", aiup=True, aiup_extra=[],
        checks=["Flyway migration added to **both** `db/migration` and `db/migration-v3`",
                "Brownfield verified with `resetDatabase.sh V2` from the umbrella (every FHHS change, not only "
                "migrations)",
                "A change to `family-history-survey.elicit` verified greenfield (a V2 database cannot take an "
                "update of the definition)",
                "The SFTP XML template still uses `{AccessCode}`; the respondent credential is never called a token"],
    ),
    "Pedigree": dict(
        base="main", aiup=False, aiup_extra=[],
        checks=["`README.md` updated when the API, the input format or the build changed",
                "`ped1.txt` still renders (the CI smoke test posts it to `/svg`)",
                "A change to the `/svg` contract was checked against FHHS, its only caller",
                "A `Dockerfile` change still builds for `linux/amd64` and `linux/arm64` (the release workflow "
                "pushes both)"],
    ),
    "Author": dict(
        base="main", aiup=True,
        aiup_extra=["The author's manual under `docs/manual/` updated when a screen changed"],
        checks=["A change to the `.elicit` format is mirrored in Survey's importer and in the umbrella's "
                "golden sample (`samples/census-household-survey.elicit`, via its generator), and the sample "
                "test still passes",
                "`scripts/sync-survey-schema.sh` run when Survey's migrations changed, so the schema mirror "
                "is fresh",
                "A new rule, control or token form was added to the golden census survey",
                I18N],
    ),
}


def pr_template(repo):
    r = REPOS[repo]
    base = r["base"]
    lines = [
        "<!--",
        "  One pull request per issue, from a branch named <type>/<issue>-<slug> off %s." % base,
        "  Title: what the change does, in the imperative, without the issue number.",
        "  Tick every line that applies; strike one through (~~like this~~) with a reason when it does not.",
        "  An unticked, unstruck line is a question the reviewer will ask.",
        "-->",
        "",
        "## Issue",
        "",
        "Closes #",
        "",
        "## What changed and why",
        "",
        "<!-- Two to five sentences. Behavior first, implementation second. -->",
        "",
    ]
    if r["aiup"]:
        lines += [
            "## AIUP",
            "",
            "<!-- docs/ is the source of truth; the docs commit comes before the code commit. -->",
            "",
            "- [ ] `docs/requirements.md` updated (IDs: ) or no requirement changed",
            "- [ ] `docs/use_cases.puml` and the use case spec(s) updated (IDs: ) or no behavior changed",
            "- [ ] `docs/entity_model.md` updated or no entity changed",
            "- [ ] Tests cite the UC and BR IDs they cover",
        ]
        lines += ["- [ ] " + x for x in r["aiup_extra"]]
        lines += [""]
    lines += ["## Checks", ""]
    lines += ["- [ ] " + x for x in r["checks"]]
    lines += [
        "- [ ] Cross-module change: the tracking issue in `ElicitSoftware/Elicit` and the sibling PRs are linked above",
        "- [ ] No AI co-author trailer in any commit message",
        "",
    ]
    return "\n".join(lines)


def y(s):
    """Quote a YAML scalar."""
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def others(repo):
    return [NAMES[x] for x in ALL if x != repo]


def bug_form(repo):
    r = REPOS[repo]
    body = []
    body.append(f"""  - type: textarea
    id: what-happened
    attributes:
      label: What happened
      description: What you did and what {NAMES[repo]} did. Paste exact messages.
    validations:
      required: true
  - type: textarea
    id: expected
    attributes:
      label: What should have happened
      description: {"Cite the use case or business rule that says so (UC-NNN, BR-NNN) when you know it." if r["aiup"] else "Describe the behavior you expected."}
    validations:
      required: true
  - type: textarea
    id: steps
    attributes:
      label: Steps to reproduce
      placeholder: |
        1.
        2.
        3.
    validations:
      required: true
  - type: input
    id: version
    attributes:
      label: Version or commit
      description: Image tag, release, or `git rev-parse --short HEAD`.
    validations:
      required: true""")
    if repo in ("Survey", "Admin", "FHHS", "Elicit"):
        body.append("""  - type: dropdown
    id: database
    attributes:
      label: Database state
      options:
        - Greenfield (resetDatabase.sh V3)
        - Brownfield (resetDatabase.sh V2, upgraded from a V2 database)
        - An existing development database
        - Not applicable
    validations:
      required: true""")
    if r["aiup"]:
        body.append("""  - type: input
    id: aiup
    attributes:
      label: Affected AIUP artifacts
      description: The FR, UC or BR IDs under docs/ that describe the behavior, or "unknown".
      placeholder: UC-004, BR-012
    validations:
      required: true""")
    body.append("""  - type: textarea
    id: logs
    attributes:
      label: Logs, screenshots, SQL
      description: Anything that helps; this field renders as code.
      render: shell""")
    return f"""name: Bug
description: Something {NAMES[repo]} does that it should not, or fails to do.
labels: ["bug"]
body:
{chr(10).join(body)}
"""


def feature_form(repo):
    r = REPOS[repo]
    body = []
    body.append(f"""  - type: textarea
    id: problem
    attributes:
      label: Problem
      description: Who is blocked or ill-served today, and how. Not the solution yet.
    validations:
      required: true
  - type: textarea
    id: proposal
    attributes:
      label: Proposed behavior
      description: What {NAMES[repo]} should do, from the user's point of view.
    validations:
      required: true""")
    if r["aiup"]:
        body.append("""  - type: input
    id: aiup
    attributes:
      label: Affected AIUP artifacts
      description: The FR and UC IDs under docs/ this changes, or "new" for a new requirement or use case. The docs change lands before the code.
      placeholder: FR-012, UC-004 or new
    validations:
      required: true""")
    opts = "\n".join(f"        - label: {o}" for o in others(repo))
    body.append(f"""  - type: checkboxes
    id: other-repos
    attributes:
      label: Other repositories affected
      description: Tick any other module this touches. More than one repository means a tracking issue in ElicitSoftware/Elicit with one PR per repository.
      options:
{opts}""")
    body.append("""  - type: textarea
    id: acceptance
    attributes:
      label: Acceptance criteria
      description: How a reviewer will know it is done. These become the use case's postconditions and the tests.
      placeholder: |
        -
        -
  - type: textarea
    id: notes
    attributes:
      label: Notes
      description: Constraints, alternatives considered, links.""")
    return f"""name: Feature or change
description: New behavior, or a change to existing behavior, in {NAMES[repo]}.
labels: ["enhancement"]
body:
{chr(10).join(body)}
"""


def config(repo):
    if repo == "Elicit":
        link = """  - name: A bug or change in one module
    url: https://github.com/orgs/ElicitSoftware/repositories
    about: Survey, Admin, FHHS, Pedigree and Author each take their own issues. Open the issue there; this repository is for the platform as a whole and for changes that span modules."""
    else:
        link = """  - name: A change that spans more than one module
    url: https://github.com/ElicitSoftware/Elicit/issues/new/choose
    about: Open the tracking issue in the umbrella repository, then one issue here for this module's part."""
    return f"""blank_issues_enabled: false
contact_links:
{link}
"""


for repo in ALL:
    gh = os.path.join(ROOT, "" if repo == "Elicit" and len(sys.argv) == 1 else repo, ".github")
    it = os.path.join(gh, "ISSUE_TEMPLATE")
    os.makedirs(it, exist_ok=True)
    with open(os.path.join(gh, "pull_request_template.md"), "w") as f:
        f.write(pr_template(repo))
    with open(os.path.join(it, "bug.yml"), "w") as f:
        f.write(bug_form(repo))
    with open(os.path.join(it, "feature.yml"), "w") as f:
        f.write(feature_form(repo))
    with open(os.path.join(it, "config.yml"), "w") as f:
        f.write(config(repo))
    print("wrote", repo)
