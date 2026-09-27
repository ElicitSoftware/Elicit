# Elicit Arabia — site brand

The brand mounted at `/opt/brand` in the Arabia site's Survey and Admin
containers (`brand.file.system.path`, wired in `../docker-compose.yml`). It
replaces the platform default (`../../../elicit-brand`) for this site only;
USA keeps the default and Mexico has its own.

Its job is to make the Arabia site **recognizable at a glance**. Three
consoles running the same application in three languages are otherwise
identical, and a screenshot or a training recording has nothing in it that
says which site is on screen.

## The flag artwork

Arabia is not a country. It is this suite's third site, and `images/flag.svg`
is a flag invented for it: a green field carrying a white eight-point star
(*khatim*, a geometric motif, not a script or a religious device). It is
deliberately **no real nation's flag**.

That is worth stating plainly, because the obvious reading of "Arabia" is
Saudi Arabia, and the Saudi flag is the one flag you should not reach for
here. It bears the *shahada* — the Islamic declaration of faith — which is why
it is never flown at half-mast, never printed on merchandise, and not used as
a decorative regional icon. Nor could it be reproduced honestly: its
calligraphy is not something to approximate in hand-written SVG.

The green is the green the Saudi flag uses, `#006C35`, so the site's palette
still reads as regional — which is what the brand is actually for.

If you do want a real country's flag here, drop its artwork in as
`images/flag.svg` and re-run `../../make-brand-images.py arabia/arabia_brand`.
Nothing else needs editing — every raster file is generated from that one SVG.

## The palette

A two-color flag does not supply everything a UI needs, so green and white
are joined by Brass and Gold.

| Token | Value | Where it lands |
| --- | --- | --- |
| `--ar-green` | `#006C35` | header, primary buttons, success, info |
| `--ar-brass` | `#8A6D1F` | secondary buttons, accent, focus ring |
| `--ar-gold` | `#C9A227` | warning fills and badges |
| `--ar-gold-text` | `#7A6115` | warning drawn as *text* |
| `--ar-crimson` | `#A3231B` | errors |
| `--ar-sand` | `#FBF8F1` | page background (`--lumo-base-color` too) |

Contrast was checked rather than assumed:

- White on the flag green is **6.57:1** — above AA, and above AAA for large
  text. The header needs no darkening.
- **Gold is split into two tokens**, and this is the trap the palette is built
  around: `#C9A227` is 2.42:1 under white text and 2.32:1 as text on Sand —
  unusable for either. Brass is the same hue darkened to 4.90:1 white-on-fill,
  and Gold-text to ~4.6:1 on Sand. The default brand splits Amber the same
  way, for the same reason.
- `--brand-hover` / `--brand-active` are 10% and 20% tints, never solid. They
  are painted behind dark text (grid row hover, list selection), so a solid
  Brass there would put charcoal on brass.

`colors/brand-colors.css` defines the same token names, in the same order, as
the default brand's — only the values differ. A brand that leaves one out
gets a Lumo fallback in that one place and looks half-themed.

## Right to left

This is the only site of the three that renders RTL, and the brand is built
for it:

- **`typography/brand-typography.css` names Arabic faces first** (`SF Arabic`,
  `Geeza Pro`, `Noto Naskh Arabic`, …). Left to the Latin stack, Arabic falls
  through to whatever the browser picks for the script — usually a face whose
  metrics disagree with the Latin one beside it, which is what makes an RTL
  page look assembled from two fonts. This is the one place the two site
  brands genuinely differ in type.
- **Line height opens to 1.7.** Arabic ascenders, descenders and dot clusters
  need more leading than Latin at the same size.
- **Nothing in `brand-colors.css` is direction-dependent.** The only edge it
  draws is the header's bottom border, which is the same on both sides.
- **The access-code pill is pinned `direction: ltr` with `unicode-bidi:
  isolate`.** Access codes are Latin letters and digits whatever the page
  language, and without the isolate an RTL paragraph reorders them on screen.

## Generated files

`../../make-brand-images.py` renders these from `images/flag.svg` and
`images/lockup.json`. Don't hand-edit them:

| File | What it is | Why that exact name |
| --- | --- | --- |
| `images/icon-white.png` | the flag as a white-matted chip | `BrandUtil.getIconResourcePath` hard-codes it |
| `images/HorizontalLogo.png` | chip + إيليسيت العربية wordmark | Admin ▸ System Branding shows it at 80px |
| `images/favicon.ico` | 16/32/48 browser icon | `BrandDiagnostics` expects `.ico` |

The white mat is not decoration: the header is the same green as the flag, so
without it the mark would have no edge at all and simply disappear.

The lockup is rendered through Chromium rather than drawn with an image
library, which is what makes the Arabic come out correctly shaped and joined,
and puts the flag to the right of the text on its own under `dir="rtl"`.

## Names

`brand-config.json` carries the base name `Elicit Arabia` and the `ar` name
`إيليسيت العربية`. Arabic-reading respondents and administrators see the
Arabic name — that is the point of the `localized` block: brand names are
translated here, never in the application bundles.

Admin ▸ System Branding (UC-026) lists every file above and flags any that is
missing or unreadable — the quickest check that this mount arrived intact.
