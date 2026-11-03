# Elicit Internationalization (i18n) Guide

How the Survey and Admin applications localize their own user interface, how a
site supplies and narrows the languages they offer, and how to get a new language translated.
Like the brand system (`BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`), translations are supplied by the
deployment on a read-only mount: the images carry English only.

## Scope

- **In scope:** application chrome — buttons, labels, page titles, notifications, validation
  messages, PDF headers and footers, and the organization name supplied by the brand.
- **Out of scope:** message templates and the body of an external report service, which appear
  in the language they were authored in.
- **Survey content is translated by a second, separate mechanism** (Survey V019): question text,
  answer options, step and section names, tooltips, validation messages and report titles live in
  `survey.translations`, are written in Author against a particular survey, and travel to a site
  inside its definition file rather than in the translations mount. The two meet at the runtime: a respondent
  is offered a content language only when the survey publishes it and the site also offers that
  language for the chrome, so no one reads translated questions between English buttons (Survey
  UC-009; Author UC-043).
- Text direction is right-to-left or left-to-right only; vertical writing modes are not
  supported.

## Where translations live

English is in the image. Each module keeps Vaadin's standard layout so the Vaadin Copilot
internationalization panel keeps working alongside hand-written extraction:

```
src/main/resources/vaadin-i18n/
  translations.properties               # English — authored here, edited by Vaadin Copilot
  translations.context.properties       # translator context per key (not read by Vaadin)
i18n/
  TRANSLATION_REQUEST.md                # generated handoff document (committed; no UI)
  README.md
```

Every other language is supplied by the deployment, in a directory mounted read-only. The
umbrella's `elicit-i18n/` is the curated set ElicitSoftware maintains beside each release, and it is
the directory `docker-compose.yml` mounts:

```
elicit-i18n/                            # mounted at /opt/i18n; i18n.file.system.path=/opt/i18n
  i18n-config.json                      # direction and font scale per language (ar: rtl, 1.15)
  survey/
    translations_es_419.properties
    translations_ar.properties
  admin/
    translations_es_419.properties
    translations_ar.properties
  author/                               # read by the authoring tool, mounted by its own compose file
    translations_es_419.properties
    translations_ar.properties
```

Each application reads only its own sub-directory, `<i18n.file.system.path>/<i18n.app.name>/`
(`/i18n/survey/` and `/i18n/admin/` by default), so one host directory serves them all. The file
name spells the tag with underscores (`es_419`); every property and runtime comparison uses
hyphens (`es-419`).

A mounted file overlays English **key by key**: it may carry only the keys it changes, and a key
it lacks is shown in English. So a site can:

- add a language by mounting a file;
- correct one wording by mounting a copy of the curated file with that text changed;
- withdraw a language by removing its file.

All three take effect on restart. The curated set is the wording a release was tested with; a
site that mounts something else owns what it says. With nothing mounted an application offers
English only and shows no language selector, which is what the GKE campsites do until they mount
translations.

Each module carries a test that fails if a translated bundle ever reaches the classpath, so no
image carries a language but English (umbrella C-015).

`vaadin-i18n/` sits at the classpath root, not under `META-INF/resources`, so Quarkus never serves
it, and the mount is read server-side only.

Adding a language to the curated set is a step for ElicitSoftware:

1. Hand each module's `TRANSLATION_REQUEST.md` to a translator.
2. Put the returned files in `elicit-i18n/survey/`, `elicit-i18n/admin/` and `elicit-i18n/author/`.
3. Run `checkLanguages.sh`. It fails the build if the three sub-directories hold different languages.

Managing languages from the Admin console (`elicit_admin`) is specified but not built (Admin UC-027).

Keys are `<view>.<element>[.<qualifier>]` (`mainView.btnLogin`, `searchView.grid.firstName`)
with shared keys under `common.*`. Values are Java `MessageFormat` patterns only when
parameters are passed, so an apostrophe in a parameter-less sentence needs no doubling.

## Resolution order (`ElicitI18NProvider`)

For a key and locale, each module's `ElicitI18NProvider` (a CDI bean qualified
`@VaadinServiceEnabled`, which is how the Vaadin Quarkus extension discovers it) reads the
classpath bundle `vaadin-i18n/translations[_tag].properties`, overlaid key by key by the mounted
file of the same name. It then falls back from the exact locale to the language-only locale, and
then to English. A key missing everywhere renders as `!key!` and is logged once, never blank. In the
`%test` profile the pseudo-locale `zxx` renders every key as `⟦key⟧`.

The **offered** locales are English plus every `translations_<tag>.properties` in the mount
sub-directory. When `i18n.offered.locales` is set, they are narrowed to the tags it lists:

- English is always offered.
- A listed tag with no mounted file is not offered, and the provider logs a warning naming the
  directory.
- A directory that does not exist is logged once, and the application offers English only.

A language that is not offered is hidden by the switcher, refused by `LocaleSelection.resolve`
when a `?lang=` names it, and not served for survey content either, because content follows the
chrome (`Survey UC-009 BR-009`).

`checkLanguages.sh` in the umbrella root fails the build when the curated set's `survey/`,
`admin/` and `author/` hold different languages.

## Choosing the language (`LocaleInitializer`, `LocaleSelection`, `LanguageSwitcher`)

Precedence: a `?lang=<tag>` query parameter on any route (Survey invitation links carry one),
then the language remembered in the browser session, then the browser's `Accept-Language`
negotiated by Vaadin against the provided locales, then English. An unknown tag is ignored. The
choice lives in the Vaadin session attribute `elicit.locale` only; nothing is stored with the
respondent or user. Every page shows a `LanguageSwitcher` (`Select`, id `language-switcher`)
listing the provided locales in their own language; picking one remembers it and reloads the
page so every view is rebuilt in the new language.

## Direction and font scale (`LocaleConfig`, `LocaleLayout`)

`LocaleLayout.apply(ui, locale)` sets Vaadin's `Direction`, the `dir`, `lang` and
`data-font-scale` attributes on the UI element (assertable in browserless tests) and
`document.documentElement.lang`/`dir` plus the `--elicit-font-scale` custom property on the
document element. Arabic, Hebrew, Persian, Urdu, Pashto, Sindhi, Uyghur, Yiddish, Dhivehi and
Kurdish (Sorani) are right-to-left by default and every language is unscaled by default. Three
layers declare otherwise, each winning over the one before:

1. `META-INF/i18n/i18n-config.json` on the classpath, which each image ships.
2. `i18n-config.json` at the root of the translations mount, shipped beside the curated
   translations and shared by both applications.
3. The `i18n.direction.<tag>` and `i18n.font-scale.<tag>` properties.

Both files have the same format:

```json
{ "locales": [ { "tag": "ar", "direction": "rtl", "fontScale": 1.15 } ] }
```

Both properties are optional per entry and resolved independently, exact tag before language. An
entry silent on one property leaves the layer below standing, so an entry that carries only
`direction` behaves as it did before font scale existed. A `fontScale` that
is not a number or falls outside `0.75`–`2.0` is logged and dropped, leaving that language at `1.0`;
it is never clamped into range, because a scale nobody meant is worse than none.

A site overrides either value for one tag and one application with a config property,
`i18n.direction.<tag>` or `i18n.font-scale.<tag>`, which wins over both files. They are read for the
tag being asked about rather than enumerated. So an operator can name any tag without the lookup
having to discover which keys exist: SmallRye's property enumeration does not survive environment
variables. A malformed mounted manifest is logged and leaves the image's own standing.

The scale is per language but page-wide: it multiplies the root font size, so a page in that
language grows entirely — spacing, controls and any Latin text on it — while other languages stay
at `1.0`.

Application CSS uses logical properties (`margin-inline-start`, `text-align: start`) so the
mirrored layout needs no per-language stylesheet. The scale has one hook, in each app's
`styles.css`:

```css
html { font-size: calc(100% * var(--elicit-font-scale, 1)); }
```

It is the *root* font size deliberately. Lumo's and Aura's font sizes, spacing and control sizes,
the `--brand-font-size-*` tokens and the applications' own rules are all expressed in `rem`, so one
property scales the page in proportion and nothing clips; scaling only the font-size tokens would
grow text inside controls that had not grown with it. `100%` is the reader's own browser default,
so a scale multiplies a reader's enlarged text rather than replacing it. Both the image's
manifest and the curated mount's scale `ar` by 1.15: Arabic's apparent x-height is
smaller than Latin's in the system font stack, so it reads small at a size that is comfortable in
English.

A per-locale `fontFamily` is deliberately *not* part of this. A family is only useful with a face
the reader's device actually has, and there is nothing to ship one from; adding the property
without that would be a setting that silently does nothing.

## Brand text

The organization name in the header and the brand description in the `brand-info` meta tag
come from the mounted brand. They are translated in the brand itself, through an optional
`localized` block keyed by language tag in `brand-config.json` (`name`, `organization`) and
`brand-info.json` (`description`), resolved exact tag → language → base. The base `name`
still derives the technical brand key. See `elicit-brand/README.md`.

## Docker and non-Docker deployment

`docker-compose.yml` mounts `./elicit-i18n` read-only at `/opt/i18n` on Survey and Admin and sets
`i18n.file.system.path=/opt/i18n`, so a stock stack offers English, Spanish and Arabic. Leave the
mount out and the stack offers English only.

Outside Docker the directory is any one the service account can read, named by
`i18n.file.system.path` through any Quarkus config source (see `DeploymentScript.md`). The dev
and test profiles point it at `../elicit-i18n`, the curated set in an umbrella checkout. Module CI
fetches that set from the umbrella branch with the same name as the one under test, or the
umbrella's default branch.

`i18n.offered.locales` narrows what one application offers without touching the mount. For
example, sites can share one mounted directory and differ only by `en`, `en,es-419` or `en,ar`.

## Getting a language translated

These are the application's own static strings, so the handoff has **no on-screen flow**: nothing
to download from a running Survey or Admin, no upload page, no per-language screen. Each
module keeps one committed Markdown document, `i18n/TRANSLATION_REQUEST.md`, generated from its
English bundle and the context sidecar (`TranslationRequestGeneratorTest`; on drift copy
`target/i18n/TRANSLATION_REQUEST.md` into `i18n/`). One document per module serves every target
language — the person requesting a translation states the target in it before handing it off.

The document carries the application's purpose and audience, a glossary of terms that must stay
consistent or untranslated (**access code**, never "token"), the placeholder, `MessageFormat`
apostrophe, HTML and maximum-length rules, every key with its English text, location, maximum
length and flags, and the return instructions: exactly one UTF-8 `translations_<tag>.properties`,
same keys, same order. Hand it to a translator or an AI agent. Put the returned file into the
umbrella's `elicit-i18n/<app>/` for the curated set, or into the site's own mount. Run each
module's `TranslationBundleConsistencyTest`, which reads `../elicit-i18n`, and the umbrella's
`checkLanguages.sh`.

Survey **content** is the opposite case and does have an on-screen flow: Author's Translations page
writes a JSON handoff per survey and target language (`ELICIT_CONTENT_TRANSLATION_V1`), and reads
the filled file back with per-item validation. Keep the two apart — a chrome translation is a
properties file in the translations mount, a content translation is JSON that travels inside the
definition file.

## Tests that guard the design

| Test (per module) | Guarantees |
|---|---|
| `DisplayedStringsCoverageTest` | No prose-like string literal reaches a UI text API in the view sources (100 % coverage gate, NFR) |
| `DisplayedStringsSweepTest` | With the `zxx` pseudo-locale, every rendered route shows only `⟦key⟧` markers outside `data-i18n-content` subtrees |
| `TranslationBundleConsistencyTest` | Locale files carry exactly the English keys; every referenced key exists; placeholders match; no untranslated values; every key has context |
| `ElicitI18NProviderTest` | The image carries English only. The curated mount offers every language in it. Nothing mounted means English only. A partial file overlays English key by key. Each app reads only its own sub-directory, and `i18n.app.name` cannot climb out of the mount. `i18n.offered.locales` narrows the set but cannot offer an unmounted language. Language fallback; `!key!` |
| `LocaleConfigTest`, `LocaleLayoutTest` | RTL/LTR defaults, the shipped manifest, the mounted manifest over it, and the `i18n.direction.<tag>` / `i18n.font-scale.<tag>` property overrides over both. Includes a refused scale, a non-numeric one, a throwing lookup, and a malformed mounted or parsed manifest. `?lang=`; switcher contents |
| `TranslationRequestGeneratorTest` | The committed handoff document matches the English bundle |
| `BrandUtilTest` | Localized brand names resolve tag → language → base |

Survey content rendered by the views (question components, review and report cards, authored
survey text on the About page) is marked with `data-i18n-content` so the sweep skips it.
| `checkLanguages.sh` (umbrella) | The curated set holds the same languages for Survey, Admin and Author; run by `buildDockerImages.sh` before anything is built |
