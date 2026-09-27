"""Render an HTML fragment to a PNG through the Chromium this suite already has.

Two scripts here need to turn markup into a picture -- ``make-brand-images.py`` for a site's
brand assets and ``make-recording.py`` for a recording's title cards -- and both need it for the
same reason: Chromium is the only text renderer on hand that shapes Arabic correctly, which PIL
and ffmpeg's ``drawtext`` do not. It is also already installed, because Playwright installed it
for the journey.

Not a script. ``import browser_shot`` from the directory it lives in.
"""

import pathlib
import shutil
import subprocess
import sys
import tempfile

from PIL import Image


def chromium() -> str:
    """Playwright's Chromium first (the suite already installs it), then Chrome."""
    cache = pathlib.Path.home() / "Library/Caches/ms-playwright"
    for pattern, tail in (
        ("chromium_headless_shell-*", "chrome-headless-shell-mac-x64/chrome-headless-shell"),
        ("chromium-*", "chrome-mac-x64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing"),
    ):
        found = sorted(cache.glob(pattern))
        if found and (found[-1] / tail).exists():
            return str(found[-1] / tail)
    for chrome in (
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        shutil.which("chromium"),
        shutil.which("google-chrome"),
    ):
        if chrome and pathlib.Path(chrome).exists():
            return chrome
    sys.exit("no Chromium found: run Playwright's `install chromium` (see README.md) or install Chrome")


def render(browser: str, html: str, width: int, height: int, scale: int = 1,
           background: str = "00000000") -> Image.Image:
    """Screenshot one HTML page at exactly ``width`` x ``height`` logical pixels.

    ``scale`` multiplies the device pixel ratio, so the returned image is
    ``width * scale`` wide. ``background`` is Chromium's own
    ``--default-background-color``, in RRGGBBAA: the default is transparent, so a page that paints
    no background of its own comes back with alpha.
    """
    with tempfile.TemporaryDirectory() as tmp:
        page = pathlib.Path(tmp, "page.html")
        page.write_text(html, encoding="utf-8")
        shot = pathlib.Path(tmp, "shot.png")
        subprocess.run(
            [browser, "--headless", "--disable-gpu", "--hide-scrollbars",
             "--force-device-scale-factor=%d" % scale,
             "--default-background-color=%s" % background,
             "--window-size=%d,%d" % (width, height),
             "--screenshot=%s" % shot, str(page)],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        return Image.open(shot).convert("RGBA")
