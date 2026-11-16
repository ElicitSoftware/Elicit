#!/usr/bin/env python3
"""Draw the Choice E logo files of the default brand.

Writes the SVG masters to elicit-brand/images/source/ and prints nothing else. The lettering is
IBM Plex Sans converted to outlines, so no file depends on an installed font. The PNG and ICO files
the applications read are rendered from the masters; see elicit-brand/README.md.

    python3 -m venv venv && venv/bin/pip install fonttools brotli
    venv/bin/python docs/brand-proposals/build-brand-assets.py
"""
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parents[2]
FONTS = ROOT / "elicit-brand" / "typography" / "fonts"
OUT = ROOT / "elicit-brand" / "images" / "source"

INK, VERDIGRIS = "#0A3D62", "#22775D"
WHITE, VERDIGRIS_LIGHT = "#FFFFFF", "#5CBBA4"

# The mark on a 256 grid: three answer rows, the middle one selected. It spans x 32..228, y 36..220.
MARK = """<circle cx="58" cy="62" r="20" fill="none" stroke="{ink}" stroke-width="12"/>
<path d="M104 62 H214" stroke="{ink}" stroke-width="28" stroke-linecap="round"/>
<circle cx="58" cy="128" r="26" fill="{green}"/>
<path d="M104 128 H180" stroke="{green}" stroke-width="28" stroke-linecap="round"/>
<circle cx="58" cy="194" r="20" fill="none" stroke="{ink}" stroke-width="12"/>
<path d="M104 194 H214" stroke="{ink}" stroke-width="28" stroke-linecap="round"/>"""
MARK_TOP, MARK_BOTTOM, MARK_LEFT, MARK_RIGHT = 36, 220, 32, 228


def outline(font_file, text, size, x, baseline, width=None):
    """The text as one SVG path. With a width, the letters are spaced out to span it exactly."""
    font = TTFont(FONTS / font_file)
    glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
    scale = size / font["head"].unitsPerEm
    names = [cmap[ord(c)] for c in text]
    advances = [glyphs[n].width * scale for n in names]
    ink_width = sum(advances)
    gap = (width - ink_width) / (len(names) - 1) if width else 0
    pen = SVGPathPen(glyphs, lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
    cursor = x
    for name, advance in zip(names, advances):
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, cursor, baseline)))
        cursor += advance + gap
    cap_height = font["OS/2"].sCapHeight * scale
    return pen.getCommands(), cursor - gap - x, cap_height


def mark(ink, green):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256">\n'
            f'{MARK.format(ink=ink, green=green)}\n</svg>\n')


def horizontal(ink, green):
    pad, gap, elicit_size, survey_size, leading = 16, 30, 96, 34, 23
    _, _, cap = outline("IBMPlexSans-Bold.woff2", "ELICIT", elicit_size, 0, 0)
    _, _, survey_cap = outline("IBMPlexSans-SemiBold.woff2", "SURVEY", survey_size, 0, 0)
    mark_height = cap + leading + survey_cap
    scale = mark_height / (MARK_BOTTOM - MARK_TOP)
    mark_width = (MARK_RIGHT - MARK_LEFT) * scale
    text_x = pad + mark_width + gap
    top, bottom = pad, pad + mark_height
    # ELICIT's cap height meets the mark's top, SURVEY's baseline its bottom.
    elicit, elicit_width, _ = outline("IBMPlexSans-Bold.woff2", "ELICIT", elicit_size, text_x, top + cap)
    # The side bearings of E and T sit inside the advance; SURVEY is inset to line up optically.
    inset = elicit_size * 0.05
    survey, _, _ = outline("IBMPlexSans-SemiBold.woff2", "SURVEY", survey_size, text_x + inset, bottom,
                           elicit_width - 2 * inset)
    width, height = text_x + elicit_width + pad, 2 * pad + mark_height
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width:.0f} {height:.0f}">\n'
            f'<g transform="translate({pad - MARK_LEFT * scale:.2f} {pad - MARK_TOP * scale:.2f}) scale({scale:.4f})">\n'
            f'{MARK.format(ink=ink, green=green)}\n</g>\n'
            f'<path d="{elicit}" fill="{ink}"/>\n<path d="{survey}" fill="{green}"/>\n</svg>\n')


def favicon():
    """The white mark on an Ink tile, so the icon reads on a light and on a dark browser tab."""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256">\n'
            f'<rect width="256" height="256" rx="48" fill="{INK}"/>\n'
            f'<g transform="translate(25.6 25.6) scale(0.8)">\n'
            f'{MARK.format(ink=WHITE, green=VERDIGRIS_LIGHT)}\n</g>\n</svg>\n')


OUT.mkdir(parents=True, exist_ok=True)
(OUT / "mark.svg").write_text(mark(INK, VERDIGRIS))
(OUT / "mark-white.svg").write_text(mark(WHITE, VERDIGRIS_LIGHT))
(OUT / "horizontal.svg").write_text(horizontal(INK, VERDIGRIS))
(OUT / "horizontal-white.svg").write_text(horizontal(WHITE, VERDIGRIS_LIGHT))
(OUT / "favicon.svg").write_text(favicon())
