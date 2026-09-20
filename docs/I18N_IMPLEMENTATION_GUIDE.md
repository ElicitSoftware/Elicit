# Elicit Internationalization (i18n) Guide

How the Survey, Admin and Author applications localize their own user interface, how a
deployment adds languages without rebuilding, and how to get a new language translated.
The design mirrors the brand system (`BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`): a per-file,
per-key fallback from an external mount to a local directory to the embedded default.

## Scope

- **In scope:** application chrome — buttons, labels, page titles, notifications, validation
  messages, PDF headers and footers, and the organization name supplied by the brand.
- **Out of scope:** survey content stored in the database (question text, answer options,
  section and step names, report bodies, message templates). It appears in the language it was
  authored in. Content translation is tracked on a separate branch.
- Text direction is right-to-left or left-to-right only; vertical writing modes are not
  supported.

## Where translations live

Each module keeps Vaadin's standard layout so the Vaadin Copilot internationalization panel
keeps working alongside hand-written extraction:

```
src/main/resources/vaadin-i18n/
  translations.properties               # English — default and fallback
  translations_es_419.properties        # Latin American Spanish
  translations_ar.properties            # Arabic
  translations.context.properties       # translator context per key (not read by Vaadin)
i18n/TRANSLATION_REQUEST.md             # generated handoff document
```

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
the union of `i18n.bundled.locales` and every `translations_*.properties` found in tiers 2–3,
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

## Direction (`LocaleDirectionConfig`, `LocaleLayout`)

`LocaleLayout.apply(ui, locale)` sets Vaadin's `Direction`, the `dir` and `lang` attributes on
the UI element (assertable in browserless tests) and `document.documentElement.lang`/`dir`.
Arabic, Hebrew, Persian, Urdu, Pashto, Sindhi, Uyghur, Yiddish, Dhivehi and Kurdish (Sorani)
are right-to-left by default; an optional `i18n-config.json` at the mount root (then local
`i18n/`, then classpath `META-INF/i18n/`) overrides or extends that:

```json
{ "locales": [ { "tag": "ar", "direction": "rtl" } ] }
```

Application CSS uses logical properties (`margin-inline-start`, `text-align: start`) so the
mirrored layout needs no per-language stylesheet.

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

Each module generates `i18n/TRANSLATION_REQUEST.md` from its English bundle and the context
sidecar (`TranslationRequestGeneratorTest`; on drift copy `target/i18n/TRANSLATION_REQUEST.md`
into `i18n/`). The document carries the application's purpose and audience, a glossary of terms
that must stay consistent or untranslated, the placeholder and HTML rules, every key with its
English text, location, maximum length and flags, and the return instructions: one
`translations_<tag>.properties`, same keys, same order. Hand it to a translator or an AI agent,
drop the returned file into the module (to ship it) or into the mount (to deploy it), and run
the module's `TranslationBundleConsistencyTest`.

## Tests that guard the design

| Test (per module) | Guarantees |
|---|---|
| `DisplayedStringsCoverageTest` | No prose-like string literal reaches a UI text API in the view sources (100 % coverage gate, NFR) |
| `DisplayedStringsSweepTest` | With the `zxx` pseudo-locale, every rendered route shows only `⟦key⟧` markers outside `data-i18n-content` subtrees |
| `TranslationBundleConsistencyTest` | Locale files carry exactly the English keys; every referenced key exists; placeholders match; no untranslated values; every key has context |
| `ElicitI18NProviderMountTest` | Mount-only locales, per-key override, language fallback, `!key!`, traversal guard, cache reset |
| `LocaleDirectionConfigTest`, `LocaleLayoutTest` | RTL/LTR defaults and manifest override; `?lang=`; switcher contents |
| `TranslationRequestGeneratorTest` | The committed handoff document matches the shipped bundle |
| `BrandUtilTest` | Localized brand names resolve tag → language → base |

Survey content rendered by the views (question components, review and report cards, authored
survey text on the About page) is marked with `data-i18n-content` so the sweep skips it.
