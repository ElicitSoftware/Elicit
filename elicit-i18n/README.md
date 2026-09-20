# Elicit translations mount

This directory is the **default translations mount** for the Elicit applications. It is
volume-mounted read-only at `/opt/i18n` (the path read through the `i18n.file.system.path`
config property) in the `survey`, `admin`, `author` and `author-survey` containers. Each
application reads only its own sub-directory:

```
elicit-i18n/
├── i18n-config.json        # optional: text direction per language tag
├── survey/                 # Elicit Survey (also used by author-survey)
│   ├── translations.properties          # English, the fallback for every other language
│   ├── translations_es_419.properties   # Latin American Spanish
│   ├── translations_ar.properties       # Arabic
│   └── TRANSLATION_REQUEST.md           # hand this to a translator or an AI agent
├── admin/                  # Elicit Admin
└── author/                 # Elicit Author
```

Each application ships English only and shows no language selector until a second language is
mounted. `translations.properties` here is a copy of the English file inside the application
(the module tests keep the two identical); the Spanish and Arabic files exist only here, so
mounting this directory is what makes those languages available. How the mount is used:

- **Add a language:** drop `translations_<tag>.properties` into the app's sub-directory and
  restart the container. The language appears in the on-page language selector; a language
  the browser prefers is picked automatically. Tags use underscores in file names
  (`es_419`, `pt_BR`) and hyphens everywhere else.
- **Override a text:** a mounted file only needs the keys you want to change; every other
  key keeps its shipped value, and a key missing from a language falls back to English.
- **Right-to-left languages:** Arabic, Hebrew, Persian, Urdu and a few others are mirrored
  automatically. For any other language, declare it in `i18n-config.json`:
  `{"locales":[{"tag":"dv","direction":"rtl"}]}`.
- **Produce a translation:** give `TRANSLATION_REQUEST.md` to a translator or an AI agent;
  it contains every key with its context and the rules for placeholders and HTML, and asks
  for a complete `translations_<tag>.properties` back. Regenerate it in the module with the
  `TranslationRequestGeneratorTest` whenever the English file changes.
- **Brand names:** the organization name shown in the header comes from the mounted brand,
  not from these files. Translate it with a `localized` block in `brand-config.json` (see
  `elicit-brand/README.md`).

Nothing in this directory is served over HTTP; the applications read it server-side only.
