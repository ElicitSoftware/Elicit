/*
 * The on-screen half of the recording harness (see Recording.java).
 *
 * Playwright's video is a screencast of the page, so anything this script draws is in the
 * recording -- and two things are missing from a raw screencast that a viewer needs:
 *
 *   1. A pointer. Playwright drives the mouse through CDP, which moves no visible cursor and
 *      leaves no trace of a click, so a raw recording is a page that changes for no reason. This
 *      draws one, and it needs no cooperation from the test: CDP input arrives in the page as
 *      ordinary `mousemove`/`mousedown` events at real coordinates, so listening in the capture
 *      phase on `window` sees every click the journey makes, wherever in the page objects it was
 *      written. The cursor is animated toward each new position rather than teleported, which is
 *      why the recording run needs a slowMo longer than TRAVEL_MS (Recording.SLOW_MO_DEFAULT):
 *      the click has to land after the cursor has visibly arrived.
 *
 *   2. Words. A three-site multilingual journey is unintelligible without being told which site
 *      this is and what is being proved; the lower third carries the phase, the beat and the
 *      site's own brand mark, which is the same mark the page header shows.
 *
 * Everything lives in a *closed* shadow root. That is not decoration: Playwright's locators
 * pierce open shadow roots, so an overlay in one could be matched by `getByText` and make an
 * assertion pass or fail on the caption rather than on the page. A closed root cannot be
 * reached by any locator, while still rendering into the video. The host is `pointer-events:
 * none`, so it is not a hit-test target either and never steals a click.
 *
 * Injected with BrowserContext.addInitScript, i.e. re-run on every document. Java pushes the
 * phase and beat back in after each navigation (Recording.push).
 */
(() => {
    'use strict';

    if (window.top !== window) return;          // frames get no chrome of their own
    if (window.__elicitRec) return;             // a soft (Vaadin) navigation keeps the one we have

    const CONFIG = "__ELICIT_RECORDER_CONFIG__";
    const TRAVEL_MIN = 90;                      // ms; short hops still read as movement
    const TRAVEL_MAX = 420;                     // ms; a corner-to-corner sweep
    const SPEED = 1.8;                          // px per ms of travel
    const POSITION_KEY = 'elicit.recorder.cursor';

    const state = {phase: '', narration: '', number: 0, total: 0, beat: '', beatLang: ''};

    let host = null, root = null;
    let cursor = null, third = null, numEl = null, ofEl = null, titleEl = null,
        beatEl = null, markEl = null, siteEl = null, appEl = null, fillEl = null, focusEl = null;
    let cx = 0, cy = 0, placed = false, clicks = 0, focusTimer = 0;

    // ---- the site this page belongs to ---------------------------------------------------------

    /**
     * Which site and which application the current URL is, by prefix. Derived from the URL rather
     * than told to us, so a visit that walks from Keycloak into Admin relabels itself without the
     * journey having to say so.
     */
    function place() {
        const url = location.href;
        for (const site of CONFIG.sites) {
            for (const app of site.apps) {
                if (url.indexOf(app.url) === 0) return {site: site, app: app.name};
            }
        }
        for (const extra of CONFIG.extras || []) {
            if (url.indexOf(extra.url) === 0) return {site: null, app: extra.name};
        }
        return {site: null, app: ''};
    }

    // ---- building ------------------------------------------------------------------------------

    const CSS = `
      * { box-sizing: border-box; }
      .layer { position: absolute; inset: 0; overflow: hidden; direction: ltr;
               font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica,
                            Arial, sans-serif;
               font-variant-numeric: tabular-nums; -webkit-font-smoothing: antialiased; }

      /* --- the pointer --- */
      #cursor { position: absolute; left: 0; top: 0; width: 26px; height: 26px;
                opacity: 0; transform: translate3d(-40px, -40px, 0);
                transition: transform 180ms cubic-bezier(.22, .61, .36, 1), opacity 160ms linear;
                will-change: transform; }
      #cursor.on { opacity: 1; }
      #cursor.down .hot { transform: scale(.82); }
      #cursor .hot { transform-origin: 3px 3px; transition: transform 90ms ease-out; }

      /* --- a click --- */
      .ripple { position: absolute; left: 0; top: 0; width: 16px; height: 16px; margin: -8px 0 0 -8px;
                border-radius: 50%; border: 3px solid rgba(17, 22, 28, .8);
                background: rgba(255, 255, 255, .3);
                box-shadow: 0 0 0 2px rgba(255, 255, 255, .9), inset 0 0 0 2px rgba(255, 255, 255, .9);
                animation: pop 520ms cubic-bezier(.2, .7, .3, 1) forwards; }
      @keyframes pop { from { transform: scale(.3);  opacity: 1; }
                       to   { transform: scale(3.4); opacity: 0; } }

      /* --- the field that just took focus --- */
      #focus { position: absolute; border-radius: 5px; opacity: 0;
               box-shadow: 0 0 0 2px rgba(255, 255, 255, .9), 0 0 0 3.5px rgba(17, 22, 28, .5);
               transition: opacity 220ms linear; }
      #focus.on { opacity: 1; }

      /* --- the lower third --- */
      #third { position: absolute; left: 34px; right: 34px; bottom: 30px; display: flex;
               align-items: center; gap: 18px; padding: 13px 18px 15px; border-radius: 14px;
               background: rgba(18, 23, 30, .84); color: #fff; overflow: hidden;
               box-shadow: 0 12px 34px rgba(0, 0, 0, .30), inset 0 0 0 1px rgba(255, 255, 255, .10);
               backdrop-filter: blur(7px) saturate(1.2);
               opacity: 0; transform: translateY(10px);
               transition: opacity 320ms ease, transform 320ms ease; }
      #third.on { opacity: 1; transform: none; }

      #chip { flex: none; display: flex; align-items: baseline; gap: 4px; padding: 6px 11px 7px;
              border-radius: 9px; background: rgba(255, 255, 255, .13);
              box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .14); }
      #num { font-size: 21px; font-weight: 650; line-height: 1; letter-spacing: -.01em; }
      #of  { font-size: 12px; font-weight: 500; line-height: 1; opacity: .62; }

      #mid { flex: 1 1 auto; min-width: 0; }
      #title { font-size: 17px; font-weight: 620; line-height: 1.25; letter-spacing: -.005em;
               white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      #beat { margin-top: 3px; font-size: 14px; font-weight: 450; line-height: 1.3; opacity: .78;
              white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
              font-family: -apple-system, BlinkMacSystemFont, "SF Arabic", "Segoe UI",
                           "Noto Sans Arabic", "Geeza Pro", Roboto, Helvetica, Arial, sans-serif; }

      #right { flex: none; display: flex; align-items: center; gap: 11px; padding-left: 16px;
               border-left: 1px solid rgba(255, 255, 255, .16); }
      #mark { width: 34px; height: 23px; border-radius: 3px; background-color: rgba(255, 255, 255, .22);
              background-size: contain; background-position: center; background-repeat: no-repeat; }
      #where { text-align: left; }
      #site { font-size: 14px; font-weight: 600; line-height: 1.2; }
      #app  { font-size: 11px; font-weight: 500; line-height: 1.2; opacity: .62;
              text-transform: uppercase; letter-spacing: .07em; }

      #bar { position: absolute; left: 0; right: 0; bottom: 0; height: 3px;
             background: rgba(255, 255, 255, .14); }
      #bar > i { display: block; height: 100%; width: 0;
                 background: linear-gradient(90deg, rgba(255, 255, 255, .55), #fff);
                 transition: width 420ms cubic-bezier(.3, .7, .3, 1); }
    `;

    const CURSOR_SVG = `
      <svg viewBox="0 0 26 26" width="26" height="26" aria-hidden="true">
        <g class="hot" filter="url(#sh)">
          <path d="M3 2 L3 20.2 L7.9 15.6 L11.1 23.2 L14.4 21.8 L11.2 14.3 L18 14.1 Z"
                fill="#fff" stroke="#11161c" stroke-width="1.6" stroke-linejoin="round"/>
        </g>
        <filter id="sh" x="-40%" y="-40%" width="200%" height="200%">
          <feDropShadow dx="0.8" dy="1.4" stdDeviation="1.1" flood-color="#000" flood-opacity=".45"/>
        </filter>
      </svg>`;

    function build() {
        if (host && host.isConnected) return true;
        const parent = document.body;
        if (!parent) return false;              // an init script runs before the body exists

        host = document.createElement('div');
        host.setAttribute('data-elicit-recorder', '');
        // inset:0 rather than width/height so the page's own box model cannot reach it; ltr so the
        // panel keeps its layout on Arabia's right-to-left pages.
        host.style.cssText = 'position:fixed;inset:0;z-index:2147483647;pointer-events:none;' +
            'direction:ltr;margin:0;padding:0;border:0;';
        root = host.attachShadow({mode: 'closed'});
        root.innerHTML =
            '<style>' + CSS + '</style>' +
            '<div class="layer">' +
            '  <div id="focus"></div>' +
            '  <div id="cursor">' + CURSOR_SVG + '</div>' +
            '  <div id="third">' +
            '    <div id="chip"><span id="num"></span><span id="of"></span></div>' +
            '    <div id="mid"><div id="title"></div><div id="beat" dir="auto"></div></div>' +
            '    <div id="right"><div id="mark"></div>' +
            '      <div id="where"><div id="site"></div><div id="app"></div></div></div>' +
            '    <div id="bar"><i></i></div>' +
            '  </div>' +
            '</div>';
        parent.appendChild(host);

        cursor = root.getElementById('cursor');
        third = root.getElementById('third');
        numEl = root.getElementById('num');
        ofEl = root.getElementById('of');
        titleEl = root.getElementById('title');
        beatEl = root.getElementById('beat');
        markEl = root.getElementById('mark');
        siteEl = root.getElementById('site');
        appEl = root.getElementById('app');
        fillEl = root.querySelector('#bar > i');
        focusEl = root.getElementById('focus');

        restoreCursor();
        return true;
    }

    /**
     * Carry the pointer across a document load. Without this the cursor jumps back to the corner
     * every time the journey navigates, which reads as a glitch rather than as one continuous hand.
     * sessionStorage is per-origin, so a hop to Keycloak does start over -- the pointer fading in
     * on a different application is honest enough.
     */
    function restoreCursor() {
        let saved = null;
        try {
            saved = JSON.parse(sessionStorage.getItem(POSITION_KEY) || 'null');
        } catch (ignored) { /* private mode, or nothing stored */ }
        if (!saved) return;
        cx = saved.x;
        cy = saved.y;
        placed = true;
        cursor.style.transition = 'none';
        cursor.style.transform = 'translate3d(' + cx + 'px,' + cy + 'px,0)';
        cursor.classList.add('on');
        void cursor.offsetWidth;                // commit the jump before transitions come back
        cursor.style.transition = '';
    }

    function saveCursor() {
        try {
            sessionStorage.setItem(POSITION_KEY, JSON.stringify({x: cx, y: cy}));
        } catch (ignored) { /* nothing to do about it */ }
    }

    // ---- the pointer ---------------------------------------------------------------------------

    /** Animates the cursor to a point and answers how long, in ms, it will take to get there. */
    function moveTo(x, y) {
        if (!build()) return 0;
        const distance = placed ? Math.hypot(x - cx, y - cy) : 0;
        const travel = placed
            ? Math.min(TRAVEL_MAX, Math.max(TRAVEL_MIN, Math.round(distance / SPEED)))
            : 0;
        cx = x;
        cy = y;
        cursor.style.transitionDuration = travel + 'ms, 160ms';
        cursor.style.transform = 'translate3d(' + x + 'px,' + y + 'px,0)';
        cursor.classList.add('on');
        placed = true;
        saveCursor();
        return travel;
    }

    /**
     * The ring is drawn when the cursor arrives, not when the click happens: Playwright presses
     * the button the instant it has moved the mouse, so a ring drawn immediately would appear at
     * the far end of a pointer still in flight.
     */
    function ripple(x, y, after) {
        window.setTimeout(() => {
            if (!root) return;
            const ring = document.createElement('div');
            ring.className = 'ripple';
            ring.style.left = x + 'px';
            ring.style.top = y + 'px';
            ring.addEventListener('animationend', () => ring.remove());
            root.querySelector('.layer').appendChild(ring);
        }, after);
    }

    function showFocus(target) {
        if (!build() || !target || !target.getBoundingClientRect) return;
        const box = target.getBoundingClientRect();
        if (box.width < 8 || box.height < 8 || box.width > 900) return;
        focusEl.style.left = (box.left - 3) + 'px';
        focusEl.style.top = (box.top - 3) + 'px';
        focusEl.style.width = (box.width + 6) + 'px';
        focusEl.style.height = (box.height + 6) + 'px';
        focusEl.classList.add('on');
        window.clearTimeout(focusTimer);
        focusTimer = window.setTimeout(() => focusEl && focusEl.classList.remove('on'), 900);
    }

    /** Fields worth ringing. A `fill()` sets a value with no visible keystrokes at all. */
    function isField(node) {
        if (!node || !node.tagName) return false;
        const tag = node.tagName.toLowerCase();
        return tag === 'input' || tag === 'textarea' || tag === 'select'
            || tag.indexOf('vaadin-') === 0;
    }

    // ---- the lower third ----------------------------------------------------------------------

    function render() {
        if (!build()) return;
        const here = place();
        const total = state.total || 0;
        numEl.textContent = state.number ? String(state.number) : '–';
        ofEl.textContent = total ? '/ ' + total : '';
        titleEl.textContent = state.narration || state.phase || '';
        beatEl.textContent = state.beat || '';
        beatEl.style.display = state.beat ? '' : 'none';
        siteEl.textContent = here.site ? here.site.name : (here.app ? '' : '—');
        appEl.textContent = here.app || '';
        markEl.style.backgroundImage = here.site && here.site.mark ? 'url("' + here.site.mark + '")' : 'none';
        markEl.style.backgroundColor = here.site && here.site.color ? here.site.color : 'rgba(255,255,255,.22)';
        fillEl.style.width = total ? ((state.number / total) * 100).toFixed(2) + '%' : '0';
        third.classList.toggle('on', !!(state.narration || state.phase));
    }

    // ---- wiring -------------------------------------------------------------------------------

    // Capture phase, on window: the first listener to see the event, so nothing in the page can
    // stop the pointer from being drawn. Passive -- this must never change what the page does.
    const listen = (type, fn) => window.addEventListener(type, fn, {capture: true, passive: true});

    listen('mousemove', e => moveTo(e.clientX, e.clientY));
    listen('mousedown', e => {
        clicks++;
        const travel = moveTo(e.clientX, e.clientY);
        cursor.classList.add('down');
        ripple(e.clientX, e.clientY, travel);
    });
    listen('mouseup', () => cursor && cursor.classList.remove('down'));
    listen('wheel', e => moveTo(e.clientX, e.clientY));
    listen('focusin', e => { if (isField(e.target)) showFocus(e.target); });
    listen('scroll', () => focusEl && focusEl.classList.remove('on'));

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', render, {once: true});
    }
    // Self-healing, and how a Vaadin route change reaches the site/application badge: build() and
    // render() are both no-ops when nothing moved, and a view that replaced the body gets the
    // overlay back within half a second.
    window.setInterval(render, 500);
    render();

    window.__elicitRec = {
        update(next) {
            Object.assign(state, next || {});
            render();
        },
        /** For RecordingHarnessSelfTest: proof the overlay is attached and really saw the clicks. */
        debug() {
            return {attached: !!(host && host.isConnected), x: cx, y: cy, clicks: clicks,
                    narration: state.narration, beat: state.beat, place: place().app};
        }
    };
})();
