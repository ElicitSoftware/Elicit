# Elicit Default Brand

This directory is the **canonical default brand** for Elicit applications
(Survey, Admin, and future projects such as Author). It uses the Ink/Verdigris
palette drawn from the Elicit clipboard/checkmark mark, a warm Paper
background, and the system font stack.

Each application embeds a copy of this directory at
`src/main/resources/META-INF/brand/` as its built-in default. This directory
can also be volume-mounted at `/opt/brand` (the path read via the
`brand.file.system.path` config property) to override the embedded default
at runtime without rebuilding -- the two should be visually indistinguishable.

## Using This Brand in a New Project

1. Copy this entire directory into the new project as
   `src/main/resources/META-INF/brand/`.
2. Update `brand-config.json`'s `name`/`organization` if the new project
   needs its own display name (optional -- "Elicit Default Brand" is a
   reasonable default).
3. No other changes are required. The new project's `styles.css` must define
   the same `--lumo-*` variable mapping from `--brand-*` tokens that Survey
   and Admin already use (see either app's
   `src/main/resources/META-INF/resources/styles.css` for the pattern to
   copy).

## Directory Structure

```
elicit-brand/
├── brand-config.json          # Brand metadata (name, organization, logos, favicon)
├── brand-info.json            # Debug/display metadata (shown in a meta tag)
├── colors/
│   └── brand-colors.css       # CSS custom properties: palette + --brand-* mappings
├── typography/
│   ├── brand-typography.css   # System font stack, type scale, utility classes
│   └── typography.json        # Font metadata (for tooling/documentation)
├── images/
│   ├── favicon.ico
│   ├── HorizontalLogo.png
│   └── icon-white.png
└── theme.css                  # Imports colors + typography together
```

## Color Palette

- **Ink** (`--elicit-ink`, `#1B629C`) -- primary brand color, drives the
  header background and primary buttons.
- **Verdigris** (`--elicit-verdigris`, `#2FA084`) -- accent/interactive color,
  drives secondary buttons and the token-pill background.
- **Paper** (`--elicit-paper`, `#F7F5F0`) -- warm off-white base background.
- **Slate** (`--elicit-slate`, `#4A5A6A`) -- question/field-label text
  (`--brand-text-secondary`), ~6.5:1 contrast on Paper.
- **Charcoal** (`--elicit-charcoal`, `#1A2229`) -- heading/body text
  (`--brand-text-primary`), ~14.8:1 contrast on Paper. Deliberately near-black
  rather than Ink, so body copy doesn't read as blue-tinted next to the blue
  header.
- **Brick** / **Amber** -- muted error/warning status colors, tuned to sit in
  the same warm/muted family as Ink and Verdigris rather than stock reds/
  yellows.

## How Brand Loading Works

1. **Colors** (`colors/brand-colors.css`) define the `--brand-*` and
   `--elicit-*` CSS custom properties.
2. **Typography** (`typography/brand-typography.css`) defines font stacks,
   sizes, and a small set of `.elicit-*` utility classes.
3. **Theme** (`theme.css`) imports both of the above; this is what an
   application's `AppConfig` inlines into the page `<head>`.

Each application's `styles.css` provides the CSS variable *contract* --
mapping `--lumo-*` Lumo theme tokens to `--brand-*` tokens with sensible
fallbacks -- so a brand only needs to populate `--brand-*` values, never
`--lumo-*` directly. In particular, `--lumo-header-text-color` and
`--lumo-body-text-color` derive from `--lumo-shade`, which this brand
deliberately does **not** override -- leaving it unset gives near-black text
from Lumo's own default, avoiding a blue-tinted `--lumo-shade` fighting with
the Ink header.

## Testing a Brand Override Locally

```bash
docker compose up -d
# then, to test THIS brand as an external override instead of the embedded default:
# add under the survey/admin service in docker-compose.yml:
#   volumes:
#     - ./elicit-brand/:/opt/brand:ro
```
