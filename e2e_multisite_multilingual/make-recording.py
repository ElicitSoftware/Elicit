#!/usr/bin/env python3
"""Assemble a recorded journey into one narrated film.

The journey records itself when run with ``-De2e.record=true`` (see Recording.java): one webm per
persona visit, a pointer and a caption burned into each one, and a ``manifest.json`` saying which
phase each clip belongs to and what was being proved at every moment of it. This turns that into:

    journey.mp4    the film: a title card per phase, then that phase's clips, with chapters
    journey.vtt    the beats as subtitles, for a player that wants text it can search
    journey.png    a still for wherever the film is embedded

Usage:
    ./make-recording.py                       # the newest run under target/recording/
    ./make-recording.py target/recording/20260927-120000
    ./make-recording.py --speed 1.0           # life size, rather than the default 0.75
    ./make-recording.py --no-cards            # clips only, no title cards and no chapters

Why the cards are rendered rather than drawn by ffmpeg: ``drawtext`` has no bidi and no Arabic
shaping, so a card naming a site in its own language would come out as disconnected letters in the
wrong order. Chromium shapes them properly, and it is already installed (browser_shot.py).

Every clip is re-encoded once, to identical h264 parameters, before anything is joined. That is
what lets the concat demuxer stream-copy the join: a pass over forty clips of differing frame rates
would otherwise re-encode the whole film a second time.

Requires ffmpeg and ffprobe on PATH, plus Playwright's Chromium (or Chrome) and Pillow.
"""

import argparse
import base64
import html
import json
import mimetypes
import pathlib
import re
import shutil
import subprocess
import sys

import browser_shot

HERE = pathlib.Path(__file__).resolve().parent
FADE = 0.35                     # seconds of fade in and out on a title card


# ---- the run ---------------------------------------------------------------------------------

def find_manifest(argument: str | None) -> pathlib.Path:
    """The manifest to assemble: the one named, the one in the directory named, or the newest."""
    if argument:
        path = pathlib.Path(argument)
        if path.is_dir():
            path = path / "manifest.json"
        if not path.is_file():
            sys.exit("no manifest at %s" % path)
        return path.resolve()
    runs = sorted((HERE / "target" / "recording").glob("*/manifest.json"))
    if not runs:
        sys.exit("nothing recorded yet: run the journey with -De2e.record=true "
                 "(see README.md, 'Recording the journey')")
    return runs[-1].resolve()


def probe_duration(clip: pathlib.Path) -> float:
    """Seconds of video in a clip, decoding it if the container will not say."""
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=nw=1:nk=1", str(clip)],
        capture_output=True, text=True).stdout.strip()
    try:
        return float(out)
    except ValueError:
        pass
    # A webm whose header carries no duration: decode it and take the last timestamp.
    decoded = subprocess.run(["ffmpeg", "-i", str(clip), "-f", "null", "-"],
                             capture_output=True, text=True).stderr
    stamps = re.findall(r"time=(\d+):(\d\d):(\d\d\.\d+)", decoded)
    if not stamps:
        return 0.0
    hours, minutes, seconds = stamps[-1]
    return int(hours) * 3600 + int(minutes) * 60 + float(seconds)


def run(command: list, verbose: bool) -> None:
    if verbose:
        print("  $ " + " ".join(str(c) for c in command))
    done = subprocess.run(command, capture_output=not verbose, text=True)
    if done.returncode != 0:
        if not verbose:
            sys.stderr.write(done.stderr or "")
        sys.exit("ffmpeg failed: " + " ".join(str(c) for c in command[:4]) + " ...")


# ---- title cards -----------------------------------------------------------------------------

CARD_CSS = """
  @page { margin: 0; }
  html, body { margin: 0; height: 100%%; background: #0F141A; }
  body { display: flex; align-items: center; justify-content: center;
         font-family: -apple-system, BlinkMacSystemFont, "SF Arabic", "Segoe UI",
                      "Noto Sans Arabic", "Geeza Pro", Roboto, Helvetica, Arial, sans-serif;
         color: #fff; -webkit-font-smoothing: antialiased; }
  body::before { content: ""; position: fixed; inset: 0;
                 background: radial-gradient(120%% 90%% at 50%% 0%%,
                             rgba(27, 98, 156, .38), rgba(15, 20, 26, 0) 62%%); }
  .card { position: relative; width: %(content)dpx; text-align: center; }
  .kicker { font-size: 19px; font-weight: 600; letter-spacing: .20em; text-transform: uppercase;
            color: rgba(255, 255, 255, .55); }
  .number { margin: 6px 0 22px; font-size: 128px; font-weight: 300; line-height: 1;
            letter-spacing: -.03em; font-variant-numeric: tabular-nums; }
  .number small { font-size: 40px; font-weight: 400; opacity: .42; letter-spacing: 0; }
  h1 { margin: 0; font-size: 58px; font-weight: 620; line-height: 1.12; letter-spacing: -.022em; }
  h2 { margin: 0; font-size: 40px; font-weight: 500; line-height: 1.26; letter-spacing: -.012em;
       color: rgba(255, 255, 255, .93); }
  .sub { margin-top: 26px; font-size: 24px; font-weight: 450; line-height: 1.4;
         color: rgba(255, 255, 255, .62); }
  .rule { width: 92px; height: 4px; margin: 34px auto 0; border-radius: 2px;
          background: linear-gradient(90deg, rgba(255, 255, 255, .25), rgba(255, 255, 255, .85)); }
  .sites { display: flex; justify-content: center; gap: 22px; margin-top: 54px; }
  .site { display: flex; align-items: center; gap: 13px; padding: 15px 22px; border-radius: 14px;
          background: rgba(255, 255, 255, .07);
          box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .12); }
  .site .mark { width: 46px; height: 31px; border-radius: 4px; background-size: contain;
                background-position: center; background-repeat: no-repeat; }
  .site .text { text-align: left; }
  .site .name { font-size: 22px; font-weight: 620; line-height: 1.15; }
  .site .lang { font-size: 15px; font-weight: 500; line-height: 1.3;
                color: rgba(255, 255, 255, .58); }
"""


def data_uri(path: pathlib.Path) -> str:
    """A file:// image is not loadable as a subresource of a file:// page; inline it."""
    if not path.is_file():
        return ""
    kind = mimetypes.guess_type(path.name)[0] or "image/png"
    return "data:%s;base64,%s" % (kind, base64.b64encode(path.read_bytes()).decode("ascii"))


def card_page(width: int, height: int, body: str) -> str:
    css = CARD_CSS % {"content": int(width * 0.74)}
    return ('<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><style>%s</style></head>'
            '<body><div class="card">%s</div></body></html>' % (css, body))


def site_strip(manifest: dict) -> str:
    rows = []
    for site in manifest.get("sites", []):
        mark = data_uri(HERE / site["mark"])
        rows.append(
            '<div class="site"><div class="mark" style="background-image:url(%s);'
            'background-color:%s"></div><div class="text"><div class="name">%s</div>'
            '<div class="lang">%s</div></div></div>'
            % ("'%s'" % mark if mark else "none", site["color"],
               html.escape(site["name"]), html.escape(site["language"])))
    return '<div class="sites">%s</div>' % "".join(rows) if rows else ""


def opening_card(manifest: dict, width: int, height: int) -> str:
    return card_page(width, height,
                     '<div class="kicker">Elicit</div>'
                     '<h1>%s</h1><div class="sub">%s</div>%s'
                     % (html.escape(manifest.get("title", "")),
                        html.escape(manifest.get("subtitle", "")),
                        site_strip(manifest)))


def phase_card(phase: dict, total: int, width: int, height: int) -> str:
    return card_page(width, height,
                     '<div class="kicker">Phase</div>'
                     '<div class="number">%d<small>&thinsp;/&thinsp;%d</small></div>'
                     '<h2>%s</h2><div class="rule"></div>'
                     % (phase["number"], total, html.escape(phase["narration"] or phase["id"])))


def closing_card(manifest: dict, phases: int, clips: int, width: int, height: int) -> str:
    recorded = (manifest.get("recordedAt") or "")[:10]
    return card_page(width, height,
                     '<div class="kicker">One definition file</div>'
                     '<h1>%s</h1>'
                     '<div class="sub">%d phases &middot; %d recorded visits &middot; '
                     'three sites &middot; three languages%s</div>%s'
                     % (html.escape(manifest.get("title", "")), phases, clips,
                        " &middot; " + html.escape(recorded) if recorded else "",
                        site_strip(manifest)))


# ---- segments --------------------------------------------------------------------------------

def encode_flags(fps: int, crf: int) -> list:
    """One encoder setting for every segment, so the join can be a stream copy."""
    return ["-c:v", "libx264", "-preset", "veryfast", "-crf", str(crf), "-pix_fmt", "yuv420p",
            "-profile:v", "high", "-level", "4.1", "-g", str(fps * 2), "-r", str(fps),
            "-video_track_timescale", "90000", "-an"]


def fit(width: int, height: int) -> str:
    """Scale to fit and pad, so a clip recorded at another size still joins cleanly."""
    return ("scale=%d:%d:force_original_aspect_ratio=decrease,"
            "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=#0F141A,setsar=1" % (width, height, width, height))


def encode_clip(source: pathlib.Path, target: pathlib.Path, size: tuple, speed: float,
                fps: int, crf: int, verbose: bool) -> None:
    chain = []
    if speed != 1.0:
        chain.append("setpts=PTS/%s" % speed)
    chain += [fit(*size), "fps=%d" % fps]
    run(["ffmpeg", "-y", "-i", str(source), "-vf", ",".join(chain), "-fps_mode", "cfr"]
        + encode_flags(fps, crf) + [str(target)], verbose)


def encode_card(png: pathlib.Path, target: pathlib.Path, seconds: float, size: tuple,
                fps: int, crf: int, verbose: bool) -> None:
    chain = [fit(*size), "fps=%d" % fps,
             "fade=t=in:st=0:d=%.2f" % FADE,
             "fade=t=out:st=%.2f:d=%.2f" % (max(0.0, seconds - FADE), FADE)]
    run(["ffmpeg", "-y", "-loop", "1", "-t", "%.3f" % seconds, "-i", str(png),
         "-vf", ",".join(chain)] + encode_flags(fps, crf) + [str(target)], verbose)


# ---- subtitles and chapters -------------------------------------------------------------------

def timestamp(seconds: float) -> str:
    hours, rest = divmod(max(0.0, seconds), 3600)
    minutes, seconds = divmod(rest, 60)
    return "%02d:%02d:%06.3f" % (hours, minutes, seconds)


def write_vtt(target: pathlib.Path, cues: list) -> None:
    lines = ["WEBVTT", ""]
    for index, (start, end, site, text) in enumerate(cues, start=1):
        lines += [str(index), "%s --> %s" % (timestamp(start), timestamp(end)),
                  ("%s: %s" % (site, text)) if site else text, ""]
    target.write_text("\n".join(lines), encoding="utf-8")


def write_chapters(target: pathlib.Path, chapters: list, title: str) -> None:
    lines = [";FFMETADATA1", "title=%s" % title]
    for start, end, name in chapters:
        lines += ["[CHAPTER]", "TIMEBASE=1/1000",
                  "START=%d" % round(start * 1000), "END=%d" % round(end * 1000),
                  # ffmetadata escapes =;#\ and a newline with a backslash.
                  "title=%s" % re.sub(r"([=;#\\\n])", r"\\\1", name)]
    target.write_text("\n".join(lines) + "\n", encoding="utf-8")


# ---- assembly --------------------------------------------------------------------------------

def assemble(manifest_path: pathlib.Path, options) -> None:
    for tool in ("ffmpeg", "ffprobe"):
        if not shutil.which(tool):
            sys.exit("%s is not on PATH" % tool)

    run_dir = manifest_path.parent
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    viewport = manifest.get("viewport") or {}
    size = (int(options.width or viewport.get("width", 1440)),
            int(options.height or viewport.get("height", 1024)))
    # h264 wants both sides even.
    size = (size[0] - size[0] % 2, size[1] - size[1] % 2)
    total_phases = manifest.get("totalPhases") or len(manifest.get("phases", []))
    card_seconds = options.card_seconds or manifest.get("titleCardSeconds", 3)

    clips = [c for c in manifest.get("clips", []) if (run_dir / c["file"]).is_file()]
    if not clips:
        sys.exit("the manifest lists no clip that exists under %s" % run_dir)

    work = run_dir / "segments"
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)
    browser = browser_shot.chromium() if options.cards else None

    print("%s: %d clips, %d phases -> %dx%d @%dfps, speed %.2fx"
          % (run_dir.name, len(clips), len(manifest.get("phases", [])), size[0], size[1],
             options.fps, options.speed))

    segments: list[pathlib.Path] = []
    cues: list = []
    chapters: list = []
    timeline = 0.0                      # seconds of film written so far
    index = 0

    def card(name: str, markup: str, seconds: float) -> None:
        """Renders a title card and appends it to the film."""
        nonlocal timeline, index
        png = work / ("%03d-%s.png" % (index, name))
        browser_shot.render(browser, markup, size[0], size[1], background="0F141AFF").save(png)
        segment = work / ("%03d-%s.mp4" % (index, name))
        encode_card(png, segment, seconds, size, options.fps, options.crf, options.verbose)
        segments.append(segment)
        timeline += seconds
        index += 1

    if options.cards:
        card("opening", opening_card(manifest, *size), card_seconds + 1.5)

    # The phases in order, each one its card followed by the clips recorded during it. A clip
    # whose phase is 0 -- anything recorded before the first phase announced itself -- is grouped
    # under 0 and played first, so nothing recorded is silently dropped.
    by_phase: dict = {}
    for clip in clips:
        by_phase.setdefault(clip.get("phase", 0), []).append(clip)
    phases = {p["number"]: p for p in manifest.get("phases", [])}
    beats = manifest.get("beats", [])

    for number in sorted(by_phase):
        phase_start = timeline
        phase = phases.get(number)
        if options.cards and phase:
            card("phase%02d" % number, phase_card(phase, total_phases, *size), card_seconds)

        for clip in by_phase[number]:
            source = run_dir / clip["file"]
            wall = max(1, clip["end"] - clip["start"])
            recorded = probe_duration(source)
            if recorded <= 0.05:
                print("  skipping %s: no decodable video" % clip["file"])
                continue
            played = recorded / options.speed
            segment = work / ("%03d-clip.mp4" % index)
            encode_clip(source, segment, size, options.speed, options.fps, options.crf,
                        options.verbose)
            segments.append(segment)

            # Beats are wall-clock; a screencast drifts from wall time, so each beat is placed at
            # the same *fraction* of the clip it fell in rather than at its raw offset.
            inside = [b for b in beats if clip["start"] <= b["at"] <= clip["end"]]
            for position, beat in enumerate(inside):
                at = timeline + ((beat["at"] - clip["start"]) / wall) * played
                until = (timeline + ((inside[position + 1]["at"] - clip["start"]) / wall) * played
                         if position + 1 < len(inside) else timeline + played)
                # The cue ends when the next beat replaces it on screen, as the burned-in
                # caption does -- a minimum duration here would overlap the following cue.
                cues.append((at, max(at + 0.2, until), beat.get("site"), beat["text"]))

            timeline += played
            index += 1

        if timeline > phase_start:
            name = ("%d. %s" % (number, phase["narration"]) if phase
                    else "Before the first phase")
            chapters.append((phase_start, timeline, name))

    if options.cards:
        card("closing", closing_card(manifest, len(phases), len(clips), *size), card_seconds)

    if not segments:
        sys.exit("nothing to join")

    # The join: identical encoder settings above mean this copies streams rather than re-encoding.
    listing = work / "concat.txt"
    listing.write_text("".join("file '%s'\n" % s.name for s in segments), encoding="utf-8")
    body = work / "body.mp4"
    run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", str(listing), "-c", "copy",
         str(body)], options.verbose)

    film = run_dir / options.out
    if options.cards and chapters:
        meta = work / "chapters.txt"
        write_chapters(meta, chapters, manifest.get("title", "Elicit journey"))
        run(["ffmpeg", "-y", "-i", str(body), "-i", str(meta), "-map_metadata", "1",
             "-c", "copy", "-movflags", "+faststart", str(film)], options.verbose)
    else:
        run(["ffmpeg", "-y", "-i", str(body), "-c", "copy", "-movflags", "+faststart",
             str(film)], options.verbose)

    # Named after the film, so a second pass with another --out does not overwrite the first's.
    subtitles = film.with_suffix(".vtt")
    write_vtt(subtitles, cues)
    poster = film.with_suffix(".png")
    run(["ffmpeg", "-y", "-ss", "%.2f" % min(1.5, timeline / 2), "-i", str(film),
         "-frames:v", "1", str(poster)], options.verbose)

    if not options.keep:
        shutil.rmtree(work)

    minutes, seconds = divmod(probe_duration(film), 60)
    print("  %s  %d:%05.2f, %.1f MB%s" % (film.name, minutes, seconds,
                                          film.stat().st_size / (1 << 20),
                                          ", %d chapters" % len(chapters) if options.cards else ""))
    print("  %s  %d cues" % (subtitles.name, len(cues)))
    print("  %s  a still from %.1fs in" % (poster.name, min(1.5, timeline / 2)))
    print(film.resolve())


def main(argv: list) -> None:
    parser = argparse.ArgumentParser(
        description="Assemble a recorded Elicit journey into one narrated film.")
    parser.add_argument("run", nargs="?", help="a recording directory or its manifest.json "
                                              "(default: the newest under target/recording)")
    parser.add_argument("--speed", type=float, default=0.75,
                        help="how fast the clips play; title cards keep their length. The default "
                             "0.75 plays them a quarter slower than they were recorded, which is "
                             "what makes a Vaadin form legible; 1.0 is life size, 1.5 a skim")
    parser.add_argument("--fps", type=int, default=30)
    parser.add_argument("--crf", type=int, default=23, help="h264 quality, lower is better")
    parser.add_argument("--card-seconds", type=float, default=None,
                        help="how long a title card holds (default: the manifest's)")
    parser.add_argument("--no-cards", dest="cards", action="store_false",
                        help="clips only: no title cards, no chapters")
    parser.add_argument("--width", type=int, default=None)
    parser.add_argument("--height", type=int, default=None)
    parser.add_argument("--out", default="journey.mp4")
    parser.add_argument("--keep", action="store_true", help="keep the intermediate segments")
    parser.add_argument("--verbose", action="store_true")
    options = parser.parse_args(argv[1:])
    assemble(find_manifest(options.run), options)


if __name__ == "__main__":
    main(sys.argv)
