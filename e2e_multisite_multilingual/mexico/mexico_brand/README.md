# Elicit Mexico — site brand

The brand mounted at `/opt/brand` in the Mexico site's Survey and Admin
containers (`brand.file.system.path`, wired in `../docker-compose.yml`). It
replaces the platform default (`../../../elicit-brand`) for this site only;
USA keeps the default and Arabia has its own.

Its job is to make the Mexico site **recognizable at a glance**. Three
consoles running the same application in three languages are otherwise
identical, and a screenshot or a training recording has nothing in it that
says which site is on screen.

## The palette

Everything comes from the national flag.

| Token | Value | Where it lands |
| --- | --- | --- |
| `--mx-green` | `#006847` | header, primary buttons, success, info |
| `--mx-red` | `#CE1126` | the rule under the header, accent, focus ring |
| `--mx-oxblood` | `#A01C1C` | errors — *not* the flag red, see below |
| `--mx-paper` | `#FCFAF6` | page background (`--lumo-base-color` too) |
| `--mx-amber` | `#D9A02C` | warnings, kept from the default brand |

Contrast was checked rather than assumed, because the flag's colors are used
literally instead of being retuned:

- White on Bandera green is **6.84:1** — above AA, and above AAA for large
  text. The header needs no darkening.
- White on Bandera red is **5.63:1** — enough for a solid secondary button.
- **The red lives in the header's underline.** The default brand rejected an
  accent-colored rule beneath its header, because a green line under a blue
  one read as an unrelated third color. Green and red come from the same flag
  here, so the rule reads as the flag. It is also the only place the red
  appears at all: neither Survey nor Admin uses Vaadin's `secondary` button
  theme, and `--brand-accent` is not mapped to any Lumo token — so without it
  this brand would be a green brand with a red flag in the corner.
- **Error is deliberately not the flag red.** A secondary button and a
  validation message would otherwise be the same color. Oxblood is the same
  hue, darker and less saturated: 7.84:1 white-on-fill, 7.50:1 as text.
- `--brand-hover` / `--brand-active` are 10% and 20% tints, never solid. They
  are painted behind dark text (grid row hover, list selection), so a solid
  Bandera red there would put charcoal on red.

`colors/brand-colors.css` defines the same token names, in the same order, as
the default brand's — only the values differ. A brand that leaves one out
gets a Lumo fallback in that one place and looks half-themed.

## The flag artwork

`images/flag.svg` is the **civil flag** — the plain green/white/red tricolor,
without the coat of arms. That is a deliberate choice, twice over:

1. At the 48px height the header renders the mark at, the escudo is an
   illegible smudge. What reads at that size is the tricolor.
2. The escudo is a nationally regulated device whose reproduction is specified
   in law. A hand-drawn approximation is worse than none.

To use the full flag instead, drop official artwork in as
`images/flag.svg` and re-run `../../make-brand-images.py mexico/mexico_brand`.
Nothing else needs editing — every raster file is generated from that one SVG.

## Generated files

`../../make-brand-images.py` renders these from `images/flag.svg` and
`images/lockup.json`. Don't hand-edit them:

| File | What it is | Why that exact name |
| --- | --- | --- |
| `images/icon-white.png` | the flag as a white-matted chip | `BrandUtil.getIconResourcePath` hard-codes it |
| `images/HorizontalLogo.png` | chip + "Elicit México" wordmark | Admin ▸ System Branding shows it at 80px |
| `images/favicon.ico` | 16/32/48 browser icon | `BrandDiagnostics` expects `.ico` |

The white mat is not decoration: the header is Bandera green, and a flag with
a green band would otherwise bleed into it with no edge at all.

## Names

`brand-config.json` carries the base name `Elicit Mexico` and the `es-419`
name `Elicit México`. The base name is deliberately unaccented — it is also
what `BrandUtil.generateBrandKey` turns into the header's CSS class, and it
strips anything outside `[a-z0-9 -]`. Spanish-speaking respondents and
administrators see the accented name, which is the point of the `localized`
block: brand names are translated here, never in the application bundles.

Admin ▸ System Branding (UC-026) lists every file above and flags any that is
missing or unreadable — the quickest check that this mount arrived intact.
