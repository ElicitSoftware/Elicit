# Elicit translations mount

This directory is the **curated set of interface translations** for the Elicit applications,
maintained by ElicitSoftware beside each release. The Survey and Admin images carry English only;
every other language reaches them from a directory like this one, mounted read-only and named by
`i18n.file.system.path`. `docker-compose.yml` mounts this one at `/opt/i18n` in the `survey` and
`admin` containers. The authoring tool, which is private and not part of this compose file, mounts
the same directory from its own compose file (`../elicit-i18n`) and reads `author/`.

Each application reads only its own sub-directory (`i18n.app.name`):

```
elicit-i18n/
├── i18n-config.json                     # text direction and font scale per language tag
├── survey/                              # Elicit Survey
│   ├── translations_es_419.properties   # Latin American Spanish
│   └── translations_ar.properties       # Arabic
├── admin/                               # Elicit Admin
│   ├── translations_es_419.properties
│   └── translations_ar.properties
└── author/                              # Elicit Author (mounted by Author's own compose file)
    ├── translations_es_419.properties
    └── translations_ar.properties
```

There is no `translations.properties` here: English is inside each image at
`src/main/resources/vaadin-i18n/translations.properties`, and it is the fallback for every key.

## How a site uses it

- **Offer the curated languages:** mount this directory, or the copy that matches your release.
  Mount the same release's set as the images: the module tests check each file against that
  version's English keys.
- **Offer English only:** mount nothing. The language selector is not shown.
- **Offer fewer than you mount:** set `i18n.offered.locales`, e.g. `en,es-419`. A language left out
  is hidden from the selector, refused in a `?lang=` link, and not served for survey content
  either. English is always offered.
- **Add a language:** put `translations_<tag>.properties` in each application's sub-directory and
  restart the containers.
  - Tags use underscores in file names (`es_419`, `pt_BR`) and hyphens everywhere else.
  - Add it for Survey, Admin and Author together. A survey's content is served only in a language
    the Survey interface also offers; which content languages a survey may be published in is
    Author's `author.content.languages`.
- **Override a text:** a mounted file only needs the keys you want to change. Every other key falls
  back to English.
- **Right-to-left and text size:** `i18n-config.json` declares a language's direction and font
  scale: `{"locales":[{"tag":"ar","direction":"rtl","fontScale":1.15}]}`.
  - Arabic, Hebrew, Persian, Urdu and a few others are right to left without an entry.
  - `fontScale` multiplies the reader's base text size for pages in that language. A value outside
    0.75–2.0 is logged and ignored.
  - For one deployment, `i18n.direction.<tag>` and `i18n.font-scale.<tag>` override this file.
- **Produce a translation:** give the application's `i18n/TRANSLATION_REQUEST.md` to a translator or
  an AI agent. It lists every key with its context and the rules for placeholders and HTML.
- **Brand names:** the organization name in the header comes from the mounted brand, not from these
  files. Translate it with a `localized` block in `brand-config.json` (see
  `elicit-brand/README.md`).

## Keeping it consistent

- **Before every build:** `checkLanguages.sh` (run by `buildDockerImages.sh`) fails when:
  - `survey/`, `admin/` and `author/` hold different languages;
  - a translated bundle has crept back into an image;
  - a module still sets the retired `i18n.bundled.locales`.
- **In each module's tests:** `TranslationBundleConsistencyTest` reads `../elicit-i18n/<app>/`. It
  fails when a file here lacks an English key, carries an unknown one, or disagrees on placeholders.
  The modules' CI fetches this directory from the umbrella branch of the same name.

Nothing in this directory is served over HTTP; the applications read it server-side only.
