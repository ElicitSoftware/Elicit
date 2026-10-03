# Elicit Internationalization (i18n) Guide

How the Survey and Admin applications localize their own user interface, how a
site chooses which of them it offers, and how to get a new language translated.
Unlike the brand system (`BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`), which a deployment supplies on a
mount, translations ship inside the release: a site chooses among them rather than providing them.

## Scope

- **In scope:** application chrome — buttons, labels, page titles, notifications, validation
  messages, PDF headers and footers, and the organization name supplied by the brand.
- **Out of scope:** message templates and the body of an external report service, which appear
  in the language they were authored in.
- **Survey content is translated by a second, separate mechanism** (Survey V019): question text,
  answer options, step and section names, tooltips, validation messages and report titles live in
  `survey.translations`, are written in Author against a particular survey, and travel to a site
  inside its definition file rather than in the image. The two meet at the runtime: a respondent
  is offered a content language only when the survey publishes it and the site also offers that
  language for the chrome, so no one reads translated questions between English buttons (Survey
  UC-009; Author UC-043).
- Text direction is right-to-left or left-to-right only; vertical writing modes are not
  supported.

## Where translations live

Each module keeps Vaadin's standard layout so the Vaadin Copilot internationalization panel
keeps working alongside hand-written extraction:

```
src/main/resources/vaadin-i18n/
  translations.properties               # English — authored here, edited by Vaadin Copilot
  translations.context.properties       # translator context per key (not read by Vaadin)
i18n/
  translations_es_419.properties        # received from a translator; packaged by the build
  translations_ar.properties
  TRANSLATION_REQUEST.md                # generated handoff document (committed; no UI)
  README.md
```

English is **authored**; the other languages are **received**, which is why they sit apart from
`src/`. A `<resource>` block in each module's `pom.xml` copies `i18n/translations_*.properties`
onto the classpath at `vaadin-i18n/`, beside the English bundle, so a released image carries the
translation it was built and tested with. Two details in that block are load-bearing: `filtering`
must be `false` (a filtered copy is re-encoded, which corrupts the Arabic, and a translated string
may contain `${...}`), and the include is `translations_*.properties` rather than
`translations*.properties`, so `i18n/` can never shadow the authored English bundle.

`vaadin-i18n/` sits at the classpath root, not under `META-INF/resources`, so Quarkus never serves
it. Requesting `/vaadin-i18n/translations_ar.properties` from a running application returns
Vaadin's SPA fallback — `index.html`, `Content-Type: text/html` — not the file.

**There is no translations mount.** Languages are curated by ElicitSoftware and arrive in a
release: a deployment can neither add a language nor patch one. That is what keeps a translation
and the code that renders it at the same version, and it is why "which translation is this site
running?" has one answer — the version tag. The module tests need no umbrella checkout.

Adding a language is therefore a release step: hand a module's `TRANSLATION_REQUEST.md` to a
translator, put the returned file in that module's `i18n/`, add the tag to
`i18n.bundled.locales`, and do the same in both applications — `checkLanguages.sh` fails the
build if they disagree. Managing languages from the Admin console (`elicit_admin`) is specified
but not built, and was specified against a writable mount that no longer exists: it has to be
re-specified as a screen that edits the offered-language list, which needs a mutable store rather
than a startup-time property (Admin `C-015`).

Keys are `<view>.<element>[.<qualifier>]` (`mainView.btnLogin`, `searchView.grid.firstName`)
with shared keys under `common.*`. Values are Java `MessageFormat` patterns only when
parameters are passed, so an apostrophe in a parameter-less sentence needs no doubling.

## Resolution order (`ElicitI18NProvider`)

For a key and locale, each module's `ElicitI18NProvider` (a CDI bean qualified
`@VaadinServiceEnabled`, which is how the Vaadin Quarkus extension discovers it) reads the
classpath bundle `vaadin-i18n/translations[_tag].properties` and nothing else, then falls back
from the exact locale to the language-only locale, then to English. A key missing everywhere
renders as `!key!` and is logged once, never blank. In the `%test` profile the pseudo-locale
`zxx` renders every key as `⟦key⟧`.

Which locales are **offered** is `i18n.bundled.locales` alone. That property does two jobs, and
the first is why it has to exist at all: classpath resources cannot be enumerated, so nothing can
discover which `translations_*.properties` the jar holds — the set has to be declared. The second
is the site's control: narrow it and the extra languages stay in the image unreachable. The
switcher hides them, `LocaleSelection.resolve` refuses a `?lang=` naming one, and survey content
is not served in one either, because content follows the chrome (`Survey UC-009 BR-009`).

`checkLanguages.sh` in the umbrella root is what keeps the declaration honest: it fails the build
when a module ships a bundle it does not declare, when a tag is declared with no bundle, or when
the modules disagree. All three are otherwise silent.

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
Kurdish (Sorani) are right-to-left by default and every language is unscaled by default; an
the classpath `META-INF/i18n/i18n-config.json` each image ships declares the rest:

```json
{ "locales": [ { "tag": "ar", "direction": "rtl", "fontScale": 1.15 } ] }
```

Both properties are optional per entry and resolved independently, exact tag before language, so an
entry that carries only `direction` behaves as it did before font scale existed. A `fontScale` that
is not a number or falls outside `0.75`–`2.0` is logged and dropped, leaving that language at `1.0`;
it is never clamped into range, because a scale nobody meant is worse than none.

A site overrides either value for one tag with a config property, `i18n.direction.<tag>` or
`i18n.font-scale.<tag>`, which wins over the shipped file. Properties rather than a file, because
there is no mounted directory to put a file in — without them a site could not adjust typography at
all short of a new build. They are read for the tag being asked about rather than enumerated, so an
operator can name any tag without the lookup having to discover which keys exist, which is the part
of SmallRye's property enumeration that does not survive environment variables.

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
so a scale multiplies a reader's enlarged text rather than replacing it. The shipped
Each image's `META-INF/i18n/i18n-config.json` scales `ar` by 1.15: Arabic's apparent x-height is
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

Nothing to mount and nothing to lay out: the languages are in the images. `docker-compose.yml`
carries no i18n wiring at all, and a stock stack offers English, Spanish and Arabic.

A site that wants fewer sets `i18n.bundled.locales` on the service, in Docker or through any
Quarkus config source outside it (see `DeploymentScript.md`). Sites that differ only by that one
line — `en`, `en,es-419`, `en,ar` — are otherwise identical deployments.

Translation files are never served over HTTP: `vaadin-i18n/` is at the classpath root, not under
`META-INF/resources`.

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
same keys, same order. Hand it to a translator or an AI agent, drop the returned file into the
module's `i18n/`, add the tag to `i18n.bundled.locales` in both applications, and run each
module's `TranslationBundleConsistencyTest` and the umbrella's `checkLanguages.sh`.

Survey **content** is the opposite case and does have an on-screen flow: Author's Translations page
writes a JSON handoff per survey and target language (`ELICIT_CONTENT_TRANSLATION_V1`), and reads
the filled file back with per-item validation. Keep the two apart — a chrome translation is a
properties file packaged in the image, a content translation is JSON that travels inside the
definition file.

## Tests that guard the design

| Test (per module) | Guarantees |
|---|---|
| `DisplayedStringsCoverageTest` | No prose-like string literal reaches a UI text API in the view sources (100 % coverage gate, NFR) |
| `DisplayedStringsSweepTest` | With the `zxx` pseudo-locale, every rendered route shows only `⟦key⟧` markers outside `data-i18n-content` subtrees |
| `TranslationBundleConsistencyTest` | Locale files carry exactly the English keys; every referenced key exists; placeholders match; no untranslated values; every key has context |
| `ElicitI18NProviderTest` | The packaged bundles resolve with no filesystem at all (this is what catches a broken pom `<resource>` mapping); narrowing `i18n.bundled.locales` hides a language whose bundle is still in the image; language fallback; `!key!`; cache reset |
| `LocaleConfigTest`, `LocaleLayoutTest` | RTL/LTR defaults, the shipped manifest, the `i18n.direction.<tag>` / `i18n.font-scale.<tag>` property overrides (including a refused scale, a non-numeric one and a throwing lookup), and `LocaleConfig.parse` against a malformed manifest; `?lang=`; switcher contents |
| `TranslationRequestGeneratorTest` | The committed handoff document matches the shipped bundle |
| `BrandUtilTest` | Localized brand names resolve tag → language → base |

Survey content rendered by the views (question components, review and report cards, authored
survey text on the About page) is marked with `data-i18n-content` so the sweep skips it.
| `checkLanguages.sh` (umbrella) | Survey and Admin ship the same languages and each declares the bundles it ships; run by `buildDockerImages.sh` before anything is built |
