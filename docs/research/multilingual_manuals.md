# Manuals in Every Language a Release Offers: Typesetting, Figures and Delivery

> **Status (2026-09-29):** Research. The platform ships its interface text in three languages and
> every one of its three manuals in one: an administrator who reads the console in Spanish opens an
> English PDF. The obstacle is not the prose — it is that nothing in the manual pipeline has a
> language dimension at all, from the single `JOB` name in `build-manual.sh` through a design
> package that hard-codes its own English furniture to a capture harness that drives the console by
> its English button labels. Three directions were decided while writing it: **the scope is the
> installation manual and the two in-application manuals, Survey having none; the figures localize
> the Elicit screens only, leaving the six Keycloak screens in English; and the prose is machine
> drafted and human reviewed, which is already how the interface bundles are produced.** Whether
> the bundled Keycloak realm turns internationalization on (section 4.5), and what the unit of
> extraction for translatable prose is (section 5.2), are deliberately not decided. Nothing here is
> implemented.
>
> This document also records two findings that are prerequisites rather than consequences: a
> released image contains no manual at all (section 6.4), and Admin's own UC-029 carries a business
> rule stating that the manual *is* published in English (section 1.4).

## Overview

Elicit offers three languages — English, Latin American Spanish (`es-419`) and Arabic (`ar`) —
declared in `i18n.bundled.locales` in each module's `application.properties` (Survey `:21`, Admin
`:140`, Author `:124`) and held to one set by `checkLanguages.sh`. A reader chooses among them per
session, and the choice reaches the page as a locale, a text direction and a font scale. None of
that reaches a manual. All three manuals are LuaLaTeX documents typeset from committed `.tex`
sources and committed PNG screenshots, and each pipeline is monolingual by construction: one output
filename, one figure directory, one shared LaTeX working directory, one set of captures taken in
whatever locale the capturing machine's Chromium defaults to.

The three manuals are not delivered the same way, and that difference decides how far localization
can go for each. The installation manual is read before any Elicit service exists, so no
application can pick its language; the two in-application manuals are served by an application that
already knows the reader's language and does not consult it.

Terminology: an **edition** is one manual in one language — sources, figures and PDF; **chrome** is
the text a manual's design contributes (call-out titles, table headings, running heads, the footer),
as distinct from the prose an author wrote; an **LTR island** is a run of left-to-right technical
text — a setting name, a path, a command — set inside a right-to-left paragraph.

## 1. What a translated manual has to be

### 1.1 The three manuals, and who reads each

| Manual | Source | Reader | Delivery |
|---|---|---|---|
| Installation manual | `docs/manual/elicit-installation-manual.tex` (umbrella UC-001, UC-002) | deployment operator | a loose PDF release artifact of this repository |
| Administrator's manual | `Admin/docs/manual/elicit-admin-manual.tex` (Admin UC-029) | console administrator | classpath `manual/elicit-admin-manual.pdf`, served role-gated at `/api/manual` |
| Author's manual | `Author/docs/manual/elicit-author-manual.tex` (Author UC-039) | survey author | the same shape, in a private repository |

Survey has no manual: there is no `manual` package, no `manual/` resource directory and no
`/api/manual` route in it. Survey is therefore out of scope, and the phrase "every manual" in this
document means these three.

### 1.2 Why delivery decides how far localization goes

The installation manual cannot be served by an application. Umbrella FR-003 asks for "a standalone
PDF that needs nothing running", and C-001 keeps it out of every image, because an operator reads
it before there is an Elicit site to serve it from (UC-001 BR-001). Its editions are therefore
chosen by a human on a release page, which makes the delivery question trivial and the *naming*
question load-bearing: three files that differ only in a language tag, published together.

The two in-application manuals are the opposite. The reader's language is already known
server-side, at both moments that matter — when the link is rendered and when the PDF is streamed
(section 6.2) — so their editions can be selected without asking. That is the case the request
names: an administrator using Admin in Spanish should get the Spanish manual.

### 1.3 What each edition has to carry

Each edition is a separate artifact and needs to be attributable as one:

1. the platform version and build date it was stamped with, which `build-info.tex` already supplies
   (`docs/manual/build-manual.sh:79-83`);
2. the language tag, in the filename and on the title page;
3. who reviewed the translation and when — the interface bundles already require this, as a
   `# Reviewed by <name or agent>, <date>` first line (`Survey/i18n/TRANSLATION_REQUEST.md`, rule 7);
4. which English revision the translation was made from, so a stale edition is detectable rather
   than merely suspected (section 5.4).

Items 3 and 4 have no counterpart in the manual pipeline today.

### 1.4 The business rule this contradicts (decided: revise it)

`Admin/docs/use_cases/UC-029-consult-the-administrators-manual.md:140-142` states the opposite
position as a rule:

> ### BR-009: The manual is published in English
>
> The console's chrome is translated (UC-026) and the entry that opens the manual is translated with
> it, but the manual itself is one English document. It describes the language selector rather than
> being reissued per language, so that a single build produces a single attributable manual.

The rule is not an oversight, and the manual is written to it:
`Admin/docs/manual/elicit-admin-manual.tex:608-627` is a chapter called "Reading the console in your
language" which describes the selector. Author's UC-039 says nothing about language either way.

BR-009's stated reason is attributability — one build, one manual. That reason is answerable rather
than fatal: the four items in section 1.3 make each edition individually attributable, which is
strictly more than the single English document carries today. What revising the rule costs is the
honesty clause it implies. A site running three languages would hold three manuals per application
of which at most one has been reviewed most recently, so the rule that replaces BR-009 has to say
what an unreviewed or stale edition does — and the only answer consistent with umbrella UC-015
BR-001, *English is always available and is the last fallback*, is that it falls back to English
rather than shipping a draft (section 5.4).

The chapter at `elicit-admin-manual.tex:608-627` survives either way. A reader still needs to know
where the selector is; what changes is that the manual around it is in the language the selector
was used to choose.

## 2. What the pipeline does today

### 2.1 One script, one output, one working directory

`docs/manual/build-manual.sh` runs a single container and copies a single file out:

```sh
docker run --rm -v "${MANUAL_DIR}:/manual" -w /manual -u "$(id -u):$(id -g)" -e HOME=/tmp \
  "${TEXLIVE_IMAGE}" \
  latexmk -lualatex -interaction=nonstopmode -halt-on-error -file-line-error \
          -outdir=build "${JOB}.tex"
```

(`:86-93`.) Every name in it is a constant: `JOB="elicit-installation-manual"` and
`PDF_NAME="${JOB}.pdf"` at `:28-29`, `-outdir=build` in the command, and the stamp written to a
fixed `build-info.tex` at `:79-83`. The engine is **LuaLaTeX** through `latexmk`, so the pass count
is not fixed, and the image is `texlive/texlive:latest` (`:26`), which is where IBM Plex and the
LaTeX packages come from. The version is taken from `Survey/pom.xml` and `Admin/pom.xml` and used
only when they agree, stamping `unknown` otherwise rather than picking a side (`:43-67`, UC-002
BR-003).

Admin and Author run the same script under a different name, with one consequential difference: the
output goes straight onto the classpath. `Admin/docs/manual/build-manual.sh:22-24` sets
`MANUAL_OUT` to `src/main/resources/manual`, `JOB` to `elicit-admin-manual` and `PDF_NAME` to
`elicit-admin-manual.pdf`, and `:67-70` copies the PDF there; `Admin/buildDockerImage.sh:12` runs
it before packaging. The packaged PDF is gitignored (`Admin/.gitignore:62`), so it is a build
artifact and not a committed one.

### 2.2 The documents

All three are monolithic. `elicit-installation-manual.tex` is 1,280 lines with **zero** `\input` or
`\include`; the only `\input` in the tree is `build-info.tex`, pulled in by the design package
(`elicit-brand.sty:86`). Admin's is 649 lines, Author's 392. There is nothing to swap per language
and nothing to split along.

Sizes, measured by stripping comments, collapsing every `\opt`/`\val`/`\file`/`\optlit`/`\url`
argument to one placeholder, deleting the remaining commands and braces, and subtracting the
placeholders (`sed` + `wc -w`; see section 5.1 for the caveats):

| Manual | Lines | Figures | `\ui{}` uses | Net translatable words |
|---|---|---|---|---|
| Installation | 1,280 | 18 | 25 | ≈7,300 |
| Administrator's | 649 | 29 | 151 | ≈3,700 |
| Author's | 392 | 19 | 52 | ≈2,350 |
| **Total** | | **66** | **228** | **≈13,350** |

So one additional language is about 13,350 words of prose and 66 figures; the two languages the
release already offers are about 26,700 words and 132 figures, less the six third-party screens
held back by the figure decision (section 4.4).

### 2.3 The build target

`Manual` is one of five parallel targets in `buildDockerImages.sh`:

```sh
ALL_MODULES=(Survey FHHS Pedigree Admin Manual)
```

(`:57`), mapped to a directory and a script list by two `case` statements at `:65-73`, and run in a
background subshell at `:164`. Two mechanical details constrain any per-language design.
`build_scripts()` emits a word-split list consumed by `for script in $(build_scripts "$m")`, so no
argument containing a space can be passed through it. And the port-warning predicate at `:88-89`
tests target names against the literal `Manual`, so a new target name is silently treated as a
module. `checkLanguages.sh` already runs unconditionally before every target, including a
manual-only run, explicitly "because the manual documents these settings" (`:103-111`) — which
makes it the natural place to source the language list.

### 2.4 The gate

`docs/manual/check-properties.sh` holds the configuration reference against the code (umbrella
NFR-004, NFR-005) and gates the typeset. It reads the manual by parsing it: `documented =
set(re.findall(r'\\opt\{([^}]*)\}', tex))` at `:127`, with documented defaults taken from the first
`\val{}` in the second cell of each row inside a `\begin{settings}` block (`:145-156`). The `.tex`
path is a constant at `:25`.

### 2.5 The capture harness

`docs/manual/capture/capture-screenshots.mjs` drives a real Chromium through Playwright against a
freshly installed stack and writes `docs/manual/images/NN-name.png` at 1440×900 with
`deviceScaleFactor: 2` (`:97-102`). It signs into Keycloak's master console and into Admin by raw
DOM ids, applies `FHHS/family-history-survey.elicit`, walks the System pages, and finishes on
Survey's branded header and language selector. Admin's own harness is the same design at larger
scale, with a `FIGURES` allow-list that refuses any name not in it. Output is parameterized by
`MANUAL_IMAGES`; nothing else is.

## 3. Typesetting a second language, and a right-to-left one

### 3.1 No document language is declared

The preamble is six lines (`elicit-installation-manual.tex:9-27`) and declares no language at all:
no `babel`, no `polyglossia`, no `bidi` or `luabidi`. `\usepackage[T1]{fontenc}` at `:11` is a
vestigial no-op under LuaLaTeX. The consequence is not only that Arabic cannot be set — it is that
LaTeX's own generated words are frozen in English, so `\tablename` renders "Table" and
`\contentsname` is never consulted at all because the contents heading is a literal (section 3.3).
Declaring a language per edition is what makes those correct for free, and under LuaLaTeX the
ordinary route for Arabic is `babel` with `bidi=basic` or `polyglossia` with `\setmainlanguage{arabic}`.

### 3.2 No Arabic face, and a guard that hides its absence

`elicit-brand.sty` loads `fontspec` (`:23`) and then exactly two families, IBM Plex Sans and IBM
Plex Mono, each inside a conditional:

```latex
\IfFontExistsTF{IBMPlexSans}{% ... }{}
\IfFontExistsTF{IBMPlexMono}{% ... }{}
```

(`:65`, `:74`.) Neither family covers Arabic. The guard is deliberate — the comment above it says a
TeX installation without IBM Plex "still builds; it just falls back to the default families rather
than failing the whole manual" — but it is exactly the wrong behavior for a script the build cannot
render: a missing Arabic face would produce a silently substituted or blank PDF rather than an
error, and 66 figures of correct Arabic screenshots wrapped in unrenderable Arabic prose is a
worse artifact than a failed build.

Worth recording as a contrast: the web interface deliberately ships **no** per-locale font family,
because "a family is only useful with a face the reader's device actually has, and there is nothing
to ship one from" (`docs/I18N_IMPLEMENTATION_GUIDE.md:145-147`), so Arabic renders in the system
stack and is compensated with a font scale instead. A PDF is not in that position. TeX Live carries
fonts, so an Arabic edition can embed a face and the web interface's constraint does not transfer.
Which face the image actually carries is the first experiment in section 8.

### 3.3 The design package holds its own English

A translator handed only the `.tex` would ship all of the following in English, because they live
in `elicit-brand.sty`:

- the call-out titles, as default optional arguments: `[Note]` (`:147`), `[Careful]` (`:158`),
  `[Before this reaches a real site]` (`:171`);
- the settings-table headings `Setting` / `Default` / `Change` / `What it does`, twice over because
  `\endfirsthead` and `\endhead` repeat them (`:233-237`, `:241-245`);
- the Change column's three values: `set`, `may`, `keep` (`:218-220`);
- the continuation furniture: `\tablename~\thetable\ (continued)` (`:240`, `:266`) and `continued
  overleaf` (`:249`);
- the word `built` in both page footers (`:99`, `:106`).

And in the document itself:

- `\section*{Contents}` as a literal rather than `\contentsname` (`.tex:70`);
- the whole title page — "Installing the Platform", "The Elicit installation manual", "Version",
  "Built", and the license paragraph (`.tex:41-62`);
- 14 `\markboth` running heads, each repeating its section title verbatim;
- 40 hand-typed cross-reference words: `Chapter~` ×25, `Section~` ×11, `Table~` ×4.

The 25 call-out titles in the document *are* translatable, because every call-out passes an
explicit English title rather than relying on the default — only the three defaults are hidden.

### 3.4 The page furniture is committed to the left

None of this mirrors on its own under a right-to-left main language:

- the settings table's column specification is absolute, four `>{\raggedright\arraybackslash}p{…}`
  columns in a fixed order (`elicit-brand.sty:228-231`), and the continuation rows are pinned with
  `\multicolumn{4}{@{}l}` and `\multicolumn{4}{@{}r}` (`:240`, `:249`); `plaintable` takes its
  column specification from the caller (`:259`), so every call site is affected too;
- all three call-out boxes put their accent rule on the left, `leftrule = 3pt` (`:150`, `:161`,
  `:174`), and all three verbatim environments use `frame = leftline` with `xleftmargin = 12pt`
  (`:282`, `:293`, `:304`);
- the page furniture is side-addressed: `\fancyhead[L]` and `\fancyfoot[L]`/`[R]` (`:98-100`,
  `:106-107`);
- list indents are `leftmargin` (`:317-319`).

An Arabic edition needs the rules and the indents on the inner edge and the table columns reversed.
`babel`'s bidi support mirrors much of this; the hand-set `[L]`/`[R]` slots and the literal
`@{}l`/`@{}r` alignments it will not.

### 3.5 LTR islands inside RTL paragraphs

The installation manual carries 371 inline technical atoms — `\val` ×203, `\opt` ×127, `\file` ×29,
`\optlit` ×12 — plus ten verbatim blocks (four `elicitcmd`, four `elicitfile`, two `elicitsql`).
All of it is left-to-right content, and the first three are built on `\DeclareUrlCommand`
(`elicit-brand.sty:195-197`) with a custom `\UrlBreaks` list (`:192`) so a long dotted key can
break inside a narrow column. Set inside an Arabic paragraph without explicit isolation, the
punctuation and parentheses around such a run reorder, and a setting name printed with its
surrounding bracket on the wrong side is a setting name the reader cannot copy. NFR-007 — text
copied out of the PDF runs as printed — makes this a correctness requirement in an Arabic edition,
not a typographic preference.

### 3.6 The design package is three divergent copies (decided: converge first)

`elicit-brand.sty` is described in its own header as the shared design, "the same design the other
Elicit manuals are typeset in (umbrella C-003: they read as one set)". It is not one file. The
umbrella copy differs from Admin's and from Author's by 202 lines each, and Admin's differs from
Author's by 12. The drift is not cosmetic: Admin's footer hard-codes the product name,
`Elicit Admin \elicitVersion` (`Admin/docs/manual/elicit-brand.sty:70`, `:77`), where the umbrella
copy reads `\elicitProduct`; Admin's `\screenshot` takes the filename **with** its extension and
emits `\caption{#2}` where the umbrella's takes a bare basename and emits `\caption[]{#2}`
(`Admin/docs/manual/elicit-brand.sty:102-110` against `docs/manual/elicit-brand.sty:131-144`).

Everything in sections 3.1 to 3.5 would otherwise have to be done three times, divergently. The
localization of the chrome is the first change that makes converging these copies cheaper than not.

## 4. Figures in the language of the manual

### 4.1 What the figures are

The installation manual's inventory, by class:

| Class | Count | Files |
|---|---|---|
| Elicit interface, renders differently per language | 14 | `05`–`18` less the Keycloak set; 13 Admin views, 1 Survey |
| Keycloak interface | 4 | `01`, `02`, `03` (admin console), `04` (login page) |
| Diagram or illustration with embedded English | 0 | — |
| Language-neutral | 1 | `elicit-logo.png` |

Across all three manuals: 66 figures, of which 60 are Elicit's own interface and 6 are Keycloak
(`02-sign-in` in Admin's set, `01-sign-in` in Author's). There are no TikZ or PGF diagrams anywhere
— `grep` for `tikz|pgf|\\draw|\\node` returns nothing — so no illustration has to be redrawn. The
one piece of embedded English outside a caption is a hand-aligned directory tree inside an
`elicitfile` block (`.tex:920`) whose right-hand column is ~20 words of annotation; translating it
breaks the alignment the spaces provide.

Two structural facts make translated editions unusually cheap on the document side. No figure
carries a `\label` — `\screenshot` emits `\caption[]{#2}` and nothing else — and no `\ref` in the
document points at a figure; all 40 references are to sections and tables. So an edition may drop,
reorder or substitute figures with no dangling reference. And `\caption[]`'s empty optional argument
is load-bearing (it keeps the caption out of the list of figures so `\opt`/`\val`/`\file` survive),
which is a thing a translator must be told not to remove.

### 4.2 The harness has no language, and its selectors are English

The capture context sets a viewport and a device scale factor and nothing else (`:97-102`): no
`locale`, no `timezoneId`, no `Accept-Language`, and no `?lang=` anywhere in the file. Every figure
is captured in Chromium's default locale. The mechanism to fix that already exists and the manual
documents it — `?lang=` is honored on any route by `LocaleInitializer`, which reads it in a
`BeforeEnterListener` and applies it through `LocaleSelection` — so the missing piece is the call,
not the capability.

The harder problem is that the harness drives the console **by its English labels**:

```js
getByRole('button', { name: 'Add a department' })      // :142
await click('New Department');                          // :156
await click('Create department');                       // :165
await fill('Department name', DEPARTMENT.name);         // :162
```

and `fill()` matches on the visible `<label>` text of a Vaadin field (`:79-93`). Against a Spanish
console every one of those misses. `tryShot` turns a miss into a warning rather than a failure
(`:59-67`), so a naive Spanish run would not crash — it would quietly produce a short set and a
non-zero exit. The single locale-proof selector in the file is `#language-switcher` (`:221`), which
is also the pattern to follow: the screens the manual photographs need stable ids.

### 4.3 Three figures are one-shot per language

Figures `05-first-sign-in`, `06-departments-empty` and `07-new-department` exist only on a database
that has never had a department; the harness captures them before it creates one, and skips them
with a reason on a stack that already has one (`:141-181`). The git history is the evidence:
`84591a9` skipped them rather than photographing an ordinary console, and `fa3a0a3` retook `05`–`18`
after a greenfield reset. So each language needs its own `resetDatabase.sh V3` pass, and the three
recaptures cannot be retried within a language without another one.

Recapture is a bulk operation in this repository — four commits have ever touched
`docs/manual/images/`, each a re-run of the harness with the commit message naming the subset — so
the per-language unit of work is a whole run, not a figure.

One coordination hazard to carry into any per-language run: the `NN-` filename prefixes no longer
match the rendered figure order. `17-branded-header` renders as figure 11, `13-system-branding` as
12, `18-language-selector` as 13, and `11`, `12`, `14`, `15`, `16` render as 14 through 18. Figures
must be identified by filename prefix and never by rendered number.

### 4.4 Arabic figures differ in geometry, not only in glyphs

`LocaleLayout.apply` sets the UI direction, the `dir` and `lang` attributes and a
`data-font-scale` attribute, and pushes `--elicit-font-scale` onto the document element. The shipped
manifest `META-INF/i18n/i18n-config.json` declares `ar` as `rtl` at `fontScale` 1.15, and each
app's `styles.css` turns that into `html { font-size: calc(100% * var(--elicit-font-scale, 1)); }`
(Admin `:113`, Survey `:53`, Author `:55`). So an Arabic capture is a mirrored layout at a larger
root size: line wrapping, control widths and how much of a page fits in 1440×900 all change. The
Arabic figures are not the Spanish figures with different glyphs, and a caption that describes where
something sits on the screen may need rewriting rather than translating.

### 4.5 Keycloak stays English (decided), and why it has to (not decided: whether to change that)

The four Keycloak figures divide into two cases. The three admin-console figures follow the *admin
user's* profile locale, not the realm's, so recapturing them means switching that account's
language. The login page, `04-sign-in`, cannot be recaptured at all as shipped: the bundled realm
turns internationalization off.

```json
  "internationalizationEnabled" : false,
  "supportedLocales" : [ ],
```

(`keycloak/elicit-realm.json:1550-1551`.) That is a two-line change to the realm, and Keycloak
ships both `es` and `ar` login themes, so the capability is there. It is deliberately not taken
here: the decision recorded in the status block is to localize Elicit's own screens only, and the
captions on those six figures should say plainly that the identity provider's screens are shown in
English. But the realm flag is worth writing down, because `04-sign-in` is the first screen the
reader meets and a Spanish reader meeting it in English is the most visible seam in the edition.
Turning it on also affects a running site, not just a screenshot, which is why it belongs in a
decision of its own rather than in a figure decision.

### 4.6 The figure path is baked into the macro

`\screenshot` writes the directory itself:

```latex
\fbox{\includegraphics[width=\dimexpr\linewidth-2\fboxrule\relax]{images/#1}}
```

(`elicit-brand.sty:139`, with `\graphicspath{{./}}` at `.tex:27`.) So per-language figures cannot be
addressed from the call sites; either `\graphicspath` becomes per-edition or the macro gains a
directory hook. The capture end already cooperates: `MANUAL_IMAGES` overrides the output directory
(`capture-screenshots.mjs:29`).

## 5. Where the prose comes from (decided: machine draft, human review)

### 5.1 The size of the job, and how it was measured

The word counts in section 2.2 were produced by stripping `%` comments, replacing every
`\opt{…}`, `\val{…}`, `\file{…}`, `\optlit{…}` and `\url{…}` with a single placeholder, deleting
the remaining `\command` names with their optional arguments, deleting braces and `&`, counting with
`wc -w`, and subtracting the placeholders. Deleting braces rather than replacing them with a space
matters: replacing inflates the count by splitting words at markup boundaries.

For the installation manual the result is ≈7,300 net words, of which 399 are the 18 figure captions
and about 1,800 are table cells — and the settings tables are more prose than they look, because
their "What it does" column carries most of those words while columns one and two are identifiers.
Excluded from the total and untranslatable: 369 inline config placeholders and about 300 words of
verbatim commands, DDL and JSON.

### 5.2 The unit of extraction (not decided)

The interface bundles are a key-value file, so a translator receives a table of keys and returns a
table of keys. A `.tex` is not. Two shapes are available and neither is obviously right:

1. **Translate the document.** The translator receives `elicit-installation-manual.tex` and returns
   `elicit-installation-manual.es-419.tex`, preserving markup. Cheapest to set up, and it lets a
   translator see a sentence in context, which matters for a manual far more than for a button
   label. It puts the whole document's markup in the translator's hands, including the four-column
   settings rows and `\caption[]`'s empty argument, both of which the property gate depends on
   (section 5.5).
2. **Extract the prose.** A generated request document lists translatable runs with their context,
   the way `TRANSLATION_REQUEST.md` lists keys, and a reassembly step puts them back. Protects the
   markup absolutely, and makes staleness detectable per run rather than per document, at the cost
   of building the extractor and of translating sentences shorn of their surroundings.

The deciding question is who does the review. If the reviewer is the same person who reviews the
interface bundles, shape 2 matches what they already do; if it is a technical translator working in
the document, shape 1 does.

### 5.3 The precedent to copy

Whatever the unit, the machinery already exists one layer down and should be mirrored rather than
reinvented:

- `src/main/resources/vaadin-i18n/translations.context.properties` is a per-key context sidecar —
  `<view or place> | <component> | <max characters> | <flags>` — read by the request generator and
  checked by a test;
- `i18n/TRANSLATION_REQUEST.md` is **generated**, by
  `src/test/java/.../i18n/TranslationRequestGenerator.java`, and a test fails on drift with the
  exact `cp` that fixes it;
- the returned file must carry `# Reviewed by <name or agent>, <date>` as its first line;
- and the drafts are already machine-made and marked as such: `Survey/i18n/translations_ar.properties:1`
  reads `# DRAFT - machine translated, pending review by a native speaker. Arabic (ar), right-to-left.`

So the decision recorded in the status block — machine draft, human review — is not a new policy
for this platform. It is the existing policy, applied to a document instead of a bundle.

### 5.4 Staleness, and what a stale edition does

A manual edition can be stale in a way a bundle cannot: the English document changes continuously
during a release cycle, and a figure can go out of date without a word changing. Three things need
deciding together, and the answer to the third is already fixed:

1. **How staleness is detected.** The cheapest honest mechanism is to record, in each edition, the
   git revision of the English source it was translated from, and to fail or warn when the English
   source has moved. This is the same idea as the request generator's drift test, applied across
   languages instead of across a regeneration.
2. **Whether it blocks.** A gate that fails the build when any edition is stale makes English
   changes expensive mid-cycle; a gate that warns lets a stale Spanish manual ship.
3. **What the reader gets.** Umbrella UC-015 BR-001 — English is always available and is the last
   fallback — settles this: a missing or withheld edition falls back to English, exactly as a
   missing translation key does. A stale edition should be withheld rather than served, which turns
   question 2 into a release-management choice rather than a correctness one.

### 5.5 The two gates the translation touches

`check-properties.sh` is bound to English twice. Its `.tex` path is a constant (`:25`), so adding
`elicit-installation-manual.es-419.tex` would leave the gate checking English and silently not
covering the edition. And its skip list is keyed on English sentinel values at `:179` — `"per app"`,
`"build"`, `"---"`, `"empty"`, `"as above"` — so translating those cells produces spurious
mismatches. The gate validates content against code rather than presentation, so the right answer
is probably to keep running it against English only and to give the sentinels a language-neutral
macro; either way the coupling must be recorded rather than discovered.

The gate that does not exist is the more valuable one. The installation manual names 13 distinct
interface labels in 25 `\ui{}` uses — `Admin`, `Apply Survey Definition`, `Branding`, `Default
message ID`, `Departments`, `FHHS`, `Languages`, `Monitor`, `Pedigree`, `Send test email`, `Survey`,
`System`, `Users` — and Admin's manual uses `\ui{}` 151 times, Author's 52. In a translated edition
every one of those must equal the value the application's own `translations_<tag>.properties` gives
for that label, or the manual names a button the reader cannot find. That is mechanically checkable
against the bundles the module already ships, and it is the one check that makes a translated manual
verifiably usable rather than merely translated.

## 6. Delivery

### 6.1 The installation manual

N PDFs, named per language tag, published with the release. `.gitignore:34` already reads
`/docs/manual/*.pdf`, so additional editions need no change there, and `/docs/manual/build/` covers
a per-language subdirectory. The whole of the delivery problem here is that `JOB` and `PDF_NAME` are
constants and `-outdir=build` is shared, so two editions built in sequence overwrite each other's
working files and two built in parallel corrupt them — along with `build-info.tex`, which both would
rewrite.

### 6.2 The two in-application manuals

The reader's language is available at both points that matter.

At link-render time, `Admin/src/main/java/com/elicitsoftware/admin/flow/MainLayout.java` builds a
header anchor and a drawer item to `MANUAL_PATH = "/api/manual"` (`:135`, `:260-272`, `:280-290`),
both gated on `manualAvailable()` (`:293-295`), and `UI.getCurrent().getLocale()` is the current
language at that moment. At serve time, `ManualResource` streams the PDF (`:39`, `:54-67`) with
`@RolesAllowed({ElicitRoles.ADMIN, ElicitRoles.USER})`, and `AdminManual` resolves exactly one
classpath path — `DEFAULT_RESOURCE = "manual/elicit-admin-manual.pdf"` (`:36`), overridable through
`@ConfigProperty(name = "admin.manual.resource", …)` (`:42-43`). Nothing in either class consults a
locale.

Two existing patterns cover the gap. `admin.manual.resource` and `author.manual.resource` already
redirect which classpath resource is served, today only so a test can simulate a manual-less build —
which is exactly the seam a per-language resource name needs. And the negotiation idiom is already
written elsewhere in the platform:
`Survey/src/main/java/com/elicitsoftware/report/PDFDownloadResource.java:71-100` derives a `Locale`
from `headers.getAcceptableLanguages()`.

### 6.3 Session locale or `Accept-Language` (not decided)

These disagree, and the disagreement is visible. The chosen language lives only in the Vaadin
session — `LocaleSelection` writes it to the `elicit.locale` session attribute and its javadoc says
"The choice lives only in the Vaadin session; nothing about it is stored with the respondent" —
while `/api/manual` is opened in a **new browser tab** (`setTarget(AnchorTarget.BLANK)` at
`MainLayout.java:267`, `setOpenInNewBrowserTab(true)` at `:288`). A request from that tab carries
the session cookie, so the session attribute is reachable; but if the resource negotiated on
`Accept-Language` alone, an administrator who switched the console to Spanish on an English-preferring
browser would get an English manual from a Spanish console. The session choice is the reader's
explicit act and should win, with `Accept-Language` as the fallback for a request that has no
session locale — but this needs deciding explicitly, because the naive reuse of the
`PDFDownloadResource` idiom gets it backwards.

Cost to record: three editions of a ~5 MB PDF on the classpath of each image. The installation
manual's 5.2 MB is almost entirely its 5.0 MB of figures.

### 6.4 The release builds no manual at all

Admin's PDF reaches the classpath because `Admin/buildDockerImage.sh:12` runs
`./docs/manual/build-manual.sh` before packaging. The tag-driven release workflow does not use that
script. `Admin/.github/workflows/maven.yml:62-70` calls Maven directly:

```yaml
          mvn -X -B clean package \
            -Dquarkus.docker.buildx.platform=linux/amd64,linux/arm64 \
            -Dquarkus.container-image.build=true \
            -Dquarkus.container-image.push=true \
```

so `src/main/resources/manual/` is empty in a released image, `AdminManual.isAvailable()` returns
false, and `MainLayout` hides both entry points. Author's workflow has the same shape. The umbrella
has **no** workflows at all, so the installation manual has no CI path and is built only by a
developer running `buildDockerImages.sh`.

This is a prerequisite, not a side note: localizing a manual that no released image contains changes
nothing for any reader.

## 7. Recommendations

Ordered by what each removes from the blocker list. Nothing here is scheduled.

### 7.1 The blockers

Tagged **T** typesetting, **F** figures, **D** delivery, **G** gates.

- **B1 — No document language is declared.** `.tex:9-27` loads no `babel`, `polyglossia` or bidi
  engine. *Why it blocks:* Arabic cannot be set at all, and LaTeX's generated words stay English in
  every edition. **T**
- **B2 — No Arabic-capable face, and the font guard hides its absence.** `elicit-brand.sty:65,74`.
  *Why it blocks:* an Arabic build would succeed and produce an unreadable PDF. **T**
- **B3 — The design package holds English chrome.** Call-out titles, settings headings, `set`/`may`/
  `keep`, the continuation rows, `built` (`:99,106,147,158,171,218-220,233-245,240,249,266`). *Why
  it blocks:* a translated document still renders English furniture. **T**
- **B4 — The document holds English chrome as literals.** Title page (`.tex:41-62`),
  `\section*{Contents}` (`:70`), 14 `\markboth`, 40 `Chapter~`/`Section~`/`Table~`. *Why it blocks:*
  same, and `\contentsname`/`\tablename` cannot help while the words are typed. **T**
- **B5 — The page furniture is committed to the left.** Column specs, `@{}l`/`@{}r`, `leftrule`,
  `frame = leftline`, `\fancyhead[L]`, `leftmargin`. *Why it blocks:* an Arabic edition mirrors its
  text and not its layout. **T**
- **B6 — LTR islands have no isolation.** 371 inline atoms and ten verbatim blocks. *Why it blocks:*
  punctuation around a setting name reorders, defeating NFR-007. **T**
- **B7 — The design package is three divergent copies.** 202 differing lines umbrella-to-Admin,
  12 Admin-to-Author. *Why it blocks:* every chrome fix has to be made three times. **T**
- **B8 — The capture harness has no language.** No `locale`, no `?lang=` (`:97-102`). *Why it
  blocks:* figures are captured in the machine's default locale. **F**
- **B9 — Every locator is an English interface string.** `:79-93`, `:142`, `:156`, `:162-165`. *Why
  it blocks:* a Spanish run misses the figures it is for, and `tryShot` turns the misses into
  warnings. **F**
- **B10 — Three figures are one-shot per language.** `05`, `06`, `07` (`:141-181`). *Why it blocks:*
  each language needs its own greenfield database pass. **F**
- **B11 — The figure directory is baked into `\screenshot`.** `elicit-brand.sty:139`. *Why it
  blocks:* call sites cannot address a per-language figure set. **F**
- **B12 — The bundled realm turns internationalization off.** `keycloak/elicit-realm.json:1550-1551`.
  *Why it blocks:* the sign-in figure cannot be localized even if the decision changed. **F**
- **B13 — One job name, one output name, one shared working directory, one shared stamp.**
  `build-manual.sh:28-29,79-83,92`. *Why it blocks:* editions overwrite each other in sequence and
  corrupt each other in parallel. **D**
- **B14 — Nothing reads the reader's locale when the manual is served.** `AdminManual:36,42-43`;
  `ManualResource:39,54-67`. *Why it blocks:* the Spanish console serves the English PDF. **D**
- **B15 — A business rule says the manual is published in English.** Admin UC-029 BR-009. *Why it
  blocks:* the specification has to change before the code does. **D**
- **B16 — The release builds no manual.** `Admin/.github/workflows/maven.yml:62-70`; no umbrella
  workflows. *Why it blocks:* no reader of a released image has any manual to localize. **D**
- **B17 — The property gate is bound to the English file and English sentinels.**
  `check-properties.sh:25,179`. *Why it blocks:* an edition is either unchecked or spuriously
  failing. **G**
- **B18 — No gate ties a `\ui{}` label to the application's own translation.** 228 uses across the
  three manuals. *Why it blocks:* a translated manual can name buttons that do not exist. **G**

### 7.2 Proposed changes

| | Change | Removes | What the build does with it |
|---|---|---|---|
| E1 | A language dimension in `build-manual.sh`: `MANUAL_LANGS`, and `JOB`/`-outdir`/`build-info.tex` per edition | B13 | loops the container once per language into `build/<tag>/`, writing `elicit-installation-manual-<tag>.pdf` |
| E2 | One `elicit-brand.sty`, shared by the three manuals, with the product name and the figure directory as hooks | B7, B11 | the chrome is localized once instead of three times |
| E3 | A per-edition string file, `\input` by the design package, holding the call-out titles, the settings headings, the Change marks, the footer word and the contents heading | B3, B4 | the chrome follows the edition; no translator sees the `.sty` |
| E4 | Declare the language per edition (`babel` with `bidi=basic` under LuaLaTeX) | B1, and `\tablename`/`\contentsname` in B4 | LaTeX's generated words and hyphenation become correct for free |
| E5 | An Arabic font family, and make the `\IfFontExistsTF` guard fail the build when the edition's script has no face | B2 | an Arabic build either embeds a face or stops |
| E6 | Direction-aware furniture: logical column order, rules and indents on the inner edge, `\fancyhead`/`\fancyfoot` through babel's mirroring | B5 | one source sets both directions |
| E7 | Explicit LTR isolation inside `\opt`/`\val`/`\file` and the verbatim environments | B6 | a setting name survives an Arabic paragraph intact |
| E8 | Stable ids on the screens the manuals photograph, and locators rewritten to use them | B9 | one harness drives every language |
| E9 | A `--lang` (or `MANUAL_LANG`) on the harness: a Playwright `locale`, `?lang=<tag>` on each `goto`, and `MANUAL_IMAGES=images/<tag>` | B8 | one greenfield pass per language produces one figure set |
| E10 | Capture every language in one greenfield pass, or make the first-run state reproducible without a reset | B10 | the three one-shot figures cost one reset, not three |
| E11 | Per-language classpath resource plus negotiation in `ManualResource`, session locale first and `Accept-Language` second | B14 | `/api/manual` serves the edition the console is in |
| E12 | Revise Admin UC-029 BR-009, with attributability carried per edition (version, tag, reviewer, source revision) and a stale edition withheld in favor of English | B15 | the specification permits what the code would do |
| E13 | Make the release build the manuals — either call `buildDockerImage.sh` from the tag workflow or add the manual step to it | B16 | a released image carries the manuals it serves |
| E14 | Language-neutral markers for the property gate's sentinels; run the gate against English only, and say so | B17 | the gate keeps working and its scope is explicit |
| E15 | A gate comparing every `\ui{}` argument in an edition against the module's `translations_<tag>.properties` | B18 | a translated manual cannot name a button the reader has not got |

E12's realm question (B12) is deliberately left unaddressed: the decision is that the Keycloak
figures stay English, so B12 is accepted rather than removed, and the captions carry the
explanation.

### 7.3 The shape of the per-language build (decided: inside the script)

Two shapes were considered. Adding targets — `Manual`, `Manual-es`, `Manual-ar` — to
`buildDockerImages.sh` parallelizes for free, because targets already run in background subshells.
It also collides: each would rewrite the shared `build-info.tex` and `build/`, every new name must
be added to `build_dir`, `build_scripts` and the port-warning predicate at `:88-89`, and
`build_scripts()`'s output is word-split so no argument can be passed through it anyway.

The loop belongs inside `build-manual.sh`, driven by a `MANUAL_LANGS` variable, with `JOB` and the
output directory varying per edition. `buildDockerImages.sh` then keeps one `Manual` target and
needs no change at all. The language list should be discovered rather than hardcoded, the way
`checkLanguages.sh:35-50` does it — glob `i18n/translations_*.properties`, normalize `_` to `-`, add
`en` implicitly — so that a manual edition cannot exist for a language the images do not carry, nor
be missing for one they do.

## 8. Experiments to run

None has been run. Each is cheap and each answers a question this document could not answer by
reading.

1. **Does `texlive/texlive:latest` carry an Arabic-capable face and a working LuaLaTeX bidi route?**
   Typeset two pages — one paragraph of Arabic prose containing an `\opt{}` atom, one settings table
   — with `babel`/`bidi=basic` and a candidate family (IBM Plex Sans Arabic if the image has it,
   Amiri or Noto Naskh otherwise). *Passes if* the Arabic renders with correct joining, the table
   mirrors, and the setting name survives with its punctuation on the right side. This gates E4, E5,
   E6 and E7.
2. **Can the existing harness be driven in Spanish?** Add a `locale` and `?lang=es-419`, replace the
   three department-form locators with ids, and take `05`, `06`, `07` on a greenfield stack.
   *Passes if* all three land with Spanish chrome and no step warns. This gates E8 and E9, and
   measures what E10 is worth.
3. **How large is an Arabic figure set?** Capture `11`–`16` with `ar` and compare page fit at
   1440×900 against English. *Passes if* the System pages still fit one screenshot each; if they do
   not, some captions describe a layout the Arabic edition does not have (section 4.4).
4. **Can the pseudo-locale sweep a manual?** The interface already has a `zxx` pseudo-locale for
   finding untranslated text. *Passes if* a `zxx` edition makes every English chrome string in
   section 3.3 visibly detectable, which would turn B3 and B4 into a mechanical checklist.
5. **Does the `\ui{}` gate find anything today?** Run the comparison of E15 against the English
   bundles before any translation exists. *Passes if* it reports zero mismatches; anything it
   reports is a defect in the English manual and is worth fixing first.

## 9. Sequenced steps

None scheduled; this is dependency order.

1. **Release — make the images carry a manual** (E13). Either the tag workflows call
   `buildDockerImage.sh`, or they gain the manual step. Worth doing on its own, immediately: it
   fixes a released defect independently of any language work.
2. **Umbrella — converge the design package** (E2). One `elicit-brand.sty`, product name and figure
   directory as hooks, the three manuals switched to it. No visible change; everything after this is
   done once instead of three times.
3. **Umbrella — the chrome becomes data** (E3, and the `\markboth`/`Chapter~` cleanup in B4).
   English-only still, and the English PDF must be byte-comparable in content before and after.
4. **Umbrella — declare the language and build per edition** (E4, E1). Two editions of the same
   English text, one declared `en`, prove the loop before any translation exists.
5. **Umbrella — right-to-left** (E5, E6, E7), gated on experiment 1.
6. **Umbrella and Admin — the figure harnesses gain a language** (E8, E9, E11 the `\graphicspath`
   half, E10), gated on experiment 2.
7. **Gates** (E14, E15), before the first translation is commissioned rather than after — E15 is
   what makes a reviewed translation verifiable.
8. **Admin and Author — specification and delivery** (E12, then E11's negotiation half). The use
   case changes first.
9. **Commission the first non-English edition** (section 5.2's decision, then section 5.3's
   machinery). One language, one manual — the administrator's manual in Spanish is the smallest
   edition that answers the original question.

## 10. Affected files (by step)

| Step | Files |
|---|---|
| 1 | `Admin/.github/workflows/maven.yml`; `Author/.github/workflows/maven.yml`; `Admin/buildDockerImage.sh`; `Author/buildDockerImage.sh` |
| 2 | `docs/manual/elicit-brand.sty`; `Admin/docs/manual/elicit-brand.sty`; `Author/docs/manual/elicit-brand.sty`; all three `elicit-*-manual.tex` |
| 3 | `docs/manual/elicit-brand.sty`; `docs/manual/elicit-installation-manual.tex`; new `docs/manual/lang/elicit-lang-en.tex` |
| 4 | `docs/manual/build-manual.sh`; `Admin/docs/manual/build-manual.sh`; `Author/docs/manual/build-manual.sh`; `.gitignore` |
| 5 | `docs/manual/elicit-brand.sty` |
| 6 | `docs/manual/capture/capture-screenshots.mjs`; `Admin/docs/manual/capture/capture-screenshots.mjs`; `Author/docs/manual/capture/capture-screenshots.mjs`; Admin and Survey views that need stable ids; `docs/manual/README.md` |
| 7 | `docs/manual/check-properties.sh`; a new `\ui{}` gate beside it; `buildDockerImages.sh` |
| 8 | `Admin/docs/use_cases/UC-029-consult-the-administrators-manual.md`; `Admin/src/main/java/com/elicitsoftware/admin/manual/{AdminManual,ManualResource}.java`; `Admin/src/main/java/com/elicitsoftware/admin/flow/MainLayout.java`; the Author equivalents |
| 9 | `Admin/i18n/` (the manual's request artifact); `Admin/docs/manual/` sources and figures per edition |
| docs | `CLAUDE.md`; `docs/manual/README.md`; `docs/I18N_IMPLEMENTATION_GUIDE.md`; `docs/requirements.md` and `docs/use_cases/` when this is scheduled (next free umbrella IDs: FR-038, NFR-013, C-016, UC-020) |
