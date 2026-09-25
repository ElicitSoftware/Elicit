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
import { mkdir } from 'node:fs/promises';
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
const pause = (ms) => page.waitForTimeout(ms);

async function shot(name) {
  await pause(1000);
  await page.screenshot({ path: path.join(OUT, `${name}.png`) });
  console.log('  ✓', name);
}

/** A figure whose screen may not be reachable: warn and carry on rather than lose the run. */
async function tryShot(name, prepare) {
  try {
    if (prepare) await prepare();
    await shot(name);
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
    deviceScaleFactor: 2, // 2x, so the figures stay sharp in print
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
  // The blocking dialog, the "no survey" banner and the default-accounts banner are all on
  // this one screen, which is exactly what an installer meets first.
  await tryShot('05-first-sign-in');

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
  await tryShot('08-console-ready');

  // ------------------------------------------- Chapter 8: installing a survey definition
  console.log('Chapter 8 — installing a survey definition');
  await tryShot('09-apply-definition', async () => {
    await goto(`${ADMIN}/survey-apply`);
  });

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

  // The selector closed, not open. Opening it would put this build's blank Spanish and
  // Arabic labels into the manual; what the figure is for is that the selector APPEARS at
  // all, which it does only once a second language is on the mount.
  await tryShot('18-language-selector', async () => {
    await page.locator('#language-switcher').first().scrollIntoViewIfNeeded();
    await pause(1200);
  });

  await browser.close();

  console.log(`\nWrote figures to ${OUT}`);
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
