#!/usr/bin/env python3
"""Render a site brand's PNG/ICO assets from its one piece of source artwork.

Each site brand keeps its flag as ``images/flag.svg`` -- that file is the only
thing anyone should have to edit -- plus ``images/lockup.json`` describing the
wordmark that sits beside it. This script produces the three raster files the
applications actually ask for, at the names they hard-code:

  images/icon-white.png    the header mark (Survey/Admin MainLayout, 48px tall)
  images/HorizontalLogo.png the full lockup (Admin > System Branding, 80px tall)
  images/favicon.ico       16/32/48 browser icon

All three are rendered at 3x for retina, by the Chromium that Playwright
already installed for this suite -- which is also what makes the Arabic
wordmark come out correctly shaped, something PIL cannot do on its own. Finding
that Chromium and driving it is browser_shot.py, shared with make-recording.py.

Usage:
    ./make-brand-images.py                       # both site brands
    ./make-brand-images.py mexico/mexico_brand   # just one

Requires python3 with Pillow (cropping and the multi-size .ico) and either
Playwright's Chromium or Google Chrome.
"""

import json
import pathlib
import sys

from PIL import Image

import browser_shot

SCALE = 3
DEFAULT_BRANDS = ["mexico/mexico_brand", "arabia/arabia_brand"]

# The white mat matters: a green flag on a green header would otherwise have no
# edge at all, and Mexico's green band would bleed into it. Every site mark is
# a flag chip -- flag, thin white border, hairline, small radius.
CHIP_CSS = """
  html, body { margin: 0; background: transparent; }
  .chip { display: inline-block; background: #fff; line-height: 0;
          box-sizing: border-box; border-radius: %(radius)s;
          padding: %(mat)s; box-shadow: 0 0 0 1px rgba(0, 0, 0, .18); }
  .chip svg { display: block; width: auto; height: %(flag)spx; }
"""


def shoot(browser: str, html: str, width: int, height: int) -> Image.Image:
    """Screenshot one HTML fragment on a transparent canvas, cropped to its ink."""
    image = browser_shot.render(browser, html, width, height, scale=SCALE)
    box = image.getbbox()
    if box is None:
        sys.exit("rendered nothing -- is the artwork empty?")
    return image.crop(box)


def flag_markup(brand: pathlib.Path) -> str:
    """The flag's own <svg> element, inlined.

    Inlined rather than referenced: Chromium will not load a file:// image as a
    subresource of a file:// page, so an <img src="flag.svg"> renders blank.
    """
    svg = (brand / "images" / "flag.svg").read_text(encoding="utf-8")
    return svg[svg.index("<svg"):]


def page(css: str, body: str, lang: str = "en", direction: str = "ltr") -> str:
    return (
        '<!DOCTYPE html><html lang="%s" dir="%s"><head><meta charset="utf-8">'
        "<style>%s</style></head><body>%s</body></html>" % (lang, direction, css, body)
    )


def header_icon(browser: str, brand: pathlib.Path) -> None:
    css = CHIP_CSS % {"radius": "3px", "mat": "2px", "flag": 44}
    html = page(css, '<div class="chip">%s</div>' % flag_markup(brand))
    shoot(browser, html, 400, 200).save(brand / "images" / "icon-white.png")


def favicon(browser: str, brand: pathlib.Path) -> None:
    """A square white tile with the flag across it -- a flag is never square."""
    css = CHIP_CSS % {"radius": "6px", "mat": "0", "flag": 26} + """
  .chip { width: 46px; height: 46px; display: flex; align-items: center;
          justify-content: center; padding: 0; }
  .chip svg { box-shadow: 0 0 0 1px rgba(0, 0, 0, .12); }
"""
    html = page(css, '<div class="chip">%s</div>' % flag_markup(brand))
    tile = shoot(browser, html, 400, 400)
    side = max(tile.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(tile, ((side - tile.width) // 2, (side - tile.height) // 2))
    square.save(brand / "images" / "favicon.ico", sizes=[(16, 16), (32, 32), (48, 48)])


def horizontal_logo(browser: str, brand: pathlib.Path) -> None:
    """Flag chip plus wordmark. Under dir=rtl the flag lands to the right of
    the Arabic text on its own, which is where it belongs."""
    spec = json.loads((brand / "images" / "lockup.json").read_text(encoding="utf-8"))
    css = CHIP_CSS % {"radius": "4px", "mat": "2px", "flag": 56} + """
  .lockup { display: inline-flex; align-items: center; gap: 16px;
            padding: 2px; font-family: %(font)s; }
  .wordmark { font-size: 40px; font-weight: %(weight)s; color: %(ink)s;
              letter-spacing: %(tracking)s; line-height: 1.15;
              white-space: nowrap; }
""" % {"font": spec["font"], "weight": spec.get("weight", 600),
       "ink": spec["inkColor"], "tracking": spec.get("letterSpacing", "0")}
    body = '<div class="lockup"><div class="chip">%s</div><div class="wordmark">%s</div></div>' % (
        flag_markup(brand), spec["wordmark"])
    html = page(css, body, lang=spec.get("lang", "en"), direction=spec.get("dir", "ltr"))
    shoot(browser, html, 1200, 300).save(brand / "images" / "HorizontalLogo.png")


def main(argv: list[str]) -> None:
    here = pathlib.Path(__file__).resolve().parent
    browser = browser_shot.chromium()
    for name in argv[1:] or DEFAULT_BRANDS:
        brand = (here / name).resolve()
        if not (brand / "images" / "flag.svg").is_file():
            sys.exit("%s has no images/flag.svg" % brand)
        header_icon(browser, brand)
        favicon(browser, brand)
        horizontal_logo(browser, brand)
        sizes = ", ".join(
            "%s %dx%d" % ((f,) + Image.open(brand / "images" / f).size)
            for f in ("icon-white.png", "HorizontalLogo.png", "favicon.ico"))
        print("%s: %s" % (brand.relative_to(here), sizes))


if __name__ == "__main__":
    main(sys.argv)
