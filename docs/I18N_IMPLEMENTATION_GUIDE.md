# Elicit Internationalization (i18n) Guide

How the Survey, Admin and Author applications localize their own user interface, how a
deployment adds languages without rebuilding, and how to get a new language translated.
The design mirrors the brand system (`BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`): a per-file,
per-key fallback from an external mount to a local directory to the embedded default.

## Scope

- **In scope:** application chrome — buttons, labels, page titles, notifications, validation
  messages, PDF headers and footers, and the organization name supplied by the brand.
- **Out of scope:** message templates and the body of an external report service, which appear
  in the language they were authored in.
- **Survey content is translated by a second, separate mechanism** (Survey V019): question text,
  answer options, step and section names, tooltips, validation messages and report titles live in
  `survey.translations`, are written in Author against a particular survey, and travel to a site
  inside its definition file rather than on this mount. The two meet at the runtime: a respondent
  is offered a content language only when the survey publishes it and that language is also
  mounted here for the chrome, so no one reads translated questions between English buttons. See
  `Survey/docs/research/i18n_survey.md`.
- Text direction is right-to-left or left-to-right only; vertical writing modes are not
  supported.

## Where translations live

Each module keeps Vaadin's standard layout so the Vaadin Copilot internationalization panel
keeps working alongside hand-written extraction:

```
src/main/resources/vaadin-i18n/
  translations.properties               # English — the only language the application ships
  translations.context.properties       # translator context per key (not read by Vaadin)
i18n/TRANSLATION_REQUEST.md             # generated handoff document (committed; no UI)
```

Every other language lives outside the application, in the deployment translations directory
(`elicit-i18n/<app>/` in this repository, see below): the release is English only, and a
deployment adds languages by mounting files. The language selector in the header appears
only when more than one language is available, so an English-only deployment never shows it.
In the test and dev profiles each module reads `../elicit-i18n`, so the module tests that
exercise Spanish and Arabic need the umbrella checkout (the module CI fetches that directory).

Adding a language today is a server-side step (drop the file into the mount and restart, see
`DeploymentScript.md`). Managing languages from the Admin console (for the console and the
survey application, `elicit_admin` role) and from the Author tool (`elicit_author` role) is
specified but not built: Admin `UC-021` / `FR-026` and Author `UC-035` / `FR-048`, which need
the mount to be writable (Admin `C-014`, Author `C-022`).

Keys are `<view>.<element>[.<qualifier>]` (`mainView.btnLogin`, `searchView.grid.firstName`)
with shared keys under `common.*`. Values are Java `MessageFormat` patterns only when
parameters are passed, so an apostrophe in a parameter-less sentence needs no doubling.

## Resolution order (`ElicitI18NProvider`)

For a key and locale, each module's `ElicitI18NProvider` (a CDI bean qualified
`@VaadinServiceEnabled`, which is how the Vaadin Quarkus extension discovers it) merges three
tiers per **key**, later tiers winning:

1. classpath `vaadin-i18n/translations[_tag].properties`
2. local directory `i18n/<app>/translations[_tag].properties` (`i18n.local.path`, default `i18n`)
3. external mount `<i18n.file.system.path>/<app>/translations[_tag].properties`
   (default `/i18n`, `/opt/i18n` in Docker)

then falls back from the exact locale to the language-only locale, then to English. A key
missing everywhere renders as `!key!` and is logged once, never blank. The provided locales are
the union of `i18n.bundled.locales` (`en`) and every `translations_*.properties` found in tiers 2–3,
so a language that exists only on the mount is offered too. `<app>` is `i18n.app.name`
(`survey`, `admin`, `author`; `author-survey` uses `survey`) and is rejected if it contains
path separators. In the `%test` profile the pseudo-locale `zxx` renders every key as `⟦key⟧`.

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
optional `i18n-config.json` at the mount root (then local `i18n/`, then classpath
`META-INF/i18n/`) overrides or extends that:

```json
{ "locales": [ { "tag": "ar", "direction": "rtl", "fontScale": 1.15 } ] }
```

Both properties are optional per entry and resolved independently, exact tag before language, so
a mount that carries only `direction` behaves as it did before font scale existed. A `fontScale`
that is not a number or falls outside `0.75`–`2.0` is logged and dropped, leaving that language at
`1.0`; it is never clamped into range, because a scale a deployer did not mean is worse than none.

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
`elicit-i18n/i18n-config.json` scales `ar` by 1.15: Arabic's apparent x-height is smaller than
Latin's in the system font stack, so it reads small at a size that is comfortable in English.

A per-locale `fontFamily` is deliberately *not* part of this. A family is only useful with a face
the reader's device actually has, or one the deployment can ship, and there is no font mount to
ship it from; adding the property without that would be a setting that silently does nothing.

## Brand text

The organization name in the header and the brand description in the `brand-info` meta tag
come from the mounted brand. They are translated in the brand itself, through an optional
`localized` block keyed by language tag in `brand-config.json` (`name`, `organization`) and
`brand-info.json` (`description`), resolved exact tag → language → base. The base `name`
still derives the technical brand key. See `elicit-brand/README.md`.

## Docker and non-Docker deployment

`docker-compose.yml` mounts `./elicit-i18n/:/opt/i18n:ro` into `survey`, `admin`, `author`
and `author-survey` and sets `i18n.file.system.path: /opt/i18n`. `elicit-i18n/` holds one
sub-directory per app with copies of the shipped bundles, so it is safe to mount as is and edit
in place; `test-partial-i18n/` shows a single-key override and a mount-only language. Outside
Docker, set `i18n.file.system.path` through any Quarkus config source and lay the directory out
the same way (see `DeploymentScript.md`). Translation files are never served over HTTP.

## Getting a language translated

These are the application's own static strings, so the handoff has **no on-screen flow**: nothing
to download from a running Survey, Admin or Author, no upload page, no per-language screen. Each
module keeps one committed Markdown document, `i18n/TRANSLATION_REQUEST.md`, generated from its
English bundle and the context sidecar (`TranslationRequestGeneratorTest`; on drift copy
`target/i18n/TRANSLATION_REQUEST.md` into `i18n/`). One document per module serves every target
language — the person requesting a translation states the target in it before handing it off.

The document carries the application's purpose and audience, a glossary of terms that must stay
consistent or untranslated (**access code**, never "token"), the placeholder, `MessageFormat`
apostrophe, HTML and maximum-length rules, every key with its English text, location, maximum
length and flags, and the return instructions: exactly one UTF-8 `translations_<tag>.properties`,
same keys, same order. Hand it to a translator or an AI agent, drop the returned file into the
module (to ship it) or into the mount (to deploy it), and run the module's
`TranslationBundleConsistencyTest`.

Survey **content** is the opposite case and does have an on-screen flow: Author's Translations page
writes a JSON handoff per survey and target language (`ELICIT_CONTENT_TRANSLATION_V1`), and reads
the filled file back with per-item validation. Keep the two apart — a chrome translation is a
properties file for the mount, a content translation is JSON that travels inside the definition
file.

## Tests that guard the design

| Test (per module) | Guarantees |
|---|---|
| `DisplayedStringsCoverageTest` | No prose-like string literal reaches a UI text API in the view sources (100 % coverage gate, NFR) |
| `DisplayedStringsSweepTest` | With the `zxx` pseudo-locale, every rendered route shows only `⟦key⟧` markers outside `data-i18n-content` subtrees |
| `TranslationBundleConsistencyTest` | Locale files carry exactly the English keys; every referenced key exists; placeholders match; no untranslated values; every key has context |
| `ElicitI18NProviderMountTest` | Mount-only locales, per-key override, language fallback, `!key!`, traversal guard, cache reset |
| `LocaleConfigTest`, `LocaleLayoutTest` | RTL/LTR defaults, font scale and their manifest overrides (including a refused scale); `?lang=`; switcher contents |
| `TranslationRequestGeneratorTest` | The committed handoff document matches the shipped bundle |
| `BrandUtilTest` | Localized brand names resolve tag → language → base |

Survey content rendered by the views (question components, review and report cards, authored
survey text on the About page) is marked with `data-i18n-content` so the sweep skips it.
