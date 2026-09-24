# Translation request: Elicit Author

This document is generated from the application's English text file and is meant to be handed,
as is, to a translator or to an AI translation agent. It contains everything needed to produce a
complete language file for one target language. The application itself ships only the English
file; a finished translation is placed in the deployment's translations directory
(`elicit-i18n/author/` in the Elicit umbrella repository, mounted at `/opt/i18n`), never inside
the application.

## About the application

Elicit Author is the survey-authoring tool of the Elicit survey platform. Survey authors
(researchers, educators, analysts) use it to build questionnaires: they arrange steps, sections
and questions on a visual designer board, define answer options, draw branching rules between
questions, tag questions for reporting, and export the finished definition as a file that the
Admin console installs. The texts below are the tool's own words: navigation, buttons, menus,
grid headers, dialog labels and help texts, notifications, and the built-in guide that explains
the survey model. The survey content authors write is not included and stays as entered.

**Audience and tone:** professional users who design surveys; some pages (the guide, the rule
editor) explain concepts in plain prose. Use clear, precise, gender-neutral language and the
formal register where the language distinguishes one (for example *usted* in Spanish). Keep
button and menu labels short.

## Glossary and words to keep

| Term | Meaning | Rule |
|------|---------|------|
| Elicit | Product name | Never translate or transliterate |
| survey, step, section, question | The structural levels of a survey (a survey has steps, a step has sections, a section has questions) | Translate consistently; the same word everywhere |
| element | Any of step, section or question | Translate consistently |
| rule, relationship | A condition on one answer that shows, hides or repeats a downstream element | Translate consistently |
| token | A placeholder inside question text such as `{NAME|default}` | Translate as "placeholder" if clearer; never as a credential |
| access code | The credential a respondent types (issued by the Admin console) | Never call it a token |
| export | Producing the definition file for the Admin console | Use "export", never "publish" |
| retire / restore | Removing an element from the current definition (kept in history) and bringing it back | Translate consistently |
| ontology, dimension, tag, reporting tag | Reporting metadata attached to questions | Translate consistently |
| PDF, CSV, JSON, HTTP, OIDC, ID, URL, `.elicit` | Technical names | Keep as written |

## Rules for the translation

1. Keep every placeholder such as `{0}`, `{1}` exactly as written; move it inside the sentence
   where the language needs it, but never remove, rename or reorder it with another placeholder.
2. Where a row is flagged **params**, the value is processed by Java MessageFormat: any apostrophe
   in your translation must be doubled (`l''accès`). Rows without the flag may use a single
   apostrophe normally.
3. Where a row is flagged **html**, keep the HTML tags and translate only the text between them.
4. Respect the *Max length* column where given; these strings sit in buttons, menus and grid headers.
5. Do not translate the keys (the first column). Do not add, remove or reorder keys.
6. For right-to-left languages, write the text naturally; the tool mirrors the layout.
7. Return exactly one file named `translations_<tag>.properties` (for example
   `translations_es_419.properties`, `translations_ar.properties`), UTF-8 encoded, with the same
   keys in the same order as the English file at the end of this document, one `key=translation`
   per line, and a first line `# Reviewed by <name or agent>, <date>`.

## Brand strings

Deployments mount their own brand. The organization name shown in the header comes from the
brand's own files and is translated there, not in this file, through a `localized` block keyed
by language tag in `brand-config.json` (`name`, `organization`) and `brand-info.json`
(`description`); the base `name` also derives a technical identifier and must stay as it is.

## Strings to translate

Every row is one key in `translations.properties`. Return a file `translations_<tag>.properties` with exactly these keys in this order, one `key=translation` per line, UTF-8, no additions and no omissions.

| Key | English | Where it appears | Max length | Notes |
|-----|---------|------------------|------------|-------|
| `common.language` | Language | Header · language selector accessible name | 20 |  |
| `common.logoAlt` | {0} logo | Header · logo image alternative text | 40 | params: {0} = organization name |
| `common.appTitle` | {0} {1} | Header · application title | 40 | params: {0} = organization name, {1} = application type; identical in most languages (reorder only) |
| `common.appTitle.default` | Elicit {0} | Header · application title when no brand is mounted | 40 | params: {0} = application type; identical in most languages, Elicit stays |
| `common.appType.author` | Author | Header · the word appended to the organization name | 20 |  |
| `common.save` | Save | Any dialog · button | 15 |  |
| `common.cancel` | Cancel | Any dialog · button | 15 |  |
| `common.close` | Close | Any dialog · button | 15 |  |
| `common.remove` | Remove | Element menus · menu item that retires an element | 15 |  |
| `common.restore` | Restore | Element menus · menu item that restores a retired element | 15 |  |
| `common.ok` | OK | Dialogs · button | 10 | identical |
| `mainLayout.nav.surveys` | Surveys | Navigation drawer · menu item | 20 |  |
| `mainLayout.nav.guide` | How surveys work | Navigation drawer · menu item linking to the survey-model guide | 25 |  |
| `mainLayout.nav.manual` | Manual (PDF) | Navigation drawer · menu item opening the author's manual PDF in a new tab; PDF stays | 25 | identical |
| `mainLayout.header.manual` | Manual | Header · link opening the author's manual PDF in a new tab | 15 | identical |
| `mainLayout.header.manualTitle` | Open the author's manual (PDF) in a new tab | Header · tooltip of the manual link; PDF stays | 60 |  |
| `mainLayout.nav.importDefinition` | Import definition | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.logout` | Logout | Navigation drawer · menu item | 20 |  |
| `loginView.redirecting` | Redirecting to sign-in… | Sign-in redirect page (rarely visible) · paragraph | 40 |  |
| `logoutView.signingOut` | Signing out… | Sign-out redirect page (rarely visible) · paragraph | 40 |  |
| `unauthorizedView.title` | Access Restricted | Access restricted page · heading | 30 |  |
| `unauthorizedView.message` | You are authenticated but do not have the required permissions to access this application. Please contact your administrator to request the 'elicit_author' role for the Author application. | Access restricted page · paragraph; 'elicit_author' is a technical role name, keep it | 250 |  |
| `unauthorizedView.logout` | Logout | Access restricted page · button | 15 |  |
| `structureNavigationTree.showRemoved` | Show removed | Drawer structure tree · checkbox label | 20 |  |
| `structureNavigationTree.header` | Survey structure | Drawer structure tree · tree column header | 25 |  |
| `surveysView.pageTitle` | Surveys | Surveys list · browser tab title | 20 |  |
| `surveysView.title` | Surveys | Surveys list · heading | 20 |  |
| `surveysView.newSurvey` | New survey | Surveys list · primary button | 20 |  |
| `surveysView.grid.name` | Name | Surveys list · grid column header | 15 |  |
| `surveysView.grid.title` | Title | Surveys list · grid column header | 15 |  |
| `surveysView.grid.order` | Order | Surveys list · grid column header (display order) | 15 |  |
| `surveysView.grid.lastEditedBy` | Last edited by | Surveys list · grid column header | 20 |  |
| `surveysView.grid.releaseNote` | Release note | Surveys list · grid column header | 20 |  |
| `surveysView.open` | Open | Surveys list · row button opening the survey overview | 12 |  |
| `surveysView.editDetails` | Edit details | Surveys list · row button opening the metadata dialog | 15 |  |
| `surveyEditorView.pageTitle` | Survey | Survey overview · browser tab title | 20 |  |
| `surveyEditorView.editDetails` | Edit details | Survey overview · toolbar button | 15 |  |
| `surveyEditorView.export` | Export… | Survey overview · toolbar button, opens the export dialog (keep the ellipsis) | 15 |  |
| `surveyEditorView.design` | Design… | Survey overview · toolbar button, opens the designer (keep the ellipsis) | 15 |  |
| `surveyEditorView.reporting` | Reporting… | Survey overview · toolbar button, opens reporting tags (keep the ellipsis) | 15 |  |
| `surveyEditorView.summary` | {0} — key {1} | Survey overview · paragraph under the heading | 80 | params: {0} = survey name, {1} = survey key |
| `surveyEditorView.neverExported` | Never exported. | Survey overview · attribution sentence | 30 |  |
| `surveyEditorView.lastEditedBy` | Last edited by {0}. | Survey overview · attribution sentence | 40 | params: {0} = user name |
| `surveyEditorView.lastExportedBy` | Last exported by {0}: {1} | Survey overview · attribution sentence | 80 | params: {0} = user name, {1} = release note |
| `surveyEditorView.validation` | Validation | Survey overview · heading of the validation panel | 20 |  |
| `surveyEditorView.readyToExport` | Ready to export: no findings. | Survey overview · paragraph when validation finds nothing | 40 |  |
| `surveyEditorView.issue.error` | Error: {0} | Survey overview · validation list item | 100 | params: {0} = finding text identical |
| `surveyEditorView.issue.warning` | Warning: {0} | Survey overview · validation list item | 100 | params: {0} = finding text |
| `surveyMetadataDialog.newSurvey` | New survey | Survey details dialog · dialog title when creating | 20 |  |
| `surveyMetadataDialog.editSurvey` | Edit survey | Survey details dialog · dialog title when editing | 20 |  |
| `surveyMetadataDialog.name` | Name | Survey details dialog · field label | 20 |  |
| `surveyMetadataDialog.title` | Title | Survey details dialog · field label | 20 |  |
| `surveyMetadataDialog.description` | Description | Survey details dialog · field label | 20 |  |
| `surveyMetadataDialog.initialDisplayKey` | Initial display key | Survey details dialog · field label | 30 |  |
| `surveyMetadataDialog.postSurveyUrl` | Post-survey address | Survey details dialog · field label (URL shown after the survey) | 30 |  |
| `surveyMetadataDialog.surveyKey` | Survey key | Survey details dialog · read-only field label | 20 |  |
| `surveyMetadataDialog.nameRequired` | Name is required | Survey details dialog · field error | 30 |  |
| `surveyMetadataDialog.titleRequired` | Title is required | Survey details dialog · field error | 30 |  |
| `surveyMetadataDialog.created` | Survey created | Survey details dialog · success notification | 30 |  |
| `surveyMetadataDialog.updated` | Survey updated | Survey details dialog · success notification | 30 |  |
| `exportDialog.title` | Export {0} | Export dialog · dialog title | 40 | params: {0} = survey title |
| `exportDialog.releaseNote` | Release note | Export dialog · text area label | 20 |  |
| `exportDialog.releaseNote.placeholder` | What changed in this revision (recorded on the survey and in the file) | Export dialog · text area placeholder | 80 |  |
| `exportDialog.releaseNote.required` | Enter a release note | Export dialog · field error | 30 |  |
| `exportDialog.export` | Export | Export dialog · primary button | 15 |  |
| `exportDialog.issue.error` | Error: {0} | Export dialog · validation list item | 100 | params: {0} = finding text identical |
| `exportDialog.issue.warning` | Warning: {0} | Export dialog · validation list item | 100 | params: {0} = finding text |
| `exportDialog.errorsBlock` | {0} error(s) block the export; fix them in the editor first. | Export dialog · paragraph | 80 | params: {0} = number of errors |
| `exportDialog.warningsDoNotBlock` | Warnings do not block the export. | Export dialog · paragraph | 50 |  |
| `exportDialog.retroactiveReminder` | Reminder: renaming a step or section and changing a question's ontology tag apply retroactively at deployed sites (historical reports and answers are relabelled). | Export dialog · secondary paragraph | 200 |  |
| `exportDialog.download` | Download {0} | Export dialog · download link | 40 | params: {0} = file name |
| `exportDialog.revisionSummary` | Revision {0}: {1} | Export dialog · paragraph after export | 80 | params: {0} = revision number, {1} = list of table names with counts |
| `importDefinitionView.pageTitle` | Import definition | Import page · browser tab title | 25 |  |
| `importDefinitionView.title` | Import definition | Import page · heading | 25 |  |
| `importDefinitionView.intro` | Upload an ELICIT_SURVEY_EXPORT_V1 (.elicit) file to open it in Author as a working copy. Every survey and element key in the file is kept exactly as it is, so a later export updates the same rows at every site that already applied it. A survey whose key is already open here is refused. | Import page · paragraph; ELICIT_SURVEY_EXPORT_V1 and .elicit are technical names | 400 |  |
| `importDefinitionView.imported` | Imported {0} | Import page · heading after import | 40 | params: {0} = survey name |
| `importDefinitionView.surveyKey` | Survey key {0} | Import page · first part of the result sentence | 40 | params: {0} = survey key |
| `importDefinitionView.fileRevision` | , file revision {0} | Import page · optional clause appended to the result sentence (keep the leading comma) | 30 | params: {0} = revision number |
| `importDefinitionView.recordsImported` | {0} records imported from {1} | Import page · second sentence of the result | 60 | params: {0} = count, {1} = file name |
| `importDefinitionView.retiredSkipped` | , {0} retired record(s) skipped | Import page · optional clause appended to the result sentence (keep the leading comma) | 40 | params: {0} = count |
| `importDefinitionView.openInEditor` | Open in editor | Import page · primary button | 20 |  |
| `importDefinitionView.importFailed` | Import failed: {0} | Import page · error paragraph | 80 | params: {0} = reason |
| `guideView.pageTitle` | How a survey is put together | Guide page · browser tab title | 40 |  |
| `guideView.title` | How a survey is put together | Guide page · heading | 40 |  |
| `guideView.diagram.alt` | The designer board for a small survey called Family history: two step cards, About you and Each child, each holding section cards (You and Children, then Child) with question rows inside them, and a Show rule drawn from the question 'Has children' to the question 'Child count'. | Guide page · alternative text of the designer screenshot; Family history, About you, Each child, You, Children, Child, Has children and Child count are the names shown in the picture and stay in English | 400 |  |
| `guideView.surveysLink` | Go to the Surveys list | Guide page · link at the end of the page | 30 |  |
| `guideView.intro.p1` | An Elicit survey is a decision tree the respondent walks through. This page shows the pieces a survey is made of and how the designer draws them, using the same names and glyphs you will see on the board and in the structure tree. | Guide page · introduction paragraph | 300 |  |
| `guideView.tree.h` | Elicit is a decision tree | Guide page · section heading | 40 |  |
| `guideView.tree.p1` | A respondent moves through the survey one page at a time. What they answer decides what they see next: a question can open a section, repeat a group of questions, or change the wording of a later question. Designing a survey means designing that path. | Guide page · paragraph | 350 |  |
| `guideView.containers.h` | Containers nest | Guide page · section heading | 40 |  |
| `guideView.containers.p1` | Everything sits inside something else. A survey holds steps, a step holds sections, and a section holds questions. | Guide page · paragraph | 150 |  |
| `guideView.containers.step.term` | Step | Guide page · bold term starting a bullet | 15 |  |
| `guideView.containers.step.desc` | — a page in the respondent's progress bar. For you it is a lane on the board that groups related sections. | Guide page · rest of the bullet after the term (keep the leading dash) | 150 |  |
| `guideView.containers.section.term` | Section | Guide page · bold term starting a bullet | 15 |  |
| `guideView.containers.section.desc` | — a titled group of questions on that page. For you it is a card inside a lane. | Guide page · rest of the bullet after the term (keep the leading dash) | 120 |  |
| `guideView.containers.question.term` | Question | Guide page · bold term starting a bullet | 15 |  |
| `guideView.containers.question.desc` | — one input the respondent answers. For you it is a row inside a card, with a glyph that shows its type. | Guide page · rest of the bullet after the term (keep the leading dash) | 150 |  |
| `guideView.order.h` | Order is the default path | Guide page · section heading | 40 |  |
| `guideView.order.p1` | Steps run in order, sections run in order within their step, and questions run in order within their section. Unless a rule says otherwise, that display order is exactly what the respondent experiences. Dragging an element to a new position changes it. | Guide page · paragraph | 300 |  |
| `guideView.rules.h` | Rules are the arrows | Guide page · section heading | 40 |  |
| `guideView.rules.p1` | A rule reads the answer to one upstream question, compares it with a reference value using an operator, and when the comparison holds performs an action on one downstream target: a step, a section, or a question. Written out, every rule is the same sentence: | Guide page · paragraph ending with a colon that introduces the example sentence | 300 |  |
| `guideView.rules.example.question` | How many children? | Guide page · example question in the rule sentence | 30 |  |
| `guideView.rules.example.action` | Repeat | Guide page · example action name in the rule sentence (same word as the Repeat rule) | 15 |  |
| `guideView.rules.example.section` | Child | Guide page · example section name in the rule sentence | 15 |  |
| `guideView.rules.example.thatManyTimes` | that many times | Guide page · end of the rule sentence | 25 |  |
| `guideView.rules.example.ariaLabel` | Example rule: question 'How many children?' greater than 0 repeats section 'Child' that many times | Guide page · accessible name of the rule sentence | 120 |  |
| `guideView.rules.show.term` | Show | Guide page · bold rule name starting a bullet | 15 |  |
| `guideView.rules.show.desc` | — the target appears only when the rule holds. Until then the respondent never sees it. | Guide page · rest of the bullet (keep the leading dash) | 120 |  |
| `guideView.rules.repeat.term` | Repeat | Guide page · bold rule name starting a bullet | 15 |  |
| `guideView.rules.repeat.desc` | — the target section or question is repeated as many times as the answer says, for example one 'Child' section per child. | Guide page · rest of the bullet (keep the leading dash) | 150 |  |
| `guideView.rules.text.term` | Text | Guide page · bold rule name starting a bullet | 15 |  |
| `guideView.rules.text.desc` | — the answer is written into the target's text wherever a {TOKEN\|default} placeholder appears, so a later question can say 'your daughter' instead of 'your child'. The rule reaches everything inside its target: one Text rule on a step fills the token in every section and question of that step. A token is a slot name, not a question — several rules may fill the same token, each where it is used. | Guide page · rest of the bullet (keep the leading dash); {TOKEN | default} is a literal placeholder syntax, keep it | 450 |
| `guideView.rules.p2` | There is no Hide rule. Hiding is the same as not showing: give the element a Show rule and it stays hidden until that rule holds. | Guide page · paragraph | 150 |  |
| `guideView.rules.p3` | Operators are Boolean, Greater than, Equal, Not equal, Field exists and Contains. When several rules point at the same target, all of them must hold. | Guide page · paragraph; operator names should match the rule editor | 150 |  |
| `guideView.designer.h` | The designer draws all of this | Guide page · section heading | 40 |  |
| `guideView.designer.p1` | On the board, lanes are steps, cards are sections, rows are questions and arrows are rules. A card starts closed; one click opens it and selects it, a double click on a lane, card or row opens its editor, and the ⋮ menu on each element lists everything you can do to it. A breadcrumb at the top of the page shows where you are (survey, step, section, question), and the structure tree at the bottom of the drawer follows your selection. Every element carries its glyph, and the glyph is its handle: drag a glyph from the Controls panel to create an element, drag it within its container to reorder, or drag it into another container to show the same element there as well (a section in another step, a question in another section); Copy to step… in a card's menu makes a separate section instead. Drag a rule glyph onto a question to start a rule, then drag the arrowhead onto the target. Everything a drag can do, the element's menu can do too. | Guide page · long paragraph; menu item names (Copy to step…) should match the designer | 900 |  |
| `guideView.designing.h` | Designing a survey | Guide page · section heading | 40 |  |
| `guideView.designing.p1` | Open a survey from the Surveys list and choose Design…. The drawer on the left is your toolbox; the board on the right is the survey. Work top-down: steps, then sections, then questions, then the lists, rules and reporting tags that give them meaning. | Guide page · paragraph; Design… must match the toolbar button | 300 |  |
| `guideView.designing.search.term` | Search | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.search.desc` | — each of the Lists, Sections and Questions panels has its own search box that narrows that panel as you type: lists by name, description and option text, sections by name and description, questions by name, text and type. Handy once a survey has dozens of each. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.controls.term` | Controls | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.controls.desc` | — the glyphs for a step, a section, a question of the chosen type, and the three rules. Drag one onto the board, or use Add step and the Add… items in the menus. | Guide page · rest of the bullet (keep the leading dash) | 200 |  |
| `guideView.designing.lists.term` | Lists | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.lists.desc` | — the lists of options a choice question (radio, combo box, multi-select, checkbox group) offers. New list… defines one with its options in order; drag a list's glyph onto a question to give it those options, or pick the list in the question's editor. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.sections.term` | Sections | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.sections.desc` | — every section in the survey, once, with the number of steps it is mounted in. Drag one into a lane, or use Mount in step…, to show the same section in another step; it stays one section, so a change to its name or questions reaches every step. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.questions.term` | Questions | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.questions.desc` | — every question in the survey, once, with the number of sections it appears in. Drag one into a card, or use Add to section…, to ask the same question again elsewhere; it stays one question, so a change to its text reaches every section it is in. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.reportingTags.term` | Reporting tags | Guide page · bold panel name starting a bullet | 20 |  |
| `guideView.designing.reportingTags.desc` | — the ⋮ menu of a question or section offers Reporting tags…; the survey's Reporting… page lists them all with the tables and columns they produce. Tagged elements show a tag badge on the board, one entry per tag. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.structure.term` | Survey structure | Guide page · bold panel name starting a bullet | 20 |  |
| `guideView.designing.structure.desc` | — the tree at the bottom of the drawer; click an element to select it on the board. Show removed reveals retired elements so they can be restored. | Guide page · rest of the bullet (keep the leading dash) | 200 |  |
| `guideView.designing.p2` | When the structure is in place, add rules for anything conditional, tag what the report needs, check the validation panel on the survey overview, and Export… the .elicit file for the Admin app to install. | Guide page · paragraph; Export… must match the toolbar button | 250 |  |
| `guideView.reportingTags.h` | Reporting tags | Guide page · section heading | 40 |  |
| `guideView.reportingTags.p1` | A reporting tag names a column of the report a site builds from the answers. Tag a question and its answers fill that column; tag a section as it appears in a step with a fixed value, and every respondent who reaches it is classified by that value. Tags that share a dimension share one table of values. Open a survey's Reporting page, or the 'Reporting tags…' item on a question or section, to manage them. Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified. | Guide page · paragraph | 500 |  |
| `guideView.identity.h` | Identity and exporting | Guide page · section heading | 40 |  |
| `guideView.identity.p1` | Every element gets a key the moment it is created and keeps it for life. Removing an element retires it rather than deleting it, so answers already recorded at a site keep their meaning. When the survey is ready you export a .elicit file; the Admin app installs it at a site the first time and updates it in place after that, matching elements by key. | Guide page · paragraph | 400 |  |
| `guideView.legend.h` | Glyph legend | Guide page · heading of the glyph legend | 20 |  |
| `guideView.legend.step` | Step, numbered by its display order | Guide page · legend entry | 50 |  |
| `guideView.legend.section` | Section | Guide page · legend entry | 20 |  |
| `guideView.legend.question` | Text question — the icon changes with the question type (number, choice, date, …) | Guide page · legend entry | 90 |  |
| `guideView.legend.showRule` | Show rule | Guide page · legend entry | 20 |  |
| `guideView.legend.repeatRule` | Repeat rule | Guide page · legend entry | 20 |  |
| `guideView.legend.textRule` | Text rule | Guide page · legend entry | 20 |  |
| `designer.guideLink` | How surveys work | Designer board and toolbox · link to the survey-model guide | 30 |  |
| `designer.menu.edit` | Edit… | Designer node menus (step, section, question, list) · menu item opening the edit dialog | 20 |  |
| `designer.menu.moveUp` | Move up | Designer node menus · menu item | 20 |  |
| `designer.menu.moveDown` | Move down | Designer node menus · menu item | 20 |  |
| `designer.menu.openDetails` | Open details | Designer node menus · menu item opening the element details page | 25 |  |
| `designer.menu.reportingTags` | Reporting tags… | Designer node menus · menu item opening the reporting-tags dialog | 25 |  |
| `designerPalette.aria` | Palette: drag a glyph onto the board to add an element | Designer palette · accessible name of the palette | 80 |  |
| `designerPalette.questionType` | Question type for new questions | Designer palette · accessible name of the question-type picker | 50 |  |
| `designerPalette.step` | Step | Designer palette · row caption next to the step glyph | 15 |  |
| `designerPalette.section` | Section | Designer palette · row caption next to the section glyph | 15 |  |
| `designerPalette.question` | Question | Designer palette · row caption next to the question glyph | 15 |  |
| `designerPalette.rule` | Rule | Designer palette · row caption next to the rule glyphs | 15 |  |
| `designerPalette.roledescription` | draggable palette item | Designer palette · aria-roledescription of each glyph (lower case) | 40 |  |
| `designerToolbox.controls` | Controls | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.lists` | Lists | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.sections` | Sections | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.questions` | Questions | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.listsCount` | Lists ({0}) | Designer toolbox · panel title while a search is active | 25 | params: {0} = number of matches |
| `designerToolbox.sectionsCount` | Sections ({0}) | Designer toolbox · panel title while a search is active | 25 | params: {0} = number of matches |
| `designerToolbox.questionsCount` | Questions ({0}) | Designer toolbox · panel title while a search is active | 25 | params: {0} = number of matches |
| `glyph.step` | Step | Designer glyph · accessible name and tooltip of a step glyph without a number | 15 |  |
| `glyph.stepOrder` | Step {0} | Designer glyph · accessible name and tooltip of a numbered step glyph | 15 | params: {0} = step number |
| `glyph.section` | Section | Designer glyph · accessible name and tooltip of a section glyph | 15 |  |
| `glyph.question` | Question | Designer glyph · accessible name and tooltip of a question glyph with no type | 15 |  |
| `glyph.questionOfType` | {0} question | Designer glyph · accessible name and tooltip of a typed question glyph | 30 | params: {0} = question type name in lower case (e.g. "radio") |
| `glyph.rule.show` | Show rule | Designer glyph · accessible name and tooltip | 20 |  |
| `glyph.rule.repeat` | Repeat rule | Designer glyph · accessible name and tooltip | 20 |  |
| `glyph.rule.text` | Text rule | Designer glyph · accessible name and tooltip | 20 |  |
| `glyph.rule` | Rule | Designer glyph · accessible name and tooltip of a rule glyph of unknown kind | 15 |  |
| `listPanel.newList` | New list… | Designer toolbox, Lists panel · button | 20 |  |
| `listPanel.search` | Search lists | Designer toolbox, Lists panel · search field placeholder | 25 |  |
| `listPanel.search.aria` | Search lists by name, description or option text | Designer toolbox, Lists panel · search field accessible name | 70 |  |
| `listPanel.roledescription` | draggable list | Designer toolbox, Lists panel · aria-roledescription of a list glyph (lower case) | 30 |  |
| `listPanel.optionCount.one` | 1 option | Designer toolbox, Lists panel · count under a list name | 15 |  |
| `listPanel.optionCount.many` | {0} options | Designer toolbox, Lists panel · count under a list name | 15 | params: {0} = number of options (0 or 2+) |
| `listPanel.questionCount.one` | 1 question | Designer toolbox, Lists panel · count of questions using the list | 15 |  |
| `listPanel.questionCount.many` | {0} questions | Designer toolbox, Lists panel · count of questions using the list | 15 | params: {0} = number of questions (2+) |
| `listPanel.menu.aria` | Actions for list {0} | Designer toolbox, Lists panel · accessible name of the row menu button | 40 | params: {0} = list name |
| `listPanel.empty` | No lists yet. A choice question (radio, combo box, multi-select, checkbox group) needs one. | Designer toolbox, Lists panel · empty-state text | 120 |  |
| `listPanel.noMatch` | No list matches ''{0}''. | Designer toolbox, Lists panel · empty-state text while a search has no hits | 50 | params: {0} = the search text |
| `pickerDialog.chooseOne` | Choose one | Step/section picker dialog · validation message under the combo box | 25 |  |
| `previewFields.htmlPlaceholder` | <em>HTML content preview</em> | Question preview · placeholder shown for an HTML question with no content | 40 | html |
| `previewFields.invalidHtml` | Enter valid HTML to preview it. | Question preview · message replacing an HTML question whose markup cannot be parsed | 50 |  |
| `questionBankPanel.search` | Search questions | Designer toolbox, Questions panel · search field placeholder | 25 |  |
| `questionBankPanel.search.aria` | Search questions by name, text or type | Designer toolbox, Questions panel · search field accessible name | 60 |  |
| `questionBankPanel.roledescription` | draggable question | Designer toolbox, Questions panel · aria-roledescription of a question glyph (lower case) | 30 |  |
| `questionBankPanel.uses.one` | in 1 section | Designer toolbox, Questions panel · where the question is placed | 20 |  |
| `questionBankPanel.uses.many` | in {0} sections | Designer toolbox, Questions panel · where the question is placed | 20 | params: {0} = number of sections (0 or 2+) |
| `questionBankPanel.menu.aria` | Actions for question {0} | Designer toolbox, Questions panel · accessible name of the row menu button | 40 | params: {0} = question name |
| `questionBankPanel.menu.addToSection` | Add to section… | Designer toolbox, Questions panel · menu item | 25 |  |
| `questionBankPanel.empty` | No questions yet. Questions you add appear here and can be reused in other sections. | Designer toolbox, Questions panel · empty-state text | 110 |  |
| `questionBankPanel.noMatch` | No question matches ''{0}''. | Designer toolbox, Questions panel · empty-state text while a search has no hits | 50 | params: {0} = the search text |
| `questionRow.aria` | Question {0} | Designer board, question row · accessible name of the row | 40 | params: {0} = question name |
| `questionRow.aria.removed` | Question {0} (removed) | Designer board, question row · accessible name of a removed row | 50 | params: {0} = question name |
| `questionRow.menu.aria` | Actions for question {0} | Designer board, question row · accessible name of the row menu button | 40 | params: {0} = question name |
| `questionRow.menu.alsoAddToSection` | Also add to section… | Designer board, question row · menu item | 30 |  |
| `questionRow.menu.addRule` | Add rule… | Designer board, question row · menu item | 20 |  |
| `questionRow.menu.removeFromSection` | Remove from this section | Designer board, question row · menu item | 30 |  |
| `sectionBankPanel.search` | Search sections | Designer toolbox, Sections panel · search field placeholder | 25 |  |
| `sectionBankPanel.search.aria` | Search sections by name or description | Designer toolbox, Sections panel · search field accessible name | 60 |  |
| `sectionBankPanel.roledescription` | draggable section | Designer toolbox, Sections panel · aria-roledescription of a section glyph (lower case) | 30 |  |
| `sectionBankPanel.uses.one` | in 1 step | Designer toolbox, Sections panel · where the section is mounted | 20 |  |
| `sectionBankPanel.uses.many` | in {0} steps | Designer toolbox, Sections panel · where the section is mounted | 20 | params: {0} = number of steps (0 or 2+) |
| `sectionBankPanel.menu.aria` | Actions for section {0} | Designer toolbox, Sections panel · accessible name of the row menu button | 40 | params: {0} = section name |
| `sectionBankPanel.menu.mountInStep` | Mount in step… | Designer toolbox, Sections panel · menu item | 25 |  |
| `sectionBankPanel.empty` | No sections yet. Sections you add appear here and can be mounted in other steps. | Designer toolbox, Sections panel · empty-state text | 110 |  |
| `sectionBankPanel.noMatch` | No section matches ''{0}''. | Designer toolbox, Sections panel · empty-state text while a search has no hits | 50 | params: {0} = the search text |
| `sectionCard.questionCount.one` | 1 question | Designer board, section card · count in the card header | 15 |  |
| `sectionCard.questionCount.many` | {0} questions | Designer board, section card · count in the card header | 15 | params: {0} = number of questions (0 or 2+) |
| `sectionCard.mounted` | in {0} steps | Designer board, section card · badge shown when the section is mounted in several steps | 20 | params: {0} = number of steps (2+) |
| `sectionCard.mounted.title` | This section is mounted in {0} steps; editing it changes all of them | Designer board, section card · tooltip of the mounted badge | 90 | params: {0} = number of steps |
| `sectionCard.menu.aria` | Actions for section {0} | Designer board, section card · accessible name of the card menu button | 40 | params: {0} = section name |
| `sectionCard.menu.addQuestion` | Add question | Designer board, section card · menu item | 25 |  |
| `sectionCard.menu.copyToStep` | Copy to step… | Designer board, section card · menu item | 25 |  |
| `sectionCard.menu.alsoMountInStep` | Also mount in step… | Designer board, section card · menu item | 30 |  |
| `sectionCard.menu.removeFromStep` | Remove from this step | Designer board, section card · menu item | 30 |  |
| `sectionCard.aria` | Section {0}, {1} | Designer board, section card · accessible name of the card header | 60 | params: {0} = section name, {1} = the question count text |
| `sectionCard.aria.removed` | Section {0}, {1}, removed | Designer board, section card · accessible name of a removed card header | 70 | params: {0} = section name, {1} = the question count text |
| `sectionCard.empty` | No questions yet — drop a question glyph here or use Add question. | Designer board, section card · empty-state text inside an empty card | 90 | mentions the menu item sectionCard.menu.addQuestion |
| `sectionPreview.heading` | Preview | Designer preview pane · pane heading | 20 |  |
| `sectionPreview.aria` | Preview of the selected section as a respondent sees it | Designer preview pane · accessible name of the pane | 80 |  |
| `sectionPreview.hint.select` | Select a step, section or question on the board to see it as a respondent will. | Designer preview pane · hint when nothing is selected | 110 |  |
| `sectionPreview.hint.noSections` | Step ''{0}'' has no sections yet, so respondents would see nothing here. | Designer preview pane · hint for an empty step | 100 | params: {0} = step name |
| `sectionPreview.hint.step` | Respondents see one section per page. Answers typed here are not saved. | Designer preview pane · hint when a step is selected | 100 |  |
| `sectionPreview.hint.section` | What a respondent sees on this page. Answers typed here are not saved. | Designer preview pane · hint when a section or question is selected | 100 |  |
| `sectionPreview.note.stepRuleShown` | Step ''{0}'' is shown only when a rule reveals it. | Designer preview pane · note above the pages of a rule-shown step | 70 | params: {0} = step name |
| `sectionPreview.page` | Page {0} of {1} | Designer preview pane · caption above each page | 20 | params: {0} = page number, {1} = page count |
| `sectionPreview.note.removed` | Removed: respondents do not see this section. | Designer preview pane · note in a removed section | 60 |  |
| `sectionPreview.note.ruleShown` | Shown only when a rule reveals it. | Designer preview pane · note under a rule-shown section or question | 50 |  |
| `sectionPreview.note.noQuestions` | No questions yet. | Designer preview pane · note in a section without questions | 30 |  |
| `stepLane.aria` | Step {0}: {1} | Designer board, step lane · accessible name of the lane | 50 | params: {0} = step number, {1} = step name |
| `stepLane.sectionCount.one` | 1 section | Designer board, step lane · count in the lane header | 15 |  |
| `stepLane.sectionCount.many` | {0} sections | Designer board, step lane · count in the lane header | 15 | params: {0} = number of sections (0 or 2+) |
| `stepLane.menu.aria` | Actions for step {0} | Designer board, step lane · accessible name of the lane menu button | 40 | params: {0} = step name |
| `stepLane.menu.addStepAfter` | Add step after | Designer board, step lane · menu item | 25 |  |
| `stepLane.menu.addSection` | Add section | Designer board, step lane · menu item | 25 |  |
| `stepLane.menu.removeStep` | Remove step | Designer board, step lane · menu item | 20 |  |
| `stepLane.header.aria` | Step {0}: {1}, {2} | Designer board, step lane · accessible name of the lane header | 70 | params: {0} = step number, {1} = step name, {2} = the section count text |
| `stepLane.header.aria.removed` | Step {0}: {1}, {2}, removed | Designer board, step lane · accessible name of a removed lane header | 80 | params: {0} = step number, {1} = step name, {2} = the section count text |
| `stepLane.empty` | No sections yet — drop a section glyph here or use Add section. | Designer board, step lane · empty-state text inside an empty lane | 90 | mentions the menu item stepLane.menu.addSection |
| `tagBadge.title` | Reporting tags: {0} | Designer board, reporting-tag badge · tooltip and accessible name | 60 | params: {0} = the tag names |
| `surveyDesignerView.pageTitle` | Designer | Designer · browser tab title | 20 |  |
| `surveyDesignerView.showRemoved` | Show removed | Designer toolbar · checkbox label | 20 |  |
| `surveyDesignerView.showRules` | Show rules | Designer toolbar · checkbox label | 20 |  |
| `surveyDesignerView.showPreview` | Show preview | Designer toolbar · checkbox label | 20 |  |
| `surveyDesignerView.overview` | Overview | Designer toolbar · button back to the survey overview | 20 |  |
| `surveyDesignerView.addStep` | Add step | Designer toolbar · button | 20 |  |
| `surveyDesignerView.breadcrumb.aria` | Where you are in the survey | Designer breadcrumb · accessible name of the breadcrumb | 40 |  |
| `surveyDesignerView.noSurvey` | No survey with id {0} | Designer · error page message for an unknown survey id | 40 | params: {0} = survey id |
| `surveyDesignerView.emptyBoard` | This survey has no steps yet. Drag the Step glyph here or use Add step. | Designer board · empty-state text, followed by the guide link | 100 |  |
| `surveyDesignerView.crumb.step` | Step: {0} | Designer breadcrumb · crumb label | 40 | params: {0} = step name |
| `surveyDesignerView.crumb.section` | Section: {0} | Designer breadcrumb · crumb label | 40 | params: {0} = section name |
| `surveyDesignerView.crumb.question` | Question: {0} | Designer breadcrumb · crumb label | 40 | params: {0} = question name |
| `surveyDesignerView.sectionAlsoInStep` | Section ''{0}'' is now also in step ''{1}'' | Designer · success notification after mounting a section | 80 | params: {0} = section name, {1} = step name |
| `surveyDesignerView.questionAlsoInSection` | Question ''{0}'' is now also in section ''{1}'' | Designer · success notification after placing a question | 80 | params: {0} = question name, {1} = section name |
| `surveyDesignerView.movedStep` | Moved step ''{0}'' | Designer · notification after a move | 50 | params: {0} = step name |
| `surveyDesignerView.movedSection` | Moved section ''{0}'' | Designer · notification after a move | 50 | params: {0} = section name |
| `surveyDesignerView.movedQuestion` | Moved question ''{0}'' | Designer · notification after a move | 50 | params: {0} = question name |
| `surveyDesignerView.copySection.title` | Copy section ''{0}'' | Designer, copy-section picker dialog · dialog title | 50 | params: {0} = section name |
| `surveyDesignerView.copySection.label` | Into step | Designer, copy-section picker dialog · combo box label | 20 |  |
| `surveyDesignerView.mountSection.title` | Mount section ''{0}'' | Designer, mount-section picker dialog · dialog title | 50 | params: {0} = section name |
| `surveyDesignerView.mountSection.label` | Also in step | Designer, mount-section picker dialog · combo box label | 20 |  |
| `surveyDesignerView.questionUsesList` | Question ''{0}'' now offers the list ''{1}'' | Designer · success notification after dropping a list on a question | 80 | params: {0} = question name, {1} = list name |
| `surveyDesignerView.removeList.title` | Remove list | Designer, remove-list confirmation · dialog title | 25 |  |
| `surveyDesignerView.removeList.text` | Remove the list ''{0}'' and its options? | Designer, remove-list confirmation · dialog text | 60 | params: {0} = list name |
| `surveyDesignerView.removedList` | Removed list ''{0}'' | Designer · success notification | 50 | params: {0} = list name |
| `surveyDesignerView.addQuestion.title` | Add question ''{0}'' | Designer, add-question picker dialog · dialog title | 50 | params: {0} = question name |
| `surveyDesignerView.addQuestion.label` | To section | Designer, add-question picker dialog · combo box label | 20 |  |
| `surveyDesignerView.kind.step` | step | Designer, remove confirmation · the word inserted into removeAssignment sentences (lower case) | 15 |  |
| `surveyDesignerView.kind.section` | section | Designer, remove confirmation · the word inserted into removeAssignment sentences (lower case) | 15 |  |
| `surveyDesignerView.kind.question` | question | Designer, remove confirmation · the word inserted into removeAssignment sentences (lower case) | 15 |  |
| `surveyDesignerView.removeAssignment.title` | Remove from {0} | Designer, remove confirmation · dialog title | 30 | params: {0} = surveyDesignerView.kind.step or .section |
| `surveyDesignerView.removeAssignment.text` | Remove {0} ''{1}'' from {2} ''{3}''? | Designer, remove confirmation · dialog text, first sentence | 80 | params: {0} = kind word (section/question), {1} = its name, {2} = parent kind word (step/section), {3} = parent name |
| `surveyDesignerView.removeAssignment.last` | This is its only place in the survey, so the {0} itself is removed (it can be restored from its details page). | Designer, remove confirmation · second sentence when this is the element's only placement | 120 | params: {0} = kind word (section/question) |
| `surveyDesignerView.removeAssignment.shared` | It stays in the other places it is used. | Designer, remove confirmation · second sentence when the element is used elsewhere | 60 |  |
| `surveyDesignerView.removedAssignment` | Removed {0} ''{1}'' from {2} ''{3}'' | Designer · success notification | 80 | params: {0} = kind word (section/question), {1} = its name, {2} = parent kind word (step/section), {3} = parent name |
| `surveyDesignerView.removeStep.title` | Remove step | Designer, remove-step confirmation · dialog title | 25 |  |
| `surveyDesignerView.removeStep.text` | Remove step ''{0}''?{1} It can be restored from its details page. | Designer, remove-step confirmation · dialog text | 100 | params: {0} = step name, {1} = empty or a space plus surveyDesignerView.removeStep.dependents |
| `surveyDesignerView.removeStep.dependents` | Removed with it: {0}. | Designer, remove-step confirmation · sentence listing what goes with the step | 60 | params: {0} = list of dependents separated by "; " |
| `surveyDesignerView.removedStep` | Removed step ''{0}'' | Designer · success notification | 50 | params: {0} = step name |
| `surveyDesignerView.dropArrowHint` | Drop the arrow on a step, a section or a question | Designer · info notification when a rule arrow is dropped nowhere useful | 70 |  |
| `surveyDesignerView.ruleGone` | This rule no longer exists | Designer · info notification | 40 |  |
| `surveyDesignerView.ruleRepointed` | Rule re-pointed | Designer · success notification after moving a rule arrow end | 30 |  |
| `surveyDesignerView.undo` | Undo | Designer · inline button in the copied-section notification | 15 |  |
| `surveyDesignerView.copiedSection` | Copied section ''{0}'' into step ''{1}''. | Designer · notification text before the Undo button | 70 | params: {0} = new section name, {1} = step name |
| `surveyDesignerView.backward.one` | {0} — 1 rule now points backwards (shown in red). Move the downstream target after the upstream question, or move the elements again. | Designer · warning after a move that turned one rule backwards | 200 | params: {0} = the move message (e.g. surveyDesignerView.movedStep) |
| `surveyDesignerView.backward.many` | {0} — {1} rules now point backwards (shown in red). Move the downstream target after the upstream question, or move the elements again. | Designer · warning after a move that turned several rules backwards | 200 | params: {0} = the move message, {1} = number of rules |
| `constraint.surveys_name_un` | Another survey already uses this name | Any save · notification or field error from a database constraint | 80 |  |
| `constraint.surveys_survey_key_un` | A survey with this key already exists in Author | Import · notification | 80 |  |
| `constraint.steps_survey_name_un` | Another step in this survey already uses this name | Step dialog · field error | 80 |  |
| `constraint.steps_survey_display_order_un` | Another step already has this position | Designer · notification | 80 |  |
| `constraint.sections_survey_display_order_un` | Another section already has this position | Designer · notification | 80 |  |
| `constraint.select_groups_name_un` | Another list in this survey already uses this name | List dialog · field error | 80 |  |
| `constraint.select_items_display_text_un` | This list already has an option with that text | List dialog · field error | 80 |  |
| `constraint.steps_sections_display_key_un` | Another assignment already uses this display key | Designer · field error | 80 |  |
| `constraint.dimensions_un` | A dimension with this name already exists | Tag dialog · field error | 80 |  |
| `constraint.ontology_un` | A tag with this name already exists in this survey's namespace | Tag dialog · field error | 80 |  |
| `constraint.metadata_un` | This element already carries this tag with the same value | Tag assignment dialog · notification | 80 |  |
| `constraint.sqlstate.23503` | This element is still referenced by another element | Any removal · notification | 80 |  |
| `constraint.sqlstate.23505` | A row with the same unique value already exists ({0}) | Any save · notification | 80 | params: {0} = technical constraint name |
| `elementView.pageTitle` | Element | Element detail page · browser tab title | 20 |  |
| `elementView.surveyOverview` | Survey overview | Element detail page · back button | 25 |  |
| `elementView.openInDesigner` | Open in designer | Element detail page · button | 25 |  |
| `elementView.kind.step` | step | Element detail page · badge next to the heading, lower case | 15 | dynamic |
| `elementView.kind.section` | section | Element detail page · badge next to the heading, lower case | 15 | dynamic |
| `elementView.kind.question` | question | Element detail page · badge next to the heading, lower case | 15 | dynamic |
| `elementView.badge.removed` | {0} (removed) | Element detail page · badge for a retired element | 25 | params: {0} = step/section/question word |
| `elementView.question.untitled` | Question {0} | Element detail page · heading when the question has no short text | 25 | params: {0} = question number |
| `elementView.fact.displayOrder` | Display order | Element detail page · fact label | 25 |  |
| `elementView.fact.dimensionName` | Dimension name | Element detail page · fact label | 25 |  |
| `elementView.fact.description` | Description | Element detail page · fact label | 25 |  |
| `elementView.fact.type` | Type | Element detail page · fact label | 25 |  |
| `elementView.fact.text` | Text | Element detail page · fact label | 25 |  |
| `elementView.fact.required` | Required | Element detail page · fact label | 25 |  |
| `elementView.fact.tooltip` | Tooltip | Element detail page · fact label | 25 |  |
| `elementView.fact.minMax` | Min / max | Element detail page · fact label (minimum / maximum) | 25 |  |
| `elementView.fact.validationText` | Validation text | Element detail page · fact label | 25 |  |
| `elementView.fact.list` | List (durable id) | Element detail page · fact label; the answer list and its technical id | 25 |  |
| `elementView.fact.mask` | Mask | Element detail page · fact label (input mask) | 25 |  |
| `elementView.fact.placeholder` | Placeholder | Element detail page · fact label | 25 |  |
| `elementView.fact.defaultValue` | Default value | Element detail page · fact label | 25 |  |
| `elementView.fact.variant` | Variant | Element detail page · fact label | 25 |  |
| `elementView.fact.tagValue` | value "{0}" | Element detail page · part of the reporting-tags fact | 30 | params: {0} = fixed value |
| `elementView.fact.reportingTags` | Reporting tags | Element detail page · fact label | 25 |  |
| `elementView.fact.elementKey` | Element key | Element detail page · fact label | 25 |  |
| `elementView.fact.durableId` | Durable id | Element detail page · fact label | 25 |  |
| `elementView.fact.removedAt` | Removed at | Element detail page · fact label (date) | 25 |  |
| `elementView.remove.header` | Remove {0}? | Remove confirmation · dialog header | 60 | params: {0} = element description, e.g. Question "Age" |
| `elementView.remove.explanation` | The element is retired, not deleted: sites that apply the next export close it, respondents who already started keep it, and recorded answers stay valid. | Remove confirmation · paragraph | 200 |  |
| `elementView.remove.dependents` | Removed together with it: | Remove confirmation · paragraph before the list of dependents | 40 |  |
| `elementView.undo` | Undo | Removal notice · button | 10 |  |
| `elementView.removed` | {0} removed | Removal notice · text | 60 | params: {0} = element description |
| `elementView.restore.blocked` | Cannot restore {0}: {1} | Element detail page · error notification | 120 | params: {0} = element description, {1} = reasons |
| `elementView.restore.header` | Restore {0}? | Restore confirmation · dialog header | 60 | params: {0} = element description |
| `elementView.restore.explanation` | The element reopens in its former position, keeping its element key. | Restore confirmation · paragraph | 120 |  |
| `elementView.restore.dependents` | Restored together with it: | Restore confirmation · paragraph before the list of dependents | 40 |  |
| `elementView.restored` | Restored | Element detail page · success notification | 20 |  |
| `reportingView.pageTitle` | Reporting tags | Reporting tags page · browser tab title | 25 |  |
| `reportingView.heading` | Reporting tags — {0} | Reporting tags page · heading | 60 | params: {0} = survey title |
| `reportingView.surveyOverview` | Survey overview | Reporting tags page · back button | 25 |  |
| `reportingView.openInDesigner` | Open in designer | Reporting tags page · button | 25 |  |
| `reportingView.newTag` | New tag… | Reporting tags page · button, opens a dialog | 20 |  |
| `reportingView.assignTag` | Assign tag… | Reporting tags page · button, opens a dialog | 20 |  |
| `reportingView.intro` | A reporting tag names a column of the site's report. Assign it to a question and the answers fill that column; assign it to a section with a fixed value and every respondent who reaches the section is classified by it. Tags on the same dimension share one table of values. | Reporting tags page · explanatory paragraph | 400 |  |
| `reportingView.retro` | Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified when the survey is exported and applied. | Reporting tags page · explanatory paragraph | 200 |  |
| `reportingView.grid.tag` | Tag | Reporting tags page · tags grid header | 15 |  |
| `reportingView.grid.dimension` | Dimension | Reporting tags page · tags grid header | 15 |  |
| `reportingView.grid.ownTable` | (own table) | Reporting tags page · tags grid cell when the tag has no dimension | 15 |  |
| `reportingView.grid.table` | Table | Reporting tags page · tags grid header | 15 |  |
| `reportingView.grid.reportColumn` | Report column | Reporting tags page · tags grid header | 20 |  |
| `reportingView.grid.assignments` | Assignments | Reporting tags page · tags grid header | 15 |  |
| `reportingView.edit` | Edit… | Reporting tags page · small grid button, opens a dialog | 10 |  |
| `reportingView.removeTag.tooltip` | Remove this tag | Reporting tags page · tooltip on the remove button | 30 |  |
| `reportingView.removeTag.blocked` | Remove its assignments first | Reporting tags page · tooltip on the disabled remove button | 40 |  |
| `reportingView.assignmentsGrid.element` | Element | Reporting tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.where` | Where | Reporting tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.appliesTo` | Applies to | Reporting tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.tag` | Tag | Reporting tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.value` | Value | Reporting tags page · assignments grid header | 15 |  |
| `reportingView.removedElement` | removed element | Reporting tags page · badge on an assignment whose element was removed | 20 |  |
| `reportingView.tags` | Tags | Reporting tags page · section heading | 15 |  |
| `reportingView.assignments` | Assignments | Reporting tags page · section heading | 15 |  |
| `reportingView.impact.title` | Dimensional impact | Reporting tags page · section heading | 25 |  |
| `reportingView.impact.intro` | When the exported survey is applied and answers arrive, the site's reporting build creates these objects in the surveyreport schema. | Reporting tags page · paragraph; surveyreport is a technical schema name | 200 |  |
| `reportingView.impact.none` | No tags yet: the survey adds nothing to the reporting schema. | Reporting tags page · paragraph | 80 |  |
| `reportingView.impact.sharedDimension` | shared dimension "{0}" | Reporting tags page · fragment of a list item | 40 | params: {0} = dimension name |
| `reportingView.impact.ownTable` | own table of tag "{0}" | Reporting tags page · fragment of a list item | 40 | params: {0} = tag name |
| `reportingView.impact.assignmentOne` | {0} assignment | Reporting tags page · count fragment, singular | 20 | params: {0} = 1 |
| `reportingView.impact.assignmentMany` | {0} assignments | Reporting tags page · count fragment, plural | 20 | params: {0} = count |
| `reportingView.impact.table` | {0} — {1} — tags: {2} — {3} | Reporting tags page · list item | 120 | params: {0} = table name, {1} = shared/own fragment, {2} = tag list, {3} = count fragment |
| `reportingView.impact.dimensionTables` | Dimension tables | Reporting tags page · list caption | 25 |  |
| `reportingView.impact.column` | {0} → {1} (tag "{2}") | Reporting tags page · list item | 80 | params: {0} = column name, {1} = table name, {2} = tag name |
| `reportingView.impact.reportColumns` | Report columns | Reporting tags page · list caption | 25 |  |
| `reportingView.impact.error` | Error: {0} | Reporting tags page · list item prefix | 40 | params: {0} = finding text; identical |
| `reportingView.impact.warning` | Warning: {0} | Reporting tags page · list item prefix | 40 | params: {0} = finding text |
| `reportingView.impact.findings` | Findings | Reporting tags page · list caption | 25 |  |
| `reportingView.removeTag.header` | Remove tag | Remove tag confirmation · dialog header | 25 |  |
| `reportingView.removeTag.text` | Remove the tag "{0}"? It has no assignments. | Remove tag confirmation · dialog text | 80 | params: {0} = tag name |
| `reportingView.removedTag` | Removed tag "{0}" | Reporting tags page · success notification | 40 | params: {0} = tag name |
| `reportingView.removeAssignment.header` | Remove reporting tag | Remove assignment confirmation · dialog header | 30 |  |
| `reportingView.removeAssignment.text` | Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied. | Remove assignment confirmation · dialog text | 200 | params: {0} = tag name, {1} = element description |
| `elementTagsDialog.title` | Reporting tags — {0} | Element tags dialog (designer) · dialog header | 60 | params: {0} = element name |
| `elementTagsDialog.addTag` | Add tag… | Element tags dialog · button, opens a dialog | 15 |  |
| `elementTagsDialog.empty` | No reporting tags yet. Without a tag, answers to this element are not reported. | Element tags dialog · paragraph when no tag is assigned | 120 |  |
| `elementTagsDialog.grid.tag` | Tag | Element tags dialog · grid header | 15 |  |
| `elementTagsDialog.grid.appliesTo` | Applies to | Element tags dialog · grid header | 15 |  |
| `elementTagsDialog.grid.value` | Value | Element tags dialog · grid header | 15 |  |
| `elementTagsDialog.grid.reportColumn` | Report column | Element tags dialog · grid header | 20 |  |
| `elementTagsDialog.edit` | Edit… | Element tags dialog · small grid button, opens a dialog | 10 |  |
| `elementTagsDialog.removeAssignment.header` | Remove reporting tag | Remove assignment confirmation · dialog header | 30 |  |
| `elementTagsDialog.removeAssignment.text` | Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied. | Remove assignment confirmation · dialog text | 200 | params: {0} = tag name, {1} = element name |
| `elementTagsDialog.removedTag` | Removed tag "{0}" | Element tags dialog · success notification | 40 | params: {0} = tag name |
| `tagAssignmentDialog.title.assign` | Assign reporting tag | Assign tag dialog · dialog header | 30 |  |
| `tagAssignmentDialog.title.edit` | Edit reporting tag | Assign tag dialog · dialog header when editing | 30 |  |
| `tagAssignmentDialog.element` | Element | Assign tag dialog · element picker label | 15 |  |
| `tagAssignmentDialog.element.section` | Section {0} — {1} | Assign tag dialog · element picker item | 80 | params: {0} = section name, {1} = path in the survey |
| `tagAssignmentDialog.element.question` | Question {0} — {1} | Assign tag dialog · element picker item | 80 | params: {0} = question name, {1} = path in the survey |
| `tagAssignmentDialog.appliesTo` | Applies to | Assign tag dialog · scope radio group label | 15 |  |
| `tagAssignmentDialog.tag` | Tag | Assign tag dialog · tag picker label | 15 |  |
| `tagAssignmentDialog.newTag` | New tag… | Assign tag dialog · small button, opens a dialog | 15 |  |
| `tagAssignmentDialog.reportedValue` | Reported value | Assign tag dialog · radio group label | 20 |  |
| `tagAssignmentDialog.useAnswer` | Use the respondent's answer | Assign tag dialog · radio option | 40 |  |
| `tagAssignmentDialog.fixedValue` | Fixed value | Assign tag dialog · radio option and text field label | 20 |  |
| `tagAssignmentDialog.value.helper` | Stored lowercased and trimmed whenever the element is answered | Assign tag dialog · helper text under the fixed value field | 80 |  |
| `tagAssignmentDialog.acknowledge` | I understand that answers already recorded at deployed sites are reclassified | Assign tag dialog · checkbox label | 100 |  |
| `tagAssignmentDialog.help.tag` | The reporting tag. Its column in the report is filled from this element. | Assign tag dialog · help tooltip | 100 |  |
| `tagAssignmentDialog.help.value` | A constant reported instead of the answer, e.g. "Female" on the section about the respondent's mother. | Assign tag dialog · help tooltip | 120 |  |
| `tagAssignmentDialog.warning` | This assignment is exported already. Changing its tag or value is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on. | Assign tag dialog · warning shown when editing an exported assignment | 200 |  |
| `tagAssignmentDialog.scope.section` | This section as it appears in step "{0}" | Assign tag dialog · scope option | 60 | params: {0} = step name |
| `tagAssignmentDialog.scope.question` | This question wherever it appears | Assign tag dialog · scope option | 40 |  |
| `tagAssignmentDialog.scope.questionInSection` | This question only in section "{0}" | Assign tag dialog · scope option | 60 | params: {0} = section name |
| `tagAssignmentDialog.impact.none` | Choose a tag to see the table and column it fills. | Assign tag dialog · preview text | 60 |  |
| `tagAssignmentDialog.impact.text` | Values go to surveyreport.{0} · report column {1}{2} | Assign tag dialog · preview text; surveyreport is a technical schema name | 100 | params: {0} = table name, {1} = column name, {2} = constant/from-answer fragment |
| `tagAssignmentDialog.impact.constant` | · constant | Assign tag dialog · preview fragment, keep the leading separator | 15 |  |
| `tagAssignmentDialog.impact.fromAnswer` | · from the answer | Assign tag dialog · preview fragment, keep the leading separator | 20 |  |
| `tagAssignmentDialog.error.chooseTag` | Choose a tag | Assign tag dialog · field error | 30 |  |
| `tagAssignmentDialog.error.fixedValue` | Enter the fixed value or report the answer instead | Assign tag dialog · field error | 60 |  |
| `tagAssignmentDialog.error.chooseElement` | Choose an element | Assign tag dialog · field error | 30 |  |
| `tagAssignmentDialog.error.chooseScope` | Choose what the tag applies to | Assign tag dialog · field error | 40 |  |
| `tagAssignmentDialog.updated` | Updated tag "{0}" | Assign tag dialog · success notification | 40 | params: {0} = tag name |
| `tagAssignmentDialog.assigned` | Assigned tag "{0}" | Assign tag dialog · success notification | 40 | params: {0} = tag name |
| `tagDialog.title.new` | New reporting tag | Tag dialog · dialog header | 30 |  |
| `tagDialog.title.edit` | Edit reporting tag | Tag dialog · dialog header when editing | 30 |  |
| `tagDialog.tag` | Tag | Tag dialog · text field label | 15 |  |
| `tagDialog.tag.helper` | Letters, digits, spaces and hyphens. Becomes a column of the report. | Tag dialog · helper text | 80 |  |
| `tagDialog.dimension` | Dimension | Tag dialog · combo box label | 15 |  |
| `tagDialog.dimension.placeholder` | None: the tag gets its own table | Tag dialog · combo box placeholder | 40 |  |
| `tagDialog.dimension.helper` | Type a new name to create a dimension. Letters, digits and underscores. | Tag dialog · helper text | 80 |  |
| `tagDialog.namespace` | Namespace | Tag dialog · read-only field label | 15 |  |
| `tagDialog.namespace.helper` | Groups this survey's tags; a site keeps one row per namespace and tag | Tag dialog · helper text | 80 |  |
| `tagDialog.help.tag` | The tag's name. Answers of the tagged elements are reported under a column named after it. | Tag dialog · help tooltip | 100 |  |
| `tagDialog.help.dimension` | A shared category such as "age" or "cancer". Tags on the same dimension share one table of values; leave it empty to give this tag a table of its own. | Tag dialog · help tooltip | 160 |  |
| `tagDialog.acknowledge` | I understand that answers already recorded at deployed sites are reclassified | Tag dialog · checkbox label | 100 |  |
| `tagDialog.warning` | This tag is assigned. Changing its name or dimension is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on. | Tag dialog · warning shown when editing an assigned tag | 200 |  |
| `tagDialog.advanced` | Advanced | Tag dialog · collapsible section title | 15 |  |
| `tagDialog.error.tagRequired` | Tag is required | Tag dialog · field error | 30 |  |
| `tagDialog.created` | Created tag "{0}" | Tag dialog · success notification | 40 | params: {0} = tag name |
| `tagDialog.updated` | Updated tag "{0}" | Tag dialog · success notification | 40 | params: {0} = tag name |
| `tagDialog.preview.none` | Choose a tag to see the table and column it produces. | Tag dialog · preview text | 60 |  |
| `tagDialog.preview.text` | Values go to surveyreport.{0} · report column {1} | Tag dialog · preview text; surveyreport is a technical schema name | 80 | params: {0} = table name, {1} = column name |
| `elementDialog.kind.step` | step | Step/section dialog · the word "step" as used inside sentences (lower case) | 15 | dynamic (referenced through ElementDialog.KIND_STEP) |
| `elementDialog.kind.section` | section | Step/section dialog · the word "section" as used inside sentences (lower case) | 15 | dynamic (referenced through ElementDialog.KIND_SECTION) |
| `elementDialog.title.new` | New {0} | Step/section dialog · dialog title | 30 | params: {0} = "step" or "section" |
| `elementDialog.title.edit` | Edit {0} | Step/section dialog · dialog title | 30 | params: {0} = "step" or "section" |
| `elementDialog.name` | Name | Step/section dialog · field label | 20 |  |
| `elementDialog.dimensionName` | Dimension name | Step/section dialog · field label | 25 |  |
| `elementDialog.description` | Description | Step/section dialog · field label | 20 |  |
| `elementDialog.acknowledge` | I understand that the new name relabels reports already produced at deployed sites | Step/section dialog · checkbox shown when renaming | 120 |  |
| `elementDialog.dimensionName.helper` | Reporting dimension; defaults to the name. Changing it is a reporting change, not a rename. | Step/section dialog · field helper text | 120 |  |
| `elementDialog.help.name` | The {0}''s name. Shown to respondents and used as the report label; renaming it is retroactive. | Step/section dialog · field tooltip | 140 | params: {0} = "step" or "section" |
| `elementDialog.help.dimensionName` | The name this {0} reports under. Defaults to the name; change it to relabel reports without renaming. | Step/section dialog · field tooltip | 140 | params: {0} = "step" or "section" |
| `elementDialog.help.description` | An optional note for authors. It is not shown to respondents. | Step/section dialog · field tooltip | 80 |  |
| `elementDialog.error.nameRequired` | Name is required | Step/section dialog · field error | 30 |  |
| `elementDialog.renameWarning` | Renaming is retroactive: reports already produced at deployed sites will show the new name, not only reports produced from now on. | Step/section dialog · warning shown when the name changes | 200 |  |
| `elementDialog.placed` | The {0} is placed where you dropped it; you can move it afterwards. | Step/section dialog · note when creating | 100 | params: {0} = "step" or "section" |
| `elementDialog.saved.created` | Created {0} ''{1}'' | Step/section dialog · notification | 60 | params: {0} = "step" or "section", {1} = the element name |
| `elementDialog.saved.updated` | Updated {0} ''{1}'' | Step/section dialog · notification | 60 | params: {0} = "step" or "section", {1} = the element name |
| `questionDialog.title.new` | New question | Question dialog · dialog title | 30 |  |
| `questionDialog.title.edit` | Edit question | Question dialog · dialog title | 30 |  |
| `questionDialog.type` | Type | Question dialog · field label | 20 |  |
| `questionDialog.text` | Question text | Question dialog · field label | 25 |  |
| `questionDialog.shortText` | Short text | Question dialog · field label | 20 |  |
| `questionDialog.toolTip` | Tooltip | Question dialog · field label | 20 |  |
| `questionDialog.required` | Required | Question dialog · checkbox label | 20 |  |
| `questionDialog.minimum` | Minimum | Question dialog · field label | 20 |  |
| `questionDialog.maximum` | Maximum | Question dialog · field label | 20 |  |
| `questionDialog.validationText` | Validation message | Question dialog · field label | 25 |  |
| `questionDialog.selectGroup` | List of options | Question dialog · field label | 25 |  |
| `questionDialog.newList` | New list… | Question dialog · small button next to the list picker | 15 |  |
| `questionDialog.mask` | Input mask | Question dialog · field label | 20 |  |
| `questionDialog.placeholder` | Placeholder | Question dialog · field label | 20 |  |
| `questionDialog.defaultValue` | Default value | Question dialog · field label | 20 |  |
| `questionDialog.sample` | Sample answer | Question dialog · field label | 20 |  |
| `questionDialog.variant` | Variant | Question dialog · field label | 20 |  |
| `questionDialog.shortText.helper` | Shown in the tree and on the board | Question dialog · field helper text | 60 |  |
| `questionDialog.sample.helper` | Previews the texts that Text rules fill with this answer | Question dialog · field helper text | 80 |  |
| `questionDialog.variant.helper` | Vaadin theme variants applied to the field | Question dialog · field helper text | 60 |  |
| `questionDialog.help.type` | The kind of input the respondent uses to answer. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.shortText` | A short machine-friendly name, shown in the tree and on the board. | Question dialog · field tooltip | 100 |  |
| `questionDialog.help.text` | The question as the respondent sees it. May contain {TOKEN} placeholders that TEXT rules fill in. | Question dialog · field tooltip; {TOKEN} is a literal placeholder example, keep it | 140 |  |
| `questionDialog.help.toolTip` | Help text shown to the respondent when they hover the field. | Question dialog · field tooltip | 100 |  |
| `questionDialog.help.required` | The respondent must answer this before continuing. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.minimum` | The smallest number the respondent may enter. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.maximum` | The largest number the respondent may enter. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.selectGroup` | The list of choices the respondent picks from. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.mask` | A pattern that constrains what the respondent can type. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.placeholder` | Faint example text shown in the empty field. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.defaultValue` | The value the field starts with. | Question dialog · field tooltip | 60 |  |
| `questionDialog.help.sample` | An example answer, used only in previews. Wherever a Text rule writes this question's answer into another text, the preview shows this instead: sample "Bob" previews "Hello {PName}" as "Hello Bob". Respondents never see it. | Question dialog · field tooltip; "Bob", "Hello {PName}" and "Hello Bob" are literal examples | 300 |  |
| `questionDialog.help.variant` | Visual style variants applied to the field: alignment, size, or layout. | Question dialog · field tooltip | 100 |  |
| `questionDialog.error.typeRequired` | Choose a question type | Question dialog · field error | 40 |  |
| `questionDialog.error.textRequired` | A question needs its text | Question dialog · field error | 40 |  |
| `questionDialog.preview` | Preview | Question dialog · heading of the live preview box | 20 |  |
| `questionDialog.preview.chooseType` | Choose a type to preview the field. | Question dialog · placeholder text in the preview box | 60 |  |
| `questionDialog.saved.created` | Question created | Question dialog · notification | 40 |  |
| `questionDialog.saved.updated` | Question updated | Question dialog · notification | 40 |  |
| `relationshipDialog.title.new` | New rule | Rule dialog · dialog title | 30 |  |
| `relationshipDialog.title.edit` | Edit rule | Rule dialog · dialog title | 30 |  |
| `relationshipDialog.upstream` | Reads the answer to | Rule dialog · field label (the question whose answer is checked) | 30 |  |
| `relationshipDialog.operator` | Operator | Rule dialog · field label | 20 |  |
| `relationshipDialog.value` | Value | Rule dialog · field label | 20 |  |
| `relationshipDialog.action` | Action | Rule dialog · field label | 20 |  |
| `relationshipDialog.targetKind` | Target | Rule dialog · radio group label | 20 |  |
| `relationshipDialog.target` | Target element | Rule dialog · field label | 25 |  |
| `relationshipDialog.token` | Token | Rule dialog · field label (a text placeholder name) | 20 | identical |
| `relationshipDialog.description` | Description | Rule dialog · field label | 20 |  |
| `relationshipDialog.defaultUpstreamValue` | Default upstream value | Rule dialog · field label | 30 |  |
| `relationshipDialog.overrideUpstreamValue` | Override upstream value | Rule dialog · field label | 30 |  |
| `relationshipDialog.removeRule` | Remove rule | Rule dialog · button | 20 |  |
| `relationshipDialog.token.helper` | Optional for Show and Repeat, required for Text. Fills {TOKEN\|default} — or a phrase holding it, {Has TOKEN been\|Were you} — in the target's text and in every text inside it | Rule dialog · field helper text; {TOKEN | default} and {Has TOKEN been | Were you} are literal examples, keep them |
| `relationshipDialog.targetKind.step` | Step | Rule dialog · radio option | 15 |  |
| `relationshipDialog.targetKind.section` | Section | Rule dialog · radio option | 15 |  |
| `relationshipDialog.targetKind.question` | Question | Rule dialog · radio option | 15 |  |
| `relationshipDialog.help.upstream` | The question whose answer this rule checks. | Rule dialog · field tooltip | 80 |  |
| `relationshipDialog.help.operator` | How the answer is compared to the value — equals, greater than, contains, or just whether an answer exists. | Rule dialog · field tooltip | 140 |  |
| `relationshipDialog.help.value` | The value the answer is compared against. Leave empty for operators that only test whether an answer exists. | Rule dialog · field tooltip | 140 |  |
| `relationshipDialog.help.action` | What the rule does when it matches: SHOW reveals the target, REPEAT instantiates it once per the answer, TEXT substitutes the answer into the target's text. A SHOW or REPEAT rule may also carry a token naming the slot the upstream answer fills in the target's texts; a TEXT rule must. | Rule dialog · field tooltip; SHOW, REPEAT and TEXT are action names, keep them | 200 |  |
| `relationshipDialog.help.targetKind` | Whether the rule acts on a whole step, a section, or a single question. A rule with a token on a step fills it in every section and question of that step; on a section, in every question of it. | Rule dialog · field tooltip | 220 |  |
| `relationshipDialog.help.target` | The element the action applies to. | Rule dialog · field tooltip | 60 |  |
| `relationshipDialog.help.token` | The slot written as {TOKEN} in the target's texts that the upstream answer fills. Any rule may carry one — a SHOW or REPEAT rule that also names a token fills it, as the runtime does — and a TEXT rule must. Pick one the survey already uses or type a new name (up to 10 characters). The same token may be filled by several rules, one per place it is used. The preview shows the upstream question's sample answer in its place. | Rule dialog · field tooltip; TEXT is an action name and {TOKEN} a literal example, keep them | 320 |  |
| `relationshipDialog.help.description` | An optional note for authors. It is not shown to respondents. | Rule dialog · field tooltip | 80 |  |
| `relationshipDialog.help.defaultUpstreamValue` | The value to assume for the upstream answer when the respondent has not answered it yet. | Rule dialog · field tooltip | 120 |  |
| `relationshipDialog.help.overrideUpstreamValue` | A fixed value to use for the upstream answer instead of the respondent's actual answer. | Rule dialog · field tooltip | 120 |  |
| `relationshipDialog.advanced` | Advanced | Rule dialog · collapsible section title | 20 |  |
| `relationshipDialog.saved.created` | Rule created | Rule dialog · notification | 40 |  |
| `relationshipDialog.saved.updated` | Rule updated | Rule dialog · notification | 40 |  |
| `relationshipDialog.removed` | Rule removed | Rule dialog · notification | 40 |  |
| `relationshipDialog.sentence` | Question ''{0}'' {1}{2} → {3}{4} ''{5}'' | Rule dialog · the rule read back as one sentence | 120 | params: {0} = upstream question label, {1} = operator name, {2} = compared value (empty or leading space), {3} = action name, {4} = token (empty or leading space), {5} = target label |
| `relationshipDialog.sentence.noOperator` | (operator) | Rule dialog · placeholder inside the sentence when no operator is chosen | 20 |  |
| `relationshipDialog.sentence.noAction` | (action) | Rule dialog · placeholder inside the sentence when no action is chosen | 20 |  |
| `selectGroupDialog.title.new` | New list of options | Options-list dialog · dialog title | 30 |  |
| `selectGroupDialog.title.edit` | Edit list of options | Options-list dialog · dialog title | 30 |  |
| `selectGroupDialog.name` | Name | Options-list dialog · field label | 20 |  |
| `selectGroupDialog.description` | Description | Options-list dialog · field label | 20 |  |
| `selectGroupDialog.dataType` | Data type | Options-list dialog · field label | 20 |  |
| `selectGroupDialog.addOption` | Add option | Options-list dialog · button | 20 |  |
| `selectGroupDialog.dataType.helper` | Informational; 'Text' unless the site's reporting expects otherwise | Options-list dialog · field helper text; 'Text' is a stored value, keep it | 100 |  |
| `selectGroupDialog.help.name` | The list's name, shown when picking options for a question. Unique within the survey. | Options-list dialog · field tooltip | 120 |  |
| `selectGroupDialog.help.description` | An optional note for authors. | Options-list dialog · field tooltip | 60 |  |
| `selectGroupDialog.help.dataType` | A label carried with the list; the Survey runtime does not interpret it. | Options-list dialog · field tooltip | 100 |  |
| `selectGroupDialog.error.nameRequired` | Name is required | Options-list dialog · field error | 30 |  |
| `selectGroupDialog.option.displayText` | Display text | Options-list dialog · option row placeholder | 20 |  |
| `selectGroupDialog.option.displayText.aria` | Option display text | Options-list dialog · option row accessible name | 30 |  |
| `selectGroupDialog.option.codedValue` | Coded value (defaults to the text) | Options-list dialog · option row placeholder | 40 |  |
| `selectGroupDialog.option.codedValue.aria` | Option coded value | Options-list dialog · option row accessible name | 30 |  |
| `selectGroupDialog.option.moveUp` | Move option up | Options-list dialog · icon button accessible name | 20 |  |
| `selectGroupDialog.option.moveDown` | Move option down | Options-list dialog · icon button accessible name | 20 |  |
| `selectGroupDialog.option.remove` | Remove option | Options-list dialog · icon button accessible name | 20 |  |
| `selectGroupDialog.optionsLabel` | Options, in the order respondents see them | Options-list dialog · heading above the option rows | 60 |  |
| `selectGroupDialog.noOptions` | No options yet. | Options-list dialog · hint when the list has no options | 30 |  |
| `selectGroupDialog.saved.created` | Created list ''{0}'' | Options-list dialog · notification | 40 | params: {0} = the list name |
| `selectGroupDialog.saved.updated` | Updated list ''{0}'' | Options-list dialog · notification | 40 | params: {0} = the list name |
| `mainLayout.resizer.title` | Drag to resize the drawer | Drawer resize handle · tooltip | 40 |  |
| `mainLayout.resizer.aria` | Resize drawer | Drawer resize handle · accessible name | 30 |  |
| `mainLayout.smallScreen.heading` | A bigger screen is needed | Full-screen notice on narrow screens · heading | 40 |  |
| `mainLayout.smallScreen.text` | Elicit Author is built for designing surveys on a tablet or larger screen. Please switch to a wider device. | Full-screen notice on narrow screens · paragraph | 160 |  |
| `common.none` | none | System screens · value shown when nothing is set | 15 |  |
| `common.unknown` | unknown | System screens · value shown when a brand field is missing | 15 |  |
| `mainLayout.nav.system` | System | Navigation drawer · section heading for setup and diagnostics pages | 20 |  |
| `mainLayout.nav.systemOverview` | Overview | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemDatabase` | Database | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemBranding` | Branding | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemConnections` | Connections | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemOidc` | OIDC | Navigation drawer · menu item (the sign-in diagnostics page; OIDC is a protocol name) | 25 | identical |
| `system.grid.item` | Item | System pages · grid column header (the name of a row) | 20 |  |
| `system.grid.value` | Value | System pages · grid column header | 20 |  |
| `system.grid.state` | State | System pages · grid column header (a status badge) | 20 |  |
| `system.grid.detail` | Detail | System pages · grid column header | 20 |  |
| `system.badge.ok` | OK | System pages · status badge for a passed check | 12 | identical |
| `system.badge.failed` | Failed | System pages · status badge for a failed check | 12 |  |
| `system.badge.notChecked` | Not checked | System pages · status badge for a check not yet run | 15 |  |
| `system.badge.present` | Present | System pages · status badge for a setting that is set | 12 |  |
| `system.badge.absent` | Absent | System pages · status badge for a setting that is missing | 12 |  |
| `system.milliseconds` | {0} ms | System pages · duration in milliseconds | 12 | params: {0} = number; identical where "ms" is the usual abbreviation |
| `systemOverviewView.pageTitle` | System Overview | System overview · browser tab title | 40 |  |
| `systemOverviewView.title` | System Overview | System overview · page heading | 40 |  |
| `systemOverviewView.intro` | What this deployment is running, whether it is healthy, and whether every required setting is present. Settings are shown as present or absent, never by value; they are startup configuration and cannot be changed here. | System overview · introductory paragraph | 300 |  |
| `systemOverviewView.running` | Running | System overview · section heading (what is running) | 30 |  |
| `systemOverviewView.row.application` | Application | System overview · row label (application name) | 25 |  |
| `systemOverviewView.row.version` | Version | System overview · row label | 25 |  |
| `systemOverviewView.row.built` | Built | System overview · row label (build time) | 25 |  |
| `systemOverviewView.row.profile` | Profile | System overview · row label (runtime profile) | 25 |  |
| `systemOverviewView.row.started` | Started | System overview · row label (start time) | 25 |  |
| `systemOverviewView.row.uptime` | Uptime | System overview · row label | 25 |  |
| `systemOverviewView.uptime.days` | {0} d {1} h {2} min | System overview · uptime value | 30 | params: {0} = days, {1} = hours, {2} = minutes; identical where the abbreviations are the same |
| `systemOverviewView.uptime.hours` | {0} h {1} min | System overview · uptime value | 30 | params: {0} = hours, {1} = minutes; identical where the abbreviations are the same |
| `systemOverviewView.uptime.minutes` | {0} min {1} s | System overview · uptime value | 30 | params: {0} = minutes, {1} = seconds; identical where the abbreviations are the same |
| `systemOverviewView.health` | Health | System overview · section heading | 30 |  |
| `systemOverviewView.area.database` | Database connection | System overview · health row, link to the Database page | 40 |  |
| `systemOverviewView.area.surveySchema` | Survey schema | System overview · health row, link to the Database page | 40 |  |
| `systemOverviewView.area.surveySchema.notChecked` | not checked: the connection failed | System overview · health detail when the database connection failed | 60 |  |
| `systemOverviewView.area.branding` | Branding | System overview · health row, link to the Branding page | 40 |  |
| `systemOverviewView.area.connections` | Connections | System overview · health row, link to the Connections page | 40 |  |
| `systemOverviewView.connectionsSummary.one` | {0} outbound target configured; open Connections to check it | System overview · health detail when exactly one outbound target is configured | 80 | params: {0} = the number 1 |
| `systemOverviewView.connectionsSummary.many` | {0} outbound targets configured; open Connections to check them | System overview · health detail for several (or zero) outbound targets | 80 | params: {0} = count |
| `systemOverviewView.grid.area` | Area | System overview · grid column header | 20 |  |
| `systemOverviewView.requiredSettings` | Required settings | System overview · section heading | 30 |  |
| `systemOverviewView.grid.setting` | Setting | System overview · grid column header (property name) | 20 |  |
| `systemOverviewView.grid.suppliedBy` | Supplied by | System overview · grid column header (environment variable) | 20 |  |
| `systemOverviewView.grid.purpose` | Purpose | System overview · grid column header | 20 |  |
| `systemOverviewView.setting.datasourcePassword` | password of the database user Author connects as | System overview · purpose of a required setting | 60 |  |
| `systemOverviewView.setting.oidcSecret` | client secret registered with the identity provider | System overview · purpose of a required setting | 60 |  |
| `systemDatabaseView.pageTitle` | System Database | System database · browser tab title | 40 |  |
| `systemDatabaseView.title` | Database | System database · page heading | 30 |  |
| `systemDatabaseView.intro` | Author owns no migrations. The survey schema in its database is created by the Survey preview instance that shares the database, so a missing schema means that instance has not started against this database yet. | System database · introductory paragraph | 300 |  |
| `systemDatabaseView.connection` | Connection | System database · section heading | 30 |  |
| `systemDatabaseView.row.state` | State | System database · row label | 25 |  |
| `systemDatabaseView.row.configuredUser` | Configured user | System database · row label (database user from configuration) | 25 |  |
| `systemDatabaseView.row.connectedAs` | Connected as | System database · row label (database user reported by the server) | 25 |  |
| `systemDatabaseView.row.database` | Database | System database · row label (database name) | 25 |  |
| `systemDatabaseView.row.server` | Server | System database · row label (server version) | 25 |  |
| `systemDatabaseView.row.roundTrip` | Round trip | System database · row label (probe duration) | 25 |  |
| `systemDatabaseView.surveySchema` | Survey schema | System database · section heading | 30 |  |
| `systemDatabaseView.row.schemaPresent` | Schema present | System database · row label | 40 |  |
| `systemDatabaseView.row.surveyMigration` | Survey migration (survey.flyway_history) | System database · row label; keep the table name in parentheses | 50 |  |
| `systemDatabaseView.migration.notInstalled` | not installed | System database · migration value when the history table is missing | 20 |  |
| `systemDatabaseView.migration.empty` | empty | System database · migration value when the history table has no rows | 20 |  |
| `systemDatabaseView.migration.applied` | {0} applied {1} | System database · migration value | 60 | params: {0} = version, {1} = date applied |
| `systemDatabaseView.migration.failed` | {0} FAILED {1} | System database · migration value for a failed migration | 60 | params: {0} = version, {1} = date attempted |
| `systemDatabaseView.migration.unreadable` | present, but not readable by {0}: {1} | System database · migration value when the table cannot be read | 120 | params: {0} = database user, {1} = technical error text |
| `systemDatabaseView.row.durableSequences` | Kimball durable-key sequences | System database · row label; Kimball is a data-warehouse design name | 40 |  |
| `systemDatabaseView.row.surveysAuthored` | Surveys authored | System database · row label | 40 |  |
| `systemDatabaseView.schema.exists` | survey.surveys exists | System database · detail of a passed check; keep the table name | 40 |  |
| `systemDatabaseView.schema.missing` | survey.surveys does not exist: the Survey preview instance has not created the schema in this database yet | System database · detail of a failed check; keep the table name | 160 |  |
| `systemDatabaseView.durableSequences.exists` | survey.{0} exists | System database · detail of a passed check | 60 | params: {0} = sequence name; keep the survey. prefix |
| `systemDatabaseView.durableSequences.missing` | survey.{0} is missing: the schema in this database predates the V3 migrations | System database · detail of a failed check | 120 | params: {0} = sequence name; keep the survey. prefix |
| `systemBrandingView.pageTitle` | System Branding | System branding · browser tab title | 40 |  |
| `systemBrandingView.title` | Branding | System branding · page heading | 30 |  |
| `systemBrandingView.intro` | The brand resolves from the mounted directory first, then the local directory, then the default packaged with the application. Each asset is listed on its own, because a partial mount renders with the wrong fonts and no error. | System branding · introductory paragraph | 300 |  |
| `systemBrandingView.reload` | Reload brand | System branding · button | 20 |  |
| `systemBrandingView.reloaded` | Brand cache discarded; the result below is freshly resolved. | System branding · notification after reloading | 80 |  |
| `systemBrandingView.resolution` | Resolution | System branding · section heading (how the brand was found) | 30 |  |
| `systemBrandingView.row.configuredPath` | Configured path (brand.file.system.path) | System branding · row label; keep the property name in parentheses | 50 |  |
| `systemBrandingView.row.localPath` | Local path (brand.local.path) | System branding · row label; keep the property name in parentheses | 50 |  |
| `systemBrandingView.path.exists` | {0} (exists) | System branding · path value | 100 | params: {0} = directory path |
| `systemBrandingView.path.notFound` | {0} (not found) | System branding · path value | 100 | params: {0} = directory path |
| `systemBrandingView.row.metadataFile` | Metadata file | System branding · row label | 30 |  |
| `systemBrandingView.row.brandName` | Brand name | System branding · row label | 30 |  |
| `systemBrandingView.row.organization` | Organization | System branding · row label | 30 |  |
| `systemBrandingView.row.brandVersion` | Brand version | System branding · row label | 30 |  |
| `systemBrandingView.row.inUse` | In use (cached) | System branding · row label (the brand currently applied) | 30 |  |
| `systemBrandingView.inUse` | {0} [{1}] | System branding · value | 60 | params: {0} = brand display name, {1} = brand key; identical (punctuation only) |
| `systemBrandingView.row.summary` | Summary | System branding · row label | 30 |  |
| `systemBrandingView.summary.mounted` | Mounted brand at {0} | System branding · summary line | 80 | params: {0} = directory path |
| `systemBrandingView.summary.mountedNamed` | Mounted brand: {0} at {1} | System branding · summary line | 80 | params: {0} = brand name, {1} = directory path |
| `systemBrandingView.summary.local` | Local brand directory {0} | System branding · summary line | 80 | params: {0} = directory path |
| `systemBrandingView.summary.embedded` | Embedded default theme; no brand directory at {0} | System branding · summary line | 80 | params: {0} = directory path |
| `systemBrandingView.assets` | Assets | System branding · section heading | 30 |  |
| `systemBrandingView.grid.asset` | Asset | System branding · grid column header | 20 |  |
| `systemBrandingView.grid.path` | Path | System branding · grid column header | 20 |  |
| `systemBrandingView.grid.source` | Source | System branding · grid column header (where the asset came from) | 20 |  |
| `systemBrandingView.grid.location` | Location | System branding · grid column header | 20 |  |
| `systemBrandingView.source.mounted` | Mounted | System branding · source badge (from the mounted directory) | 15 |  |
| `systemBrandingView.source.local` | Local | System branding · source badge (from the local directory) | 15 | identical in Romance languages |
| `systemBrandingView.source.embedded` | Embedded default | System branding · source badge (packaged default) | 20 |  |
| `systemBrandingView.source.absent` | Absent | System branding · source badge | 15 |  |
| `systemBrandingView.source.unreadable` | Unreadable | System branding · source badge | 15 |  |
| `systemBrandingView.asset.colourStylesheet` | colour stylesheet | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.typographyStylesheet` | typography stylesheet | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.themeStylesheet` | theme stylesheet | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.horizontalLogo` | horizontal logo | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.headerIcon` | header icon | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.favicon` | favicon | System branding · what an asset is for (the browser tab icon) | 30 | identical |
| `systemBrandingView.asset.notFound` | not found in any location | System branding · asset location when nothing was found | 40 |  |
| `systemBrandingView.asset.unreadable` | {0} exists but cannot be read | System branding · asset location when the file cannot be read | 80 | params: {0} = file path |
| `systemBrandingView.logoPreview` | Logo preview | System branding · section heading | 30 |  |
| `systemBrandingView.logoAlt` | Brand logo | System branding · logo image alternative text | 30 |  |
| `systemConnectionsView.pageTitle` | System Connections | System connections · browser tab title | 40 |  |
| `systemConnectionsView.title` | Connections | System connections · page heading | 30 |  |
| `systemConnectionsView.intro` | Each outbound dependency Author will call, read from the same settings the runtime uses. A check sends one read-only request and gives up after {0} seconds. Author''s only other dependency is the database, checked on its own page. | System connections · introductory paragraph | 300 | params: {0} = timeout in seconds; double any apostrophe |
| `systemConnectionsView.empty` | No outbound targets are configured. | System connections · text when there is nothing to check | 60 |  |
| `systemConnectionsView.checkAll` | Check all | System connections · button | 15 |  |
| `systemConnectionsView.check` | Check | System connections · button in a grid row | 12 |  |
| `systemConnectionsView.grid.dependency` | Dependency | System connections · grid column header (kind of dependency) | 20 |  |
| `systemConnectionsView.grid.source` | Source | System connections · grid column header (where the address comes from) | 20 |  |
| `systemConnectionsView.grid.address` | Address | System connections · grid column header | 20 |  |
| `systemConnectionsView.grid.time` | Time | System connections · grid column header (probe duration) | 20 |  |
| `systemConnectionsView.group.identityProvider` | Identity provider | System connections · kind of dependency | 25 |  |
| `systemConnectionsView.group.telemetryCollector` | Telemetry collector | System connections · kind of dependency | 25 |  |
| `systemConnectionsView.result.timedOut` | timed out after {0} s | System connections · check detail | 40 | params: {0} = seconds |
| `systemConnectionsView.result.interrupted` | interrupted | System connections · check detail | 20 |  |
| `systemConnectionsView.result.discoveryServed` | discovery document served at {0} | System connections · check detail | 80 | params: {0} = address |
| `systemConnectionsView.result.notDiscovery` | HTTP {0} from {1} is not an OIDC discovery document; check the realm address | System connections · check detail; OIDC is a protocol name | 120 | params: {0} = HTTP status code, {1} = address |
| `systemConnectionsView.result.reachable` | reachable, HTTP {0} | System connections · check detail | 40 | params: {0} = HTTP status code |
| `systemConnectionsView.result.noHostPort` | address {0} has no host and port | System connections · check detail | 60 | params: {0} = address |
| `systemConnectionsView.result.connected` | connected to {0}:{1} | System connections · check detail | 40 | params: {0} = host, {1} = port |
| `oidcDiagnosticsView.pageTitle` | OIDC Diagnostics | OIDC diagnostics page · browser tab title | 40 |  |
| `oidcDiagnosticsView.title` | OIDC | OIDC diagnostics page · page heading; OIDC is a protocol name | 20 | identical |
| `oidcDiagnosticsView.intro` | The identity the provider handed this session: who signed in, which roles arrived, and whether the authoring role is among them. Tokens are masked. | OIDC diagnostics page · introductory paragraph | 200 |  |
| `oidcDiagnosticsView.user` | User: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = user name |
| `oidcDiagnosticsView.isAnonymous` | Is Anonymous: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = true or false |
| `oidcDiagnosticsView.roles` | Roles: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = list of role names; identical in Romance languages |
| `oidcDiagnosticsView.roleSource` | Role Source: {0} | OIDC diagnostics page · label in a monospace block (where the roles were read from) | 40 | params: {0} = source name |
| `oidcDiagnosticsView.hasRole` | Has {0}: {1} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = role name, {1} = true or false |
| `oidcDiagnosticsView.issuer` | Issuer: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = issuer address |
| `oidcDiagnosticsView.email` | Email: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = email address |
| `oidcDiagnosticsView.idToken` | ID Token: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = masked token |
| `oidcDiagnosticsView.idTokenUnavailable` | ID Token: Not available or resolvable | OIDC diagnostics page · label in a monospace block | 60 |  |
| `oidcDiagnosticsView.idTokenError` | ID Token Error: {0} | OIDC diagnostics page · label in a monospace block | 60 | params: {0} = technical error text |
| `oidcDiagnosticsView.accessToken` | Access Token: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = masked token |
| `oidcDiagnosticsView.accessTokenUnavailable` | Access Token: Not available or resolvable | OIDC diagnostics page · label in a monospace block | 60 |  |
| `oidcDiagnosticsView.accessTokenError` | Access Token Error: {0} | OIDC diagnostics page · label in a monospace block | 60 | params: {0} = technical error text |

## English source file

```properties
common.language=Language
common.logoAlt={0} logo
common.appTitle={0} {1}
common.appTitle.default=Elicit {0}
common.appType.author=Author
common.save=Save
common.cancel=Cancel
common.close=Close
common.remove=Remove
common.restore=Restore
common.ok=OK
mainLayout.nav.surveys=Surveys
mainLayout.nav.guide=How surveys work
mainLayout.nav.manual=Manual (PDF)
mainLayout.header.manual=Manual
mainLayout.header.manualTitle=Open the author's manual (PDF) in a new tab
mainLayout.nav.importDefinition=Import definition
mainLayout.nav.logout=Logout
loginView.redirecting=Redirecting to sign-in…
logoutView.signingOut=Signing out…
unauthorizedView.title=Access Restricted
unauthorizedView.message=You are authenticated but do not have the required permissions to access this application. Please contact your administrator to request the 'elicit_author' role for the Author application.
unauthorizedView.logout=Logout
structureNavigationTree.showRemoved=Show removed
structureNavigationTree.header=Survey structure
surveysView.pageTitle=Surveys
surveysView.title=Surveys
surveysView.newSurvey=New survey
surveysView.grid.name=Name
surveysView.grid.title=Title
surveysView.grid.order=Order
surveysView.grid.lastEditedBy=Last edited by
surveysView.grid.releaseNote=Release note
surveysView.open=Open
surveysView.editDetails=Edit details
surveyEditorView.pageTitle=Survey
surveyEditorView.editDetails=Edit details
surveyEditorView.export=Export…
surveyEditorView.design=Design…
surveyEditorView.reporting=Reporting…
surveyEditorView.summary={0} — key {1}
surveyEditorView.neverExported=Never exported.
surveyEditorView.lastEditedBy=Last edited by {0}.
surveyEditorView.lastExportedBy=Last exported by {0}: {1}
surveyEditorView.validation=Validation
surveyEditorView.readyToExport=Ready to export: no findings.
surveyEditorView.issue.error=Error: {0}
surveyEditorView.issue.warning=Warning: {0}
surveyMetadataDialog.newSurvey=New survey
surveyMetadataDialog.editSurvey=Edit survey
surveyMetadataDialog.name=Name
surveyMetadataDialog.title=Title
surveyMetadataDialog.description=Description
surveyMetadataDialog.initialDisplayKey=Initial display key
surveyMetadataDialog.postSurveyUrl=Post-survey address
surveyMetadataDialog.surveyKey=Survey key
surveyMetadataDialog.nameRequired=Name is required
surveyMetadataDialog.titleRequired=Title is required
surveyMetadataDialog.created=Survey created
surveyMetadataDialog.updated=Survey updated
exportDialog.title=Export {0}
exportDialog.releaseNote=Release note
exportDialog.releaseNote.placeholder=What changed in this revision (recorded on the survey and in the file)
exportDialog.releaseNote.required=Enter a release note
exportDialog.export=Export
exportDialog.issue.error=Error: {0}
exportDialog.issue.warning=Warning: {0}
exportDialog.errorsBlock={0} error(s) block the export; fix them in the editor first.
exportDialog.warningsDoNotBlock=Warnings do not block the export.
exportDialog.retroactiveReminder=Reminder: renaming a step or section and changing a question's ontology tag apply retroactively at deployed sites (historical reports and answers are relabelled).
exportDialog.download=Download {0}
exportDialog.revisionSummary=Revision {0}: {1}
importDefinitionView.pageTitle=Import definition
importDefinitionView.title=Import definition
importDefinitionView.intro=Upload an ELICIT_SURVEY_EXPORT_V1 (.elicit) file to open it in Author as a working copy. Every survey and element key in the file is kept exactly as it is, so a later export updates the same rows at every site that already applied it. A survey whose key is already open here is refused.
importDefinitionView.imported=Imported {0}
importDefinitionView.surveyKey=Survey key {0}
importDefinitionView.fileRevision=, file revision {0}
importDefinitionView.recordsImported={0} records imported from {1}
importDefinitionView.retiredSkipped=, {0} retired record(s) skipped
importDefinitionView.openInEditor=Open in editor
importDefinitionView.importFailed=Import failed: {0}
guideView.pageTitle=How a survey is put together
guideView.title=How a survey is put together
guideView.diagram.alt=The designer board for a small survey called Family history: two step cards, About you and Each child, each holding section cards (You and Children, then Child) with question rows inside them, and a Show rule drawn from the question 'Has children' to the question 'Child count'.
guideView.surveysLink=Go to the Surveys list
guideView.intro.p1=An Elicit survey is a decision tree the respondent walks through. This page shows the pieces a survey is made of and how the designer draws them, using the same names and glyphs you will see on the board and in the structure tree.
guideView.tree.h=Elicit is a decision tree
guideView.tree.p1=A respondent moves through the survey one page at a time. What they answer decides what they see next: a question can open a section, repeat a group of questions, or change the wording of a later question. Designing a survey means designing that path.
guideView.containers.h=Containers nest
guideView.containers.p1=Everything sits inside something else. A survey holds steps, a step holds sections, and a section holds questions.
guideView.containers.step.term=Step
guideView.containers.step.desc=— a page in the respondent's progress bar. For you it is a lane on the board that groups related sections.
guideView.containers.section.term=Section
guideView.containers.section.desc=— a titled group of questions on that page. For you it is a card inside a lane.
guideView.containers.question.term=Question
guideView.containers.question.desc=— one input the respondent answers. For you it is a row inside a card, with a glyph that shows its type.
guideView.order.h=Order is the default path
guideView.order.p1=Steps run in order, sections run in order within their step, and questions run in order within their section. Unless a rule says otherwise, that display order is exactly what the respondent experiences. Dragging an element to a new position changes it.
guideView.rules.h=Rules are the arrows
guideView.rules.p1=A rule reads the answer to one upstream question, compares it with a reference value using an operator, and when the comparison holds performs an action on one downstream target: a step, a section, or a question. Written out, every rule is the same sentence:
guideView.rules.example.question=How many children?
guideView.rules.example.action=Repeat
guideView.rules.example.section=Child
guideView.rules.example.thatManyTimes=that many times
guideView.rules.example.ariaLabel=Example rule: question 'How many children?' greater than 0 repeats section 'Child' that many times
guideView.rules.show.term=Show
guideView.rules.show.desc=— the target appears only when the rule holds. Until then the respondent never sees it.
guideView.rules.repeat.term=Repeat
guideView.rules.repeat.desc=— the target section or question is repeated as many times as the answer says, for example one 'Child' section per child.
guideView.rules.text.term=Text
guideView.rules.text.desc=— the answer is written into the target's text wherever a {TOKEN|default} placeholder appears, so a later question can say 'your daughter' instead of 'your child'. The rule reaches everything inside its target: one Text rule on a step fills the token in every section and question of that step. A token is a slot name, not a question — several rules may fill the same token, each where it is used.
guideView.rules.p2=There is no Hide rule. Hiding is the same as not showing: give the element a Show rule and it stays hidden until that rule holds.
guideView.rules.p3=Operators are Boolean, Greater than, Equal, Not equal, Field exists and Contains. When several rules point at the same target, all of them must hold.
guideView.designer.h=The designer draws all of this
guideView.designer.p1=On the board, lanes are steps, cards are sections, rows are questions and arrows are rules. A card starts closed; one click opens it and selects it, a double click on a lane, card or row opens its editor, and the ⋮ menu on each element lists everything you can do to it. A breadcrumb at the top of the page shows where you are (survey, step, section, question), and the structure tree at the bottom of the drawer follows your selection. Every element carries its glyph, and the glyph is its handle: drag a glyph from the Controls panel to create an element, drag it within its container to reorder, or drag it into another container to show the same element there as well (a section in another step, a question in another section); Copy to step… in a card's menu makes a separate section instead. Drag a rule glyph onto a question to start a rule, then drag the arrowhead onto the target. Everything a drag can do, the element's menu can do too.
guideView.designing.h=Designing a survey
guideView.designing.p1=Open a survey from the Surveys list and choose Design…. The drawer on the left is your toolbox; the board on the right is the survey. Work top-down: steps, then sections, then questions, then the lists, rules and reporting tags that give them meaning.
guideView.designing.search.term=Search
guideView.designing.search.desc=— each of the Lists, Sections and Questions panels has its own search box that narrows that panel as you type: lists by name, description and option text, sections by name and description, questions by name, text and type. Handy once a survey has dozens of each.
guideView.designing.controls.term=Controls
guideView.designing.controls.desc=— the glyphs for a step, a section, a question of the chosen type, and the three rules. Drag one onto the board, or use Add step and the Add… items in the menus.
guideView.designing.lists.term=Lists
guideView.designing.lists.desc=— the lists of options a choice question (radio, combo box, multi-select, checkbox group) offers. New list… defines one with its options in order; drag a list's glyph onto a question to give it those options, or pick the list in the question's editor.
guideView.designing.sections.term=Sections
guideView.designing.sections.desc=— every section in the survey, once, with the number of steps it is mounted in. Drag one into a lane, or use Mount in step…, to show the same section in another step; it stays one section, so a change to its name or questions reaches every step.
guideView.designing.questions.term=Questions
guideView.designing.questions.desc=— every question in the survey, once, with the number of sections it appears in. Drag one into a card, or use Add to section…, to ask the same question again elsewhere; it stays one question, so a change to its text reaches every section it is in.
guideView.designing.reportingTags.term=Reporting tags
guideView.designing.reportingTags.desc=— the ⋮ menu of a question or section offers Reporting tags…; the survey's Reporting… page lists them all with the tables and columns they produce. Tagged elements show a tag badge on the board, one entry per tag.
guideView.designing.structure.term=Survey structure
guideView.designing.structure.desc=— the tree at the bottom of the drawer; click an element to select it on the board. Show removed reveals retired elements so they can be restored.
guideView.designing.p2=When the structure is in place, add rules for anything conditional, tag what the report needs, check the validation panel on the survey overview, and Export… the .elicit file for the Admin app to install.
guideView.reportingTags.h=Reporting tags
guideView.reportingTags.p1=A reporting tag names a column of the report a site builds from the answers. Tag a question and its answers fill that column; tag a section as it appears in a step with a fixed value, and every respondent who reaches it is classified by that value. Tags that share a dimension share one table of values. Open a survey's Reporting page, or the 'Reporting tags…' item on a question or section, to manage them. Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified.
guideView.identity.h=Identity and exporting
guideView.identity.p1=Every element gets a key the moment it is created and keeps it for life. Removing an element retires it rather than deleting it, so answers already recorded at a site keep their meaning. When the survey is ready you export a .elicit file; the Admin app installs it at a site the first time and updates it in place after that, matching elements by key.
guideView.legend.h=Glyph legend
guideView.legend.step=Step, numbered by its display order
guideView.legend.section=Section
guideView.legend.question=Text question — the icon changes with the question type (number, choice, date, …)
guideView.legend.showRule=Show rule
guideView.legend.repeatRule=Repeat rule
guideView.legend.textRule=Text rule
designer.guideLink=How surveys work
designer.menu.edit=Edit…
designer.menu.moveUp=Move up
designer.menu.moveDown=Move down
designer.menu.openDetails=Open details
designer.menu.reportingTags=Reporting tags…
designerPalette.aria=Palette: drag a glyph onto the board to add an element
designerPalette.questionType=Question type for new questions
designerPalette.step=Step
designerPalette.section=Section
designerPalette.question=Question
designerPalette.rule=Rule
designerPalette.roledescription=draggable palette item
designerToolbox.controls=Controls
designerToolbox.lists=Lists
designerToolbox.sections=Sections
designerToolbox.questions=Questions
designerToolbox.listsCount=Lists ({0})
designerToolbox.sectionsCount=Sections ({0})
designerToolbox.questionsCount=Questions ({0})
glyph.step=Step
glyph.stepOrder=Step {0}
glyph.section=Section
glyph.question=Question
glyph.questionOfType={0} question
glyph.rule.show=Show rule
glyph.rule.repeat=Repeat rule
glyph.rule.text=Text rule
glyph.rule=Rule
listPanel.newList=New list…
listPanel.search=Search lists
listPanel.search.aria=Search lists by name, description or option text
listPanel.roledescription=draggable list
listPanel.optionCount.one=1 option
listPanel.optionCount.many={0} options
listPanel.questionCount.one=1 question
listPanel.questionCount.many={0} questions
listPanel.menu.aria=Actions for list {0}
listPanel.empty=No lists yet. A choice question (radio, combo box, multi-select, checkbox group) needs one.
listPanel.noMatch=No list matches ''{0}''.
pickerDialog.chooseOne=Choose one
previewFields.htmlPlaceholder=<em>HTML content preview</em>
previewFields.invalidHtml=Enter valid HTML to preview it.
questionBankPanel.search=Search questions
questionBankPanel.search.aria=Search questions by name, text or type
questionBankPanel.roledescription=draggable question
questionBankPanel.uses.one=in 1 section
questionBankPanel.uses.many=in {0} sections
questionBankPanel.menu.aria=Actions for question {0}
questionBankPanel.menu.addToSection=Add to section…
questionBankPanel.empty=No questions yet. Questions you add appear here and can be reused in other sections.
questionBankPanel.noMatch=No question matches ''{0}''.
questionRow.aria=Question {0}
questionRow.aria.removed=Question {0} (removed)
questionRow.menu.aria=Actions for question {0}
questionRow.menu.alsoAddToSection=Also add to section…
questionRow.menu.addRule=Add rule…
questionRow.menu.removeFromSection=Remove from this section
sectionBankPanel.search=Search sections
sectionBankPanel.search.aria=Search sections by name or description
sectionBankPanel.roledescription=draggable section
sectionBankPanel.uses.one=in 1 step
sectionBankPanel.uses.many=in {0} steps
sectionBankPanel.menu.aria=Actions for section {0}
sectionBankPanel.menu.mountInStep=Mount in step…
sectionBankPanel.empty=No sections yet. Sections you add appear here and can be mounted in other steps.
sectionBankPanel.noMatch=No section matches ''{0}''.
sectionCard.questionCount.one=1 question
sectionCard.questionCount.many={0} questions
sectionCard.mounted=in {0} steps
sectionCard.mounted.title=This section is mounted in {0} steps; editing it changes all of them
sectionCard.menu.aria=Actions for section {0}
sectionCard.menu.addQuestion=Add question
sectionCard.menu.copyToStep=Copy to step…
sectionCard.menu.alsoMountInStep=Also mount in step…
sectionCard.menu.removeFromStep=Remove from this step
sectionCard.aria=Section {0}, {1}
sectionCard.aria.removed=Section {0}, {1}, removed
sectionCard.empty=No questions yet — drop a question glyph here or use Add question.
sectionPreview.heading=Preview
sectionPreview.aria=Preview of the selected section as a respondent sees it
sectionPreview.hint.select=Select a step, section or question on the board to see it as a respondent will.
sectionPreview.hint.noSections=Step ''{0}'' has no sections yet, so respondents would see nothing here.
sectionPreview.hint.step=Respondents see one section per page. Answers typed here are not saved.
sectionPreview.hint.section=What a respondent sees on this page. Answers typed here are not saved.
sectionPreview.note.stepRuleShown=Step ''{0}'' is shown only when a rule reveals it.
sectionPreview.page=Page {0} of {1}
sectionPreview.note.removed=Removed: respondents do not see this section.
sectionPreview.note.ruleShown=Shown only when a rule reveals it.
sectionPreview.note.noQuestions=No questions yet.
stepLane.aria=Step {0}: {1}
stepLane.sectionCount.one=1 section
stepLane.sectionCount.many={0} sections
stepLane.menu.aria=Actions for step {0}
stepLane.menu.addStepAfter=Add step after
stepLane.menu.addSection=Add section
stepLane.menu.removeStep=Remove step
stepLane.header.aria=Step {0}: {1}, {2}
stepLane.header.aria.removed=Step {0}: {1}, {2}, removed
stepLane.empty=No sections yet — drop a section glyph here or use Add section.
tagBadge.title=Reporting tags: {0}
surveyDesignerView.pageTitle=Designer
surveyDesignerView.showRemoved=Show removed
surveyDesignerView.showRules=Show rules
surveyDesignerView.showPreview=Show preview
surveyDesignerView.overview=Overview
surveyDesignerView.addStep=Add step
surveyDesignerView.breadcrumb.aria=Where you are in the survey
surveyDesignerView.noSurvey=No survey with id {0}
surveyDesignerView.emptyBoard=This survey has no steps yet. Drag the Step glyph here or use Add step.
surveyDesignerView.crumb.step=Step: {0}
surveyDesignerView.crumb.section=Section: {0}
surveyDesignerView.crumb.question=Question: {0}
surveyDesignerView.sectionAlsoInStep=Section ''{0}'' is now also in step ''{1}''
surveyDesignerView.questionAlsoInSection=Question ''{0}'' is now also in section ''{1}''
surveyDesignerView.movedStep=Moved step ''{0}''
surveyDesignerView.movedSection=Moved section ''{0}''
surveyDesignerView.movedQuestion=Moved question ''{0}''
surveyDesignerView.copySection.title=Copy section ''{0}''
surveyDesignerView.copySection.label=Into step
surveyDesignerView.mountSection.title=Mount section ''{0}''
surveyDesignerView.mountSection.label=Also in step
surveyDesignerView.questionUsesList=Question ''{0}'' now offers the list ''{1}''
surveyDesignerView.removeList.title=Remove list
surveyDesignerView.removeList.text=Remove the list ''{0}'' and its options?
surveyDesignerView.removedList=Removed list ''{0}''
surveyDesignerView.addQuestion.title=Add question ''{0}''
surveyDesignerView.addQuestion.label=To section
surveyDesignerView.kind.step=step
surveyDesignerView.kind.section=section
surveyDesignerView.kind.question=question
surveyDesignerView.removeAssignment.title=Remove from {0}
surveyDesignerView.removeAssignment.text=Remove {0} ''{1}'' from {2} ''{3}''?
surveyDesignerView.removeAssignment.last=This is its only place in the survey, so the {0} itself is removed (it can be restored from its details page).
surveyDesignerView.removeAssignment.shared=It stays in the other places it is used.
surveyDesignerView.removedAssignment=Removed {0} ''{1}'' from {2} ''{3}''
surveyDesignerView.removeStep.title=Remove step
surveyDesignerView.removeStep.text=Remove step ''{0}''?{1} It can be restored from its details page.
surveyDesignerView.removeStep.dependents=Removed with it: {0}.
surveyDesignerView.removedStep=Removed step ''{0}''
surveyDesignerView.dropArrowHint=Drop the arrow on a step, a section or a question
surveyDesignerView.ruleGone=This rule no longer exists
surveyDesignerView.ruleRepointed=Rule re-pointed
surveyDesignerView.undo=Undo
surveyDesignerView.copiedSection=Copied section ''{0}'' into step ''{1}''.
surveyDesignerView.backward.one={0} — 1 rule now points backwards (shown in red). Move the downstream target after the upstream question, or move the elements again.
surveyDesignerView.backward.many={0} — {1} rules now point backwards (shown in red). Move the downstream target after the upstream question, or move the elements again.
constraint.surveys_name_un=Another survey already uses this name
constraint.surveys_survey_key_un=A survey with this key already exists in Author
constraint.steps_survey_name_un=Another step in this survey already uses this name
constraint.steps_survey_display_order_un=Another step already has this position
constraint.sections_survey_display_order_un=Another section already has this position
constraint.select_groups_name_un=Another list in this survey already uses this name
constraint.select_items_display_text_un=This list already has an option with that text
constraint.steps_sections_display_key_un=Another assignment already uses this display key
constraint.dimensions_un=A dimension with this name already exists
constraint.ontology_un=A tag with this name already exists in this survey's namespace
constraint.metadata_un=This element already carries this tag with the same value
constraint.sqlstate.23503=This element is still referenced by another element
constraint.sqlstate.23505=A row with the same unique value already exists ({0})
elementView.pageTitle=Element
elementView.surveyOverview=Survey overview
elementView.openInDesigner=Open in designer
elementView.kind.step=step
elementView.kind.section=section
elementView.kind.question=question
elementView.badge.removed={0} (removed)
elementView.question.untitled=Question {0}
elementView.fact.displayOrder=Display order
elementView.fact.dimensionName=Dimension name
elementView.fact.description=Description
elementView.fact.type=Type
elementView.fact.text=Text
elementView.fact.required=Required
elementView.fact.tooltip=Tooltip
elementView.fact.minMax=Min / max
elementView.fact.validationText=Validation text
elementView.fact.list=List (durable id)
elementView.fact.mask=Mask
elementView.fact.placeholder=Placeholder
elementView.fact.defaultValue=Default value
elementView.fact.variant=Variant
elementView.fact.tagValue=value "{0}"
elementView.fact.reportingTags=Reporting tags
elementView.fact.elementKey=Element key
elementView.fact.durableId=Durable id
elementView.fact.removedAt=Removed at
elementView.remove.header=Remove {0}?
elementView.remove.explanation=The element is retired, not deleted: sites that apply the next export close it, respondents who already started keep it, and recorded answers stay valid.
elementView.remove.dependents=Removed together with it:
elementView.undo=Undo
elementView.removed={0} removed
elementView.restore.blocked=Cannot restore {0}: {1}
elementView.restore.header=Restore {0}?
elementView.restore.explanation=The element reopens in its former position, keeping its element key.
elementView.restore.dependents=Restored together with it:
elementView.restored=Restored
reportingView.pageTitle=Reporting tags
reportingView.heading=Reporting tags — {0}
reportingView.surveyOverview=Survey overview
reportingView.openInDesigner=Open in designer
reportingView.newTag=New tag…
reportingView.assignTag=Assign tag…
reportingView.intro=A reporting tag names a column of the site's report. Assign it to a question and the answers fill that column; assign it to a section with a fixed value and every respondent who reaches the section is classified by it. Tags on the same dimension share one table of values.
reportingView.retro=Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified when the survey is exported and applied.
reportingView.grid.tag=Tag
reportingView.grid.dimension=Dimension
reportingView.grid.ownTable=(own table)
reportingView.grid.table=Table
reportingView.grid.reportColumn=Report column
reportingView.grid.assignments=Assignments
reportingView.edit=Edit…
reportingView.removeTag.tooltip=Remove this tag
reportingView.removeTag.blocked=Remove its assignments first
reportingView.assignmentsGrid.element=Element
reportingView.assignmentsGrid.where=Where
reportingView.assignmentsGrid.appliesTo=Applies to
reportingView.assignmentsGrid.tag=Tag
reportingView.assignmentsGrid.value=Value
reportingView.removedElement=removed element
reportingView.tags=Tags
reportingView.assignments=Assignments
reportingView.impact.title=Dimensional impact
reportingView.impact.intro=When the exported survey is applied and answers arrive, the site's reporting build creates these objects in the surveyreport schema.
reportingView.impact.none=No tags yet: the survey adds nothing to the reporting schema.
reportingView.impact.sharedDimension=shared dimension "{0}"
reportingView.impact.ownTable=own table of tag "{0}"
reportingView.impact.assignmentOne={0} assignment
reportingView.impact.assignmentMany={0} assignments
reportingView.impact.table={0} — {1} — tags: {2} — {3}
reportingView.impact.dimensionTables=Dimension tables
reportingView.impact.column={0} → {1} (tag "{2}")
reportingView.impact.reportColumns=Report columns
reportingView.impact.error=Error: {0}
reportingView.impact.warning=Warning: {0}
reportingView.impact.findings=Findings
reportingView.removeTag.header=Remove tag
reportingView.removeTag.text=Remove the tag "{0}"? It has no assignments.
reportingView.removedTag=Removed tag "{0}"
reportingView.removeAssignment.header=Remove reporting tag
reportingView.removeAssignment.text=Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied.
elementTagsDialog.title=Reporting tags — {0}
elementTagsDialog.addTag=Add tag…
elementTagsDialog.empty=No reporting tags yet. Without a tag, answers to this element are not reported.
elementTagsDialog.grid.tag=Tag
elementTagsDialog.grid.appliesTo=Applies to
elementTagsDialog.grid.value=Value
elementTagsDialog.grid.reportColumn=Report column
elementTagsDialog.edit=Edit…
elementTagsDialog.removeAssignment.header=Remove reporting tag
elementTagsDialog.removeAssignment.text=Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied.
elementTagsDialog.removedTag=Removed tag "{0}"
tagAssignmentDialog.title.assign=Assign reporting tag
tagAssignmentDialog.title.edit=Edit reporting tag
tagAssignmentDialog.element=Element
tagAssignmentDialog.element.section=Section {0} — {1}
tagAssignmentDialog.element.question=Question {0} — {1}
tagAssignmentDialog.appliesTo=Applies to
tagAssignmentDialog.tag=Tag
tagAssignmentDialog.newTag=New tag…
tagAssignmentDialog.reportedValue=Reported value
tagAssignmentDialog.useAnswer=Use the respondent's answer
tagAssignmentDialog.fixedValue=Fixed value
tagAssignmentDialog.value.helper=Stored lowercased and trimmed whenever the element is answered
tagAssignmentDialog.acknowledge=I understand that answers already recorded at deployed sites are reclassified
tagAssignmentDialog.help.tag=The reporting tag. Its column in the report is filled from this element.
tagAssignmentDialog.help.value=A constant reported instead of the answer, e.g. "Female" on the section about the respondent's mother.
tagAssignmentDialog.warning=This assignment is exported already. Changing its tag or value is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on.
tagAssignmentDialog.scope.section=This section as it appears in step "{0}"
tagAssignmentDialog.scope.question=This question wherever it appears
tagAssignmentDialog.scope.questionInSection=This question only in section "{0}"
tagAssignmentDialog.impact.none=Choose a tag to see the table and column it fills.
tagAssignmentDialog.impact.text=Values go to surveyreport.{0} · report column {1}{2}
tagAssignmentDialog.impact.constant=· constant
tagAssignmentDialog.impact.fromAnswer=· from the answer
tagAssignmentDialog.error.chooseTag=Choose a tag
tagAssignmentDialog.error.fixedValue=Enter the fixed value or report the answer instead
tagAssignmentDialog.error.chooseElement=Choose an element
tagAssignmentDialog.error.chooseScope=Choose what the tag applies to
tagAssignmentDialog.updated=Updated tag "{0}"
tagAssignmentDialog.assigned=Assigned tag "{0}"
tagDialog.title.new=New reporting tag
tagDialog.title.edit=Edit reporting tag
tagDialog.tag=Tag
tagDialog.tag.helper=Letters, digits, spaces and hyphens. Becomes a column of the report.
tagDialog.dimension=Dimension
tagDialog.dimension.placeholder=None: the tag gets its own table
tagDialog.dimension.helper=Type a new name to create a dimension. Letters, digits and underscores.
tagDialog.namespace=Namespace
tagDialog.namespace.helper=Groups this survey's tags; a site keeps one row per namespace and tag
tagDialog.help.tag=The tag's name. Answers of the tagged elements are reported under a column named after it.
tagDialog.help.dimension=A shared category such as "age" or "cancer". Tags on the same dimension share one table of values; leave it empty to give this tag a table of its own.
tagDialog.acknowledge=I understand that answers already recorded at deployed sites are reclassified
tagDialog.warning=This tag is assigned. Changing its name or dimension is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on.
tagDialog.advanced=Advanced
tagDialog.error.tagRequired=Tag is required
tagDialog.created=Created tag "{0}"
tagDialog.updated=Updated tag "{0}"
tagDialog.preview.none=Choose a tag to see the table and column it produces.
tagDialog.preview.text=Values go to surveyreport.{0} · report column {1}
elementDialog.kind.step=step
elementDialog.kind.section=section
elementDialog.title.new=New {0}
elementDialog.title.edit=Edit {0}
elementDialog.name=Name
elementDialog.dimensionName=Dimension name
elementDialog.description=Description
elementDialog.acknowledge=I understand that the new name relabels reports already produced at deployed sites
elementDialog.dimensionName.helper=Reporting dimension; defaults to the name. Changing it is a reporting change, not a rename.
elementDialog.help.name=The {0}''s name. Shown to respondents and used as the report label; renaming it is retroactive.
elementDialog.help.dimensionName=The name this {0} reports under. Defaults to the name; change it to relabel reports without renaming.
elementDialog.help.description=An optional note for authors. It is not shown to respondents.
elementDialog.error.nameRequired=Name is required
elementDialog.renameWarning=Renaming is retroactive: reports already produced at deployed sites will show the new name, not only reports produced from now on.
elementDialog.placed=The {0} is placed where you dropped it; you can move it afterwards.
elementDialog.saved.created=Created {0} ''{1}''
elementDialog.saved.updated=Updated {0} ''{1}''
questionDialog.title.new=New question
questionDialog.title.edit=Edit question
questionDialog.type=Type
questionDialog.text=Question text
questionDialog.shortText=Short text
questionDialog.toolTip=Tooltip
questionDialog.required=Required
questionDialog.minimum=Minimum
questionDialog.maximum=Maximum
questionDialog.validationText=Validation message
questionDialog.selectGroup=List of options
questionDialog.newList=New list…
questionDialog.mask=Input mask
questionDialog.placeholder=Placeholder
questionDialog.defaultValue=Default value
questionDialog.sample=Sample answer
questionDialog.variant=Variant
questionDialog.shortText.helper=Shown in the tree and on the board
questionDialog.sample.helper=Previews the texts that Text rules fill with this answer
questionDialog.variant.helper=Vaadin theme variants applied to the field
questionDialog.help.type=The kind of input the respondent uses to answer.
questionDialog.help.shortText=A short machine-friendly name, shown in the tree and on the board.
questionDialog.help.text=The question as the respondent sees it. May contain {TOKEN} placeholders that TEXT rules fill in.
questionDialog.help.toolTip=Help text shown to the respondent when they hover the field.
questionDialog.help.required=The respondent must answer this before continuing.
questionDialog.help.minimum=The smallest number the respondent may enter.
questionDialog.help.maximum=The largest number the respondent may enter.
questionDialog.help.selectGroup=The list of choices the respondent picks from.
questionDialog.help.mask=A pattern that constrains what the respondent can type.
questionDialog.help.placeholder=Faint example text shown in the empty field.
questionDialog.help.defaultValue=The value the field starts with.
questionDialog.help.sample=An example answer, used only in previews. Wherever a Text rule writes this question's answer into another text, the preview shows this instead: sample "Bob" previews "Hello {PName}" as "Hello Bob". Respondents never see it.
questionDialog.help.variant=Visual style variants applied to the field: alignment, size, or layout.
questionDialog.error.typeRequired=Choose a question type
questionDialog.error.textRequired=A question needs its text
questionDialog.preview=Preview
questionDialog.preview.chooseType=Choose a type to preview the field.
questionDialog.saved.created=Question created
questionDialog.saved.updated=Question updated
relationshipDialog.title.new=New rule
relationshipDialog.title.edit=Edit rule
relationshipDialog.upstream=Reads the answer to
relationshipDialog.operator=Operator
relationshipDialog.value=Value
relationshipDialog.action=Action
relationshipDialog.targetKind=Target
relationshipDialog.target=Target element
relationshipDialog.token=Token
relationshipDialog.description=Description
relationshipDialog.defaultUpstreamValue=Default upstream value
relationshipDialog.overrideUpstreamValue=Override upstream value
relationshipDialog.removeRule=Remove rule
relationshipDialog.token.helper=Optional for Show and Repeat, required for Text. Fills {TOKEN|default} — or a phrase holding it, {Has TOKEN been|Were you} — in the target's text and in every text inside it
relationshipDialog.targetKind.step=Step
relationshipDialog.targetKind.section=Section
relationshipDialog.targetKind.question=Question
relationshipDialog.help.upstream=The question whose answer this rule checks.
relationshipDialog.help.operator=How the answer is compared to the value — equals, greater than, contains, or just whether an answer exists.
relationshipDialog.help.value=The value the answer is compared against. Leave empty for operators that only test whether an answer exists.
relationshipDialog.help.action=What the rule does when it matches: SHOW reveals the target, REPEAT instantiates it once per the answer, TEXT substitutes the answer into the target's text. A SHOW or REPEAT rule may also carry a token naming the slot the upstream answer fills in the target's texts; a TEXT rule must.
relationshipDialog.help.targetKind=Whether the rule acts on a whole step, a section, or a single question. A rule with a token on a step fills it in every section and question of that step; on a section, in every question of it.
relationshipDialog.help.target=The element the action applies to.
relationshipDialog.help.token=The slot written as {TOKEN} in the target's texts that the upstream answer fills. Any rule may carry one — a SHOW or REPEAT rule that also names a token fills it, as the runtime does — and a TEXT rule must. Pick one the survey already uses or type a new name (up to 10 characters). The same token may be filled by several rules, one per place it is used. The preview shows the upstream question's sample answer in its place.
relationshipDialog.help.description=An optional note for authors. It is not shown to respondents.
relationshipDialog.help.defaultUpstreamValue=The value to assume for the upstream answer when the respondent has not answered it yet.
relationshipDialog.help.overrideUpstreamValue=A fixed value to use for the upstream answer instead of the respondent's actual answer.
relationshipDialog.advanced=Advanced
relationshipDialog.saved.created=Rule created
relationshipDialog.saved.updated=Rule updated
relationshipDialog.removed=Rule removed
relationshipDialog.sentence=Question ''{0}'' {1}{2} → {3}{4} ''{5}''
relationshipDialog.sentence.noOperator=(operator)
relationshipDialog.sentence.noAction=(action)
selectGroupDialog.title.new=New list of options
selectGroupDialog.title.edit=Edit list of options
selectGroupDialog.name=Name
selectGroupDialog.description=Description
selectGroupDialog.dataType=Data type
selectGroupDialog.addOption=Add option
selectGroupDialog.dataType.helper=Informational; 'Text' unless the site's reporting expects otherwise
selectGroupDialog.help.name=The list's name, shown when picking options for a question. Unique within the survey.
selectGroupDialog.help.description=An optional note for authors.
selectGroupDialog.help.dataType=A label carried with the list; the Survey runtime does not interpret it.
selectGroupDialog.error.nameRequired=Name is required
selectGroupDialog.option.displayText=Display text
selectGroupDialog.option.displayText.aria=Option display text
selectGroupDialog.option.codedValue=Coded value (defaults to the text)
selectGroupDialog.option.codedValue.aria=Option coded value
selectGroupDialog.option.moveUp=Move option up
selectGroupDialog.option.moveDown=Move option down
selectGroupDialog.option.remove=Remove option
selectGroupDialog.optionsLabel=Options, in the order respondents see them
selectGroupDialog.noOptions=No options yet.
selectGroupDialog.saved.created=Created list ''{0}''
selectGroupDialog.saved.updated=Updated list ''{0}''
mainLayout.resizer.title=Drag to resize the drawer
mainLayout.resizer.aria=Resize drawer
mainLayout.smallScreen.heading=A bigger screen is needed
mainLayout.smallScreen.text=Elicit Author is built for designing surveys on a tablet or larger screen. Please switch to a wider device.
common.none=none
common.unknown=unknown
mainLayout.nav.system=System
mainLayout.nav.systemOverview=Overview
mainLayout.nav.systemDatabase=Database
mainLayout.nav.systemBranding=Branding
mainLayout.nav.systemConnections=Connections
mainLayout.nav.systemOidc=OIDC
system.grid.item=Item
system.grid.value=Value
system.grid.state=State
system.grid.detail=Detail
system.badge.ok=OK
system.badge.failed=Failed
system.badge.notChecked=Not checked
system.badge.present=Present
system.badge.absent=Absent
system.milliseconds={0} ms
systemOverviewView.pageTitle=System Overview
systemOverviewView.title=System Overview
systemOverviewView.intro=What this deployment is running, whether it is healthy, and whether every required setting is present. Settings are shown as present or absent, never by value; they are startup configuration and cannot be changed here.
systemOverviewView.running=Running
systemOverviewView.row.application=Application
systemOverviewView.row.version=Version
systemOverviewView.row.built=Built
systemOverviewView.row.profile=Profile
systemOverviewView.row.started=Started
systemOverviewView.row.uptime=Uptime
systemOverviewView.uptime.days={0} d {1} h {2} min
systemOverviewView.uptime.hours={0} h {1} min
systemOverviewView.uptime.minutes={0} min {1} s
systemOverviewView.health=Health
systemOverviewView.area.database=Database connection
systemOverviewView.area.surveySchema=Survey schema
systemOverviewView.area.surveySchema.notChecked=not checked: the connection failed
systemOverviewView.area.branding=Branding
systemOverviewView.area.connections=Connections
systemOverviewView.connectionsSummary.one={0} outbound target configured; open Connections to check it
systemOverviewView.connectionsSummary.many={0} outbound targets configured; open Connections to check them
systemOverviewView.grid.area=Area
systemOverviewView.requiredSettings=Required settings
systemOverviewView.grid.setting=Setting
systemOverviewView.grid.suppliedBy=Supplied by
systemOverviewView.grid.purpose=Purpose
systemOverviewView.setting.datasourcePassword=password of the database user Author connects as
systemOverviewView.setting.oidcSecret=client secret registered with the identity provider
systemDatabaseView.pageTitle=System Database
systemDatabaseView.title=Database
systemDatabaseView.intro=Author owns no migrations. The survey schema in its database is created by the Survey preview instance that shares the database, so a missing schema means that instance has not started against this database yet.
systemDatabaseView.connection=Connection
systemDatabaseView.row.state=State
systemDatabaseView.row.configuredUser=Configured user
systemDatabaseView.row.connectedAs=Connected as
systemDatabaseView.row.database=Database
systemDatabaseView.row.server=Server
systemDatabaseView.row.roundTrip=Round trip
systemDatabaseView.surveySchema=Survey schema
systemDatabaseView.row.schemaPresent=Schema present
systemDatabaseView.row.surveyMigration=Survey migration (survey.flyway_history)
systemDatabaseView.migration.notInstalled=not installed
systemDatabaseView.migration.empty=empty
systemDatabaseView.migration.applied={0} applied {1}
systemDatabaseView.migration.failed={0} FAILED {1}
systemDatabaseView.migration.unreadable=present, but not readable by {0}: {1}
systemDatabaseView.row.durableSequences=Kimball durable-key sequences
systemDatabaseView.row.surveysAuthored=Surveys authored
systemDatabaseView.schema.exists=survey.surveys exists
systemDatabaseView.schema.missing=survey.surveys does not exist: the Survey preview instance has not created the schema in this database yet
systemDatabaseView.durableSequences.exists=survey.{0} exists
systemDatabaseView.durableSequences.missing=survey.{0} is missing: the schema in this database predates the V3 migrations
systemBrandingView.pageTitle=System Branding
systemBrandingView.title=Branding
systemBrandingView.intro=The brand resolves from the mounted directory first, then the local directory, then the default packaged with the application. Each asset is listed on its own, because a partial mount renders with the wrong fonts and no error.
systemBrandingView.reload=Reload brand
systemBrandingView.reloaded=Brand cache discarded; the result below is freshly resolved.
systemBrandingView.resolution=Resolution
systemBrandingView.row.configuredPath=Configured path (brand.file.system.path)
systemBrandingView.row.localPath=Local path (brand.local.path)
systemBrandingView.path.exists={0} (exists)
systemBrandingView.path.notFound={0} (not found)
systemBrandingView.row.metadataFile=Metadata file
systemBrandingView.row.brandName=Brand name
systemBrandingView.row.organization=Organization
systemBrandingView.row.brandVersion=Brand version
systemBrandingView.row.inUse=In use (cached)
systemBrandingView.inUse={0} [{1}]
systemBrandingView.row.summary=Summary
systemBrandingView.summary.mounted=Mounted brand at {0}
systemBrandingView.summary.mountedNamed=Mounted brand: {0} at {1}
systemBrandingView.summary.local=Local brand directory {0}
systemBrandingView.summary.embedded=Embedded default theme; no brand directory at {0}
systemBrandingView.assets=Assets
systemBrandingView.grid.asset=Asset
systemBrandingView.grid.path=Path
systemBrandingView.grid.source=Source
systemBrandingView.grid.location=Location
systemBrandingView.source.mounted=Mounted
systemBrandingView.source.local=Local
systemBrandingView.source.embedded=Embedded default
systemBrandingView.source.absent=Absent
systemBrandingView.source.unreadable=Unreadable
systemBrandingView.asset.colourStylesheet=colour stylesheet
systemBrandingView.asset.typographyStylesheet=typography stylesheet
systemBrandingView.asset.themeStylesheet=theme stylesheet
systemBrandingView.asset.horizontalLogo=horizontal logo
systemBrandingView.asset.headerIcon=header icon
systemBrandingView.asset.favicon=favicon
systemBrandingView.asset.notFound=not found in any location
systemBrandingView.asset.unreadable={0} exists but cannot be read
systemBrandingView.logoPreview=Logo preview
systemBrandingView.logoAlt=Brand logo
systemConnectionsView.pageTitle=System Connections
systemConnectionsView.title=Connections
systemConnectionsView.intro=Each outbound dependency Author will call, read from the same settings the runtime uses. A check sends one read-only request and gives up after {0} seconds. Author''s only other dependency is the database, checked on its own page.
systemConnectionsView.empty=No outbound targets are configured.
systemConnectionsView.checkAll=Check all
systemConnectionsView.check=Check
systemConnectionsView.grid.dependency=Dependency
systemConnectionsView.grid.source=Source
systemConnectionsView.grid.address=Address
systemConnectionsView.grid.time=Time
systemConnectionsView.group.identityProvider=Identity provider
systemConnectionsView.group.telemetryCollector=Telemetry collector
systemConnectionsView.result.timedOut=timed out after {0} s
systemConnectionsView.result.interrupted=interrupted
systemConnectionsView.result.discoveryServed=discovery document served at {0}
systemConnectionsView.result.notDiscovery=HTTP {0} from {1} is not an OIDC discovery document; check the realm address
systemConnectionsView.result.reachable=reachable, HTTP {0}
systemConnectionsView.result.noHostPort=address {0} has no host and port
systemConnectionsView.result.connected=connected to {0}:{1}
oidcDiagnosticsView.pageTitle=OIDC Diagnostics
oidcDiagnosticsView.title=OIDC
oidcDiagnosticsView.intro=The identity the provider handed this session: who signed in, which roles arrived, and whether the authoring role is among them. Tokens are masked.
oidcDiagnosticsView.user=User: {0}
oidcDiagnosticsView.isAnonymous=Is Anonymous: {0}
oidcDiagnosticsView.roles=Roles: {0}
oidcDiagnosticsView.roleSource=Role Source: {0}
oidcDiagnosticsView.hasRole=Has {0}: {1}
oidcDiagnosticsView.issuer=Issuer: {0}
oidcDiagnosticsView.email=Email: {0}
oidcDiagnosticsView.idToken=ID Token: {0}
oidcDiagnosticsView.idTokenUnavailable=ID Token: Not available or resolvable
oidcDiagnosticsView.idTokenError=ID Token Error: {0}
oidcDiagnosticsView.accessToken=Access Token: {0}
oidcDiagnosticsView.accessTokenUnavailable=Access Token: Not available or resolvable
oidcDiagnosticsView.accessTokenError=Access Token Error: {0}
```
