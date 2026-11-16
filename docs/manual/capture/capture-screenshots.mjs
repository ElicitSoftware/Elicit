/*
 * Umbrella UC-001 BR-007: every stage of the installation manual that is a screen carries a
 * screenshot of that screen.
 *
 * Drives a real Chromium against a freshly installed umbrella stack and writes the manual's
 * figures to docs/manual/images/. See docs/manual/README.md for how to run it.
 *
 * ORDER MATTERS. Figures 04 to 08 are first-run screens: they exist only on a database that
 * has never had a department or a survey. They are captured before the department is created
 * and before the definition is applied, and they cannot be retaken without another
 * `resetDatabase.sh V3`. Everything after 08 needs those two steps to have happened, so the
 * script does them itself, in order, in one pass.
 *
 * A step that cannot find its screen logs a warning and carries on, so one changed selector
 * costs one figure rather than the whole run. The exit status is non-zero if any figure was
 * missed, and FAILED.png is written at the point of an unhandled error.
 */
import { chromium } from 'playwright';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';

const ADMIN = process.env.ADMIN_URL || 'http://localhost:8081';
const SURVEY = process.env.SURVEY_URL || 'http://localhost:8080';
const KEYCLOAK = process.env.KEYCLOAK_URL || 'http://localhost:8180';
const USER = process.env.ADMIN_USER || 'admin';
const PASS = process.env.ADMIN_PASSWORD || 'admin';
const KC_USER = process.env.KEYCLOAK_ADMIN || 'admin';
const KC_PASS = process.env.KEYCLOAK_ADMIN_PASSWORD || 'admin';
const OUT = process.env.MANUAL_IMAGES || path.resolve(import.meta.dirname, '../images');
const DEFINITION =
  process.env.MANUAL_DEFINITION ||
  path.resolve(import.meta.dirname, '../../../FHHS/family-history-survey.elicit');

/** The department the manual's figures show being created. */
const DEPARTMENT = {
  name: 'Cancer Genetics',
  code: 'CG',
  email: 'cancer.genetics@example.org',
};

let page;
const missed = [];
const skipped = [];

/** A figure this deployment's state cannot produce; the committed one stays as it is. */
function skip(name, why) {
  skipped.push({ name, why });
  console.log(`  – ${name} — ${why}`);
}
const pause = (ms) => page.waitForTimeout(ms);

/*
 * #132: a figure ends a little below the last thing on its screen instead of carrying the empty
 * band under a short page, and a figure a procedure points into carries that procedure's step
 * numbers. `marks` is a list of [step number, locator]; their positions are written to
 * <name>.markers.tex beside the image, which the manual's \screenshot reads, so a recapture moves
 * the markers with the screen.
 */
const MIN_FIGURE_HEIGHT = 420;

/** The lowest visible text, control, image or ruled box: where the screen's content ends. */
async function contentBottom(fullPage) {
  return page.evaluate((whole) => {
    const limit = whole ? document.documentElement.scrollHeight : window.innerHeight;
    const media = /^(INPUT|TEXTAREA|BUTTON|IMG|SVG|CANVAS|VAADIN-BUTTON|VAADIN-ICON)$/i;
    let bottom = 0;
    const walk = (root) => {
      for (const el of root.querySelectorAll('*')) {
        if (el.shadowRoot) walk(el.shadowRoot);
        const r = el.getBoundingClientRect();
        const top = r.top + window.scrollY;
        // A container nearly as tall as the window is layout, not content.
        if (r.width < 2 || r.height < 2 || r.height > window.innerHeight * 0.6) continue;
        if (top >= limit || r.bottom + window.scrollY <= 0) continue;
        const st = getComputedStyle(el);
        if (st.visibility === 'hidden' || st.display === 'none' || Number(st.opacity) === 0) continue;
        const leaf = el.children.length === 0 || media.test(el.tagName);
        const drawn = el.textContent.trim() || media.test(el.tagName) || st.backgroundImage !== 'none'
          || st.borderBottomWidth !== '0px';
        if (leaf && drawn) bottom = Math.max(bottom, Math.min(r.bottom + window.scrollY, limit));
      }
    };
    walk(document);
    return bottom;
  }, fullPage);
}

/**
 * Where a step's disc goes: beside the control, on whichever side the screen is empty, so it
 * covers neither the control's label nor a neighbour's; on its corner when both sides are taken.
 * The disc is about 48 CSS pixels across at the manual's figure width.
 */
async function markerSpot(box) {
  return page.evaluate((b) => {
    const media = /^(INPUT|TEXTAREA|BUTTON|IMG|SVG|CANVAS|VAADIN-BUTTON|VAADIN-ICON|LABEL)$/i;
    const deepest = (x, y) => {
      let el = document.elementFromPoint(x, y);
      while (el && el.shadowRoot) {
        const inner = el.shadowRoot.elementFromPoint(x, y);
        if (!inner || inner === el) break;
        el = inner;
      }
      return el;
    };
    const occupied = (x, y) => {
      if (x < 4 || y < 4 || x > window.innerWidth - 4 || y > window.innerHeight - 4) return true;
      const el = deepest(x, y);
      if (!el) return false;
      return media.test(el.tagName) || (el.children.length === 0 && el.textContent.trim() !== '');
    };
    const free = (cx, cy) => [[0, 0], [-18, 0], [18, 0], [0, -14], [0, 14]]
      .every(([dx, dy]) => !occupied(cx + dx, cy + dy));
    const cy = b.y + b.height / 2;
    for (const [cx, cyy] of [[b.x - 30, cy], [b.x + b.width + 30, cy]]) {
      if (free(cx, cyy)) return [cx, cyy];
    }
    return [Math.max(12, b.x), Math.max(12, b.y)];
  }, box);
}

async function framedScreenshot(file, { fullPage = false, marks = [] } = {}) {
  const { width, height: viewportHeight } = page.viewportSize();
  const bottom = await contentBottom(fullPage);
  let height = Math.max(MIN_FIGURE_HEIGHT, Math.ceil(bottom + 28));
  if (!fullPage) height = Math.min(height, viewportHeight);
  await page.screenshot({ path: file, fullPage, clip: { x: 0, y: 0, width, height } });

  const markers = file.replace(/\.png$/, '.markers.tex');
  if (!marks.length) {
    await rm(markers, { force: true });
    return;
  }
  const scrollY = fullPage ? await page.evaluate(() => window.scrollY) : 0;
  const lines = ['% Written by capture-screenshots.mjs: the step markers on this figure (#132).'];
  for (const [step, locator] of marks) {
    const box = await locator.first().boundingBox().catch(() => null);
    if (!box) {
      console.warn(`    ! marker ${step}: its element is not on the screen`);
      continue;
    }
    const [vx, vy] = await markerSpot(box);
    const x = vx;
    const y = vy + scrollY;
    lines.push(`\\elicitmark{${step}}{${(x / width).toFixed(4)}}{${(y / height).toFixed(4)}}`);
  }
  await writeFile(markers, lines.join('\n') + '\n');
}

async function shot(name, options = {}) {
  await pause(1000);
  await framedScreenshot(path.join(OUT, `${name}.png`), options);
  console.log('  ✓', name);
}

/** A figure whose screen may not be reachable: warn and carry on rather than lose the run. */
async function tryShot(name, prepare, options) {
  try {
    if (prepare) await prepare();
    await shot(name, typeof options === 'function' ? await options() : options);
  } catch (err) {
    missed.push(name);
    console.warn(`  ! ${name} — ${err.message.split('\n')[0]}`);
  }
}

async function goto(url) {
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await pause(2500);
}

async function click(name, opts = {}) {
  await page.getByRole('button', { name, ...opts }).first().click();
  await pause(2500);
}

/** Vaadin 25 slots the label as a <label> child of the field host, and the department form
 *  mixes text fields with an email field, so match on the label across both tags. */
const FIELD_TAGS = ['vaadin-text-field', 'vaadin-email-field', 'vaadin-integer-field'];

async function fill(label, value) {
  const input = page
    .locator(FIELD_TAGS.map((t) => `${t}:has(> label:text-is("${label}")) input`).join(','))
    .first();
  // Vaadin's binder validates on focus and blur, and leaves the submit button disabled
  // until it has seen both. Setting the value alone is not enough.
  await input.click();
  await input.fill(value);
  await input.press('Tab');
  await pause(600);
}

async function main() {
  await mkdir(OUT, { recursive: true });
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1.25, // 1800 px across: sharp in print at the text width, and a small PDF
  });
  page = await context.newPage();

  // ------------------------------------------------------------ Chapter 4: the provider
  console.log('Chapter 4 — the identity provider');
  await goto(`${KEYCLOAK}/admin/master/console/`);
  if (await page.locator('#username').isVisible().catch(() => false)) {
    await page.fill('#username', KC_USER);
    await page.fill('#password', KC_PASS);
    await page.press('#password', 'Enter');
    await pause(6000);
  }

  await tryShot('01-keycloak-clients', async () => {
    await goto(`${KEYCLOAK}/admin/master/console/#/elicit/clients`);
    await pause(2000);
  });

  await tryShot('02-keycloak-client', async () => {
    await page.getByRole('link', { name: 'elicit-admin', exact: true }).first().click();
    await pause(3500);
  });

  await tryShot('03-keycloak-roles', async () => {
    await page.getByRole('tab', { name: /^Roles$/ }).first().click();
    await pause(3000);
  });

  // ------------------------------------------------ Chapter 7: the first sign-in (one-shot)
  console.log('Chapter 7 — the first sign-in (first-run screens)');
  await goto(ADMIN);
  await tryShot('04-sign-in');

  await page.fill('#username', USER);
  await page.fill('#password', PASS);
  await page.press('#password', 'Enter');
  await pause(7000);
  // Figures 05 to 07 exist only on a database that has never had a department: the blocking
  // dialog is what makes them. On an installed stack the same steps would quietly photograph
  // an ordinary console and put that in the manual, so decide which run this is first.
  const firstRun = await page
    .getByRole('button', { name: 'Add a department' })
    .isVisible()
    .catch(() => false);

  if (firstRun) {
    // The blocking dialog, the "no survey" banner and the default-accounts banner are all on
    // this one screen, which is exactly what an installer meets first.
    // Step 2 of the first sign-in: the dialog's link to Departments.
    await tryShot('05-first-sign-in', null, () => ({
      marks: [[2, page.getByRole('button', { name: 'Add a department' })]],
    }));

    await tryShot('06-departments-empty', async () => {
      await click('Add a department');
    });

    await tryShot('07-new-department', async () => {
      await click('New Department');
    });

    // Creating it assigns the department to its creator and unblocks the console. Until this
    // succeeds the dialog is modal over every route, so it intercepts every later click and
    // nothing after this point can be captured.
    await fill('Department name', DEPARTMENT.name);
    await fill('Department code', DEPARTMENT.code);
    await fill('From email', DEPARTMENT.email);
    await click('Create department');
    await pause(5000);

    await goto(ADMIN);
    if (await page.getByRole('button', { name: 'Add a department' }).isVisible().catch(() => false)) {
      throw new Error(
        'the department was not created — the blocking dialog is still up, so no ' +
          'later figure would be usable. Check the fields on the department form.'
      );
    }
  } else {
    const why = 'this deployment already has a department; the screen exists only before the first one'
      + ' — run resetDatabase.sh V3 first';
    for (const name of ['05-first-sign-in', '06-departments-empty', '07-new-department']) {
      skip(name, why);
    }
  }
  await tryShot('08-console-ready');

  // ------------------------------------------- Chapter 8: installing a survey definition
  console.log('Chapter 8 — installing a survey definition');
  // Steps 1 and 2 of installing a definition: the navigation entry, and the upload.
  await tryShot('09-apply-definition', async () => {
    await goto(`${ADMIN}/survey-apply`);
  }, () => ({
    marks: [
      [1, page.getByRole('link', { name: 'Apply Survey Definition', exact: true })],
      [2, page.locator('vaadin-upload vaadin-button, vaadin-upload [slot="add-button"]')],
    ],
  }));

  await tryShot('10-apply-result', async () => {
    // A vaadin-upload: setting the file input starts the upload on its own.
    await page.locator('input[type="file"]').first().setInputFiles(DEFINITION);
    await pause(15000);
  });

  // ------------------------------------------------- Chapter 12: the System section
  console.log('Chapter 12 — the System section');
  for (const [name, route] of [
    ['11-system-overview', '/system'],
    ['12-system-database', '/system/database'],
    ['13-system-branding', '/system/branding'],
    ['14-system-email', '/system/email'],
    ['15-system-connections', '/system/connections'],
    ['16-system-oidc', '/oidc'],
  ]) {
    await tryShot(name, async () => {
      await goto(`${ADMIN}${route}`);
      await pause(2500);
    });
  }

  // ------------------------------------- Chapters 10 and 11: branding and translations
  console.log('Chapters 10 and 11 — branding and translations');
  await tryShot('17-branded-header', async () => {
    await goto(SURVEY);
  });

  // Open, so the figure shows the languages this site offers rather than repeating figure 17.
  // The overlay's items render after it opens; at 900 ms the figure caught them blank.
  await tryShot('18-language-selector', async () => {
    const selector = page.locator('#language-switcher').first();
    if (!(await selector.isVisible().catch(() => false))) {
      throw new Error(
        'the selector is not in the header, so only one language is offered — mount ' +
          'elicit-i18n on Survey (and unset i18n.offered.locales) and re-run'
      );
    }
    await selector.click();
    await pause(2500);
  });

  // Escape, never a selection: choosing a language would reload Survey in it.
  await page.keyboard.press('Escape').catch(() => {});

  await browser.close();

  console.log(`\nWrote figures to ${OUT}`);
  if (skipped.length) {
    console.log(`\n${skipped.length} figure(s) this deployment cannot produce — the committed ones stand:`);
    for (const { name, why } of skipped) console.log(`  ${name} — ${why}`);
  }
  if (missed.length) {
    console.error(`\n${missed.length} figure(s) not captured: ${missed.join(', ')}`);
    process.exit(1);
  }
}

main().catch(async (err) => {
  console.error(err);
  if (page) await page.screenshot({ path: path.join(OUT, 'FAILED.png') }).catch(() => {});
  process.exit(1);
});
