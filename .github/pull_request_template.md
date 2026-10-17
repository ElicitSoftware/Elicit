<!--
  One pull request per issue, from a branch named <type>/<issue>-<slug> off V3.
  Title: what the change does, in the imperative, without the issue number.
  Tick every line that applies; strike one through (~~like this~~) with a reason when it does not.
  An unticked, unstruck line is a question the reviewer will ask.
-->

## Issue

Closes #

## What changed and why

<!-- Two to five sentences. Behavior first, implementation second. -->

## AIUP

<!-- docs/ is the source of truth; the docs commit comes before the code commit. -->

- [ ] `docs/requirements.md` updated (IDs: ) or no requirement changed
- [ ] `docs/use_cases.puml` and the use case spec(s) updated (IDs: ) or no behavior changed
- [ ] `docs/entity_model.md` updated or no entity changed
- [ ] Tests cite the UC and BR IDs they cover
- [ ] `docs/manual/` updated when installation, configuration, branding or verification changed, and `docs/manual/check-properties.sh` still passes

## Checks

- [ ] `docker compose config` and `status.sh` still work when `docker-compose.yml` or a script changed
- [ ] `checkLanguages.sh` still passes when anything about languages changed
- [ ] A change to `samples/census-household-survey.elicit` was made in the generator, appended at the end, and the twin in Author's multilingual e2e suite refreshed
- [ ] Cross-module change: the tracking issue in `ElicitSoftware/Elicit` and the sibling PRs are linked above
- [ ] No AI co-author trailer in any commit message
