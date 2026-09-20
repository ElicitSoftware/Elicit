# test-partial-i18n

Fixture for the translations mount (see `elicit-i18n/README.md`). Mount it instead of
`elicit-i18n` to check the fallback rules:

- `survey/translations_es_419.properties` overrides a single Spanish key (`mainView.btnLogin`);
  every other Spanish text falls back to English, because the image ships English only and
  the complete Spanish file lives in `elicit-i18n`, which is not mounted in this setup.
- `survey/translations_fr.properties` adds French, a language `elicit-i18n` does not provide. It
  appears in the language selector; its three keys are French and every other text falls
  back to English.
- `i18n-config.json` declares French left-to-right explicitly (the default anyway) to show
  the manifest shape.
