package com.elicitsoftware.e2e.multilingual;

import com.elicitsoftware.e2e.survey.SectionPage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code samples/census-household-survey.elicit} as this journey drives it: where every question
 * sits, how a respondent answers it, and what a respondent should read in each language.
 *
 * <p>Structure (six steps, eleven sections, twenty-five questions, twelve rules -- see
 * {@code samples/README.md}):</p>
 * <pre>
 * 1 Welcome              1 Introduction        HTML intro, CHECKBOX consent
 * 2 About You            1 About you           TEXT, INTEGER, RADIO, COMBOBOX
 *                        2 Race and language   CHECKBOX_GROUP, TEXT (shown by CONTAINS 'OTHER'), MULTI_SELECT
 * 3 Your Home            1 Housing             RADIO tenure, DATE_PICKER, CHECKBOX (shown by NOT_EQUAL 'OWN')
 *                        2 Rent details        DOUBLE          (section shown by EQUAL 'RENT')
 *                        3 Vehicles            INTEGER count
 *                        4 Vehicle             TEXT            (section REPEATed per vehicle)
 * 4 Household Members    1 Household members   INTEGER size, TEXT person name (question REPEATed per person)
 * 5 {name|this person}   1 {name|this person}  INTEGER, RADIO, COMBOBOX (step shown once per person name)
 * 6 Finishing Up         1 Contact             EMAIL, TIME_PICKER, DATE_TIME_PICKER
 *                        2 Anything else       TEXTAREA, MODAL thank-you
 * </pre>
 *
 * <p>Steps 2, 3, 4 and 6 are gated on the consent checkbox, so a respondent's first page is the
 * Welcome step alone; step 5 appears once per name typed into the REPEATed person question.</p>
 *
 * <p>Display keys (Survey {@code DisplayKey}: survey-step-stepInstance-section-sectionInstance-
 * question-questionInstance) start with the survey id, which differs per site and per run, so the
 * helpers here match fields by the key's suffix.</p>
 */
final class CensusHouseholdSurvey {

    static final String NAME = "Census Household Survey";

    /** The race question's short text, which is how the Author designer labels its row. */
    static final String RACE_ROW = "Race";

    /** The question the journey rewords and reads back, in both revisions. */
    static final String Q_RACE_V1 = "Which of the following describe you? Select all that apply.";
    static final String Q_RACE_V2 = "What race do you consider yourself to be? Select all that apply.";

    /** How many vehicles and how many other household members every respondent reports. */
    static final int VEHICLES = 2;
    static final int PEOPLE = 2;

    // ---- the rule this suite's copy of the definition breaks on purpose ------------------------
    // See census-household-survey.elicit in this directory: relationship 3, the SHOW rule that
    // reveals the Rent details section, arrives pointing at the About you section of step 2
    // instead, so phase 1 has something real to find and fix in the designer (Author UC-033).

    /** Board labels of the step and the section the rent rule reads from. */
    static final String STEP_YOUR_HOME = "Your Home";
    static final String SECTION_HOUSING = "Housing";
    /** The section the rule is meant to reveal, and the one the broken copy points it at. */
    static final String SECTION_RENT_DETAILS = "Rent details";
    static final String SECTION_ABOUT_YOU = "About you";
    /** The rule's description, which is how Author's validation panel names it. */
    static final String RULE_RENT_DETAILS = "Show rent details when renting";

    private CensusHouseholdSurvey() {
    }

    /** A display-key suffix: everything after the survey-id group. */
    static String suffix(int step, int stepInstance, int section, int sectionInstance, int question, int questionInstance) {
        return String.format("-%04d-%04d-%04d-%04d-%04d-%04d",
                step, stepInstance, section, sectionInstance, question, questionInstance);
    }

    // ---- step 1: Welcome / Introduction --------------------------------------------------------
    static String welcomeHtml() {
        return suffix(1, 0, 1, 0, 1, 0);
    }

    static String consent() {
        return suffix(1, 0, 1, 0, 2, 0);
    }

    // ---- step 2: About You ---------------------------------------------------------------------
    static String fullName() {
        return suffix(2, 0, 1, 0, 1, 0);
    }

    static String age() {
        return suffix(2, 0, 1, 0, 2, 0);
    }

    static String gender() {
        return suffix(2, 0, 1, 0, 3, 0);
    }

    static String maritalStatus() {
        return suffix(2, 0, 1, 0, 4, 0);
    }

    static String race() {
        return suffix(2, 0, 2, 0, 1, 0);
    }

    static String otherRace() {
        return suffix(2, 0, 2, 0, 2, 0);
    }

    static String languages() {
        return suffix(2, 0, 2, 0, 3, 0);
    }

    // ---- step 3: Your Home ---------------------------------------------------------------------
    static String tenure() {
        return suffix(3, 0, 1, 0, 1, 0);
    }

    static String moveIn() {
        return suffix(3, 0, 1, 0, 2, 0);
    }

    static String subsidized() {
        return suffix(3, 0, 1, 0, 3, 0);
    }

    static String monthlyRent() {
        return suffix(3, 0, 2, 0, 1, 0);
    }

    static String vehicleCount() {
        return suffix(3, 0, 3, 0, 1, 0);
    }

    /** The REPEATed Vehicle section, instance {@code vehicle} (1-based). */
    static String vehicle(int vehicle) {
        return suffix(3, 0, 4, vehicle, 1, 0);
    }

    // ---- step 4: Household Members -------------------------------------------------------------
    static String householdSize() {
        return suffix(4, 0, 1, 0, 1, 0);
    }

    /** The REPEATed person-name question, instance {@code person} (1-based). */
    static String personName(int person) {
        return suffix(4, 0, 1, 0, 2, person);
    }

    // ---- step 5: one instance per person -------------------------------------------------------
    static String personAge(int person) {
        return suffix(5, person, 1, 0, 1, 0);
    }

    static String personGender(int person) {
        return suffix(5, person, 1, 0, 2, 0);
    }

    static String personRelationship(int person) {
        return suffix(5, person, 1, 0, 3, 0);
    }

    // ---- step 6: Finishing Up ------------------------------------------------------------------
    static String email() {
        return suffix(6, 0, 1, 0, 1, 0);
    }

    static String departureTime() {
        return suffix(6, 0, 1, 0, 2, 0);
    }

    static String followUp() {
        return suffix(6, 0, 1, 0, 3, 0);
    }

    static String comments() {
        return suffix(6, 0, 2, 0, 1, 0);
    }

    static String thanksModal() {
        return suffix(6, 0, 2, 0, 2, 0);
    }

    /**
     * One respondent's answers. Everything is deterministic and keyed off {@code label}, so the
     * nine respondents of a run differ from each other in the data an export carries, while the
     * shape of every walk is identical.
     */
    record Answers(String label) {

        String ownName() {
            return "Subject " + label.toUpperCase();
        }

        String emailAddress() {
            return label + "@example.org";
        }

        String otherRaceText() {
            return "Cornish (" + label + ")";
        }

        String vehicleText(int vehicle) {
            return (vehicle == 1 ? "Ford F-150" : "Toyota Corolla") + " " + label;
        }

        String personText(int person) {
            return (person == 1 ? "Alice" : "Bob") + " " + label.toUpperCase();
        }

        int personAgeValue(int person) {
            return person == 1 ? 41 : 12;
        }

        /** Positions in the Gender list: person 1 Female (1), person 2 Male (0). */
        int personGenderOption(int person) {
            return person == 1 ? 1 : 0;
        }

        /** Positions in the Relationship list: person 1 Spouse (0), person 2 Child (1). */
        int personRelationshipOption(int person) {
            return person == 1 ? 0 : 1;
        }

        String commentsText() {
            return "Nothing further from " + label + ".";
        }
    }

    /**
     * Answers every question of the survey that is on the section currently shown, in one pass, and
     * returns what it answered (display-key suffixes) so a caller can tell an empty pass from a
     * useful one.
     *
     * <p>Which questions are present depends on the rules: consent builds steps 2, 3, 4 and 6;
     * {@code CONTAINS 'OTHER'} reveals the other-race question; {@code EQUAL 'RENT'} reveals the
     * Rent details section and {@code NOT_EQUAL 'OWN'} the subsidy question; the vehicle count
     * REPEATs the Vehicle section and the household size REPEATs the person-name question, each of
     * which then SHOWs one instance of step 5. So this is driven by what is on the page rather than
     * by a fixed script.</p>
     */
    static List<String> answerWhatIsShown(SectionPage section, Answers a, Set<String> done) {
        List<String> answered = new ArrayList<>();

        // Step 1. Consent gates everything else, so it is answered first and on its own pass.
        if (shown(section, done, consent())) {
            section.fillById(section.keyEndingWith(consent()), "true");
            record(done, answered, consent());
        }

        // Step 2, About you.
        if (shown(section, done, fullName())) {
            section.fillById(section.keyEndingWith(fullName()), a.ownName());
            section.fillById(section.keyEndingWith(age()), "42");
            section.chooseOptionsAt(section.keyEndingWith(gender()), 0);          // Male
            section.chooseOptionsAt(section.keyEndingWith(maritalStatus()), 1);   // Married
            record(done, answered, fullName());
        }
        // Step 2, Race and language. White (0) and "Some other race or origin" (5); the second is
        // coded OTHER, which is what reveals the other-race question in this same section.
        if (shown(section, done, race())) {
            section.chooseOptionsAt(section.keyEndingWith(race()), 0, 5);
            record(done, answered, race());
        }
        if (shown(section, done, otherRace())) {
            section.fillById(section.keyEndingWith(otherRace()), a.otherRaceText());
            record(done, answered, otherRace());
        }
        if (shown(section, done, languages())) {
            section.chooseOptionsAt(section.keyEndingWith(languages()), 0, 1);    // English, Spanish
            record(done, answered, languages());
        }

        // Step 3, Housing. Rented (1) reveals the Rent details section and the subsidy question.
        if (shown(section, done, tenure())) {
            section.chooseOptionsAt(section.keyEndingWith(tenure()), 1);
            section.fillById(section.keyEndingWith(moveIn()), "2019-01-15");
            record(done, answered, tenure());
        }
        if (shown(section, done, subsidized())) {
            section.fillById(section.keyEndingWith(subsidized()), "true");
            record(done, answered, subsidized());
        }
        if (shown(section, done, monthlyRent())) {
            section.fillById(section.keyEndingWith(monthlyRent()), "1200.50");
            record(done, answered, monthlyRent());
        }
        if (shown(section, done, vehicleCount())) {
            section.fillById(section.keyEndingWith(vehicleCount()), String.valueOf(VEHICLES));
            record(done, answered, vehicleCount());
        }
        for (int v = 1; v <= VEHICLES; v++) {
            if (shown(section, done, vehicle(v))) {
                section.fillById(section.keyEndingWith(vehicle(v)), a.vehicleText(v));
                record(done, answered, vehicle(v));
            }
        }

        // Step 4. The size REPEATs the name question, so the names are answered on a later pass
        // over the same section.
        if (shown(section, done, householdSize())) {
            section.fillById(section.keyEndingWith(householdSize()), String.valueOf(PEOPLE));
            record(done, answered, householdSize());
        }
        for (int p = 1; p <= PEOPLE; p++) {
            if (shown(section, done, personName(p))) {
                section.fillById(section.keyEndingWith(personName(p)), a.personText(p));
                record(done, answered, personName(p));
            }
        }

        // Step 5, one instance per person.
        for (int p = 1; p <= PEOPLE; p++) {
            if (shown(section, done, personAge(p))) {
                section.fillById(section.keyEndingWith(personAge(p)), String.valueOf(a.personAgeValue(p)));
                section.chooseOptionsAt(section.keyEndingWith(personGender(p)), a.personGenderOption(p));
                section.chooseOptionsAt(section.keyEndingWith(personRelationship(p)), a.personRelationshipOption(p));
                record(done, answered, personAge(p));
            }
        }

        // Step 6.
        if (shown(section, done, email())) {
            section.fillById(section.keyEndingWith(email()), a.emailAddress());
            section.fillById(section.keyEndingWith(departureTime()), "08:30");
            section.fillById(section.keyEndingWith(followUp()), "2026-10-01T09:00");
            record(done, answered, email());
        }
        if (shown(section, done, comments())) {
            section.fillById(section.keyEndingWith(comments()), a.commentsText());
            record(done, answered, comments());
        }
        return answered;
    }

    /** True when the question is on the section and this walk has not answered it yet. */
    private static boolean shown(SectionPage section, Set<String> done, String keySuffix) {
        return !done.contains(keySuffix) && section.hasKeyEndingWith(keySuffix);
    }

    private static void record(Set<String> done, List<String> answered, String keySuffix) {
        done.add(keySuffix);
        answered.add(keySuffix);
    }

    /**
     * Walks the survey from wherever the respondent currently is to the review page.
     *
     * <p>One pass answers every question of the survey that is on the section and not yet answered
     * in this walk. A pass that answered something is followed by another pass over the <em>same</em>
     * section, because an answer can add fields to it -- the household size REPEATs the person-name
     * question, the vehicle count REPEATs the Vehicle section, the race answer reveals the
     * other-race question. Only a pass that answers nothing advances. That is what makes the walk
     * independent of the order the rules fire in.</p>
     *
     * @param section the section page, already showing a section
     * @param a       the respondent's answers
     * @param done    the display-key suffixes this walk has already answered; grows as it goes
     */
    static void walkToReview(SectionPage section, Answers a, Set<String> done) {
        for (int pass = 0; pass < 60; pass++) {
            // Before answering, not after: the MODAL thank-you opens itself the moment the last
            // section attaches and covers that section's own fields and buttons, so a pass that
            // tried to answer first would wait forever on a field it cannot reach.
            section.closeModalQuestions();
            int before = done.size();
            answerWhatIsShown(section, a, done);
            if (done.size() > before) {
                continue;
            }
            if (section.onLastSection()) {
                section.review();
                return;
            }
            section.next();
        }
        throw new IllegalStateException("The survey did not reach review within 60 passes; last fields "
                + section.fieldIds());
    }

    static void walkToReview(SectionPage section, Answers a) {
        walkToReview(section, a, new HashSet<>());
    }

    /**
     * Starts the survey and stops on the Race and language section -- consent, then About you, then
     * one step forward. This is how the journey's second respondent at each site leaves the survey
     * unfinished after seeing the race question in the revision it was published under.
     *
     * @return the suffixes answered, so a later resume does not answer them twice
     */
    static Set<String> startAndReachRaceSection(SectionPage section, Answers a) {
        Set<String> done = new HashSet<>();
        for (int pass = 0; pass < 20; pass++) {
            if (section.hasKeyEndingWith(race())) {
                return done;
            }
            int before = done.size();
            answerWhatIsShown(section, a, done);
            if (done.size() > before) {
                continue;
            }
            section.next();
        }
        throw new IllegalStateException("The Race and language section was not reached; last fields "
                + section.fieldIds());
    }

    /**
     * Asserts the respondent is reading the race question in the expected wording, and returns its
     * label. {@code expected} is the wording in the respondent's own language.
     */
    static String assertRaceQuestionReads(SectionPage section, String expected) {
        String key = section.keyEndingWith(race());
        String label = section.labelOf(key);
        assertEquals(expected, label, "the race question's wording");
        return label;
    }

    /** Asserts every option of the race question reads as {@code expected} does, in order. */
    static void assertRaceOptionsRead(SectionPage section, List<String> expected) {
        List<String> shown = section.optionLabels(section.keyEndingWith(race()));
        assertEquals(expected, shown, "the race question's options");
    }

    /** The review page's section titles a finished respondent must see, in the given language. */
    static void assertReviewLists(List<String> titles, List<String> expectedFragments) {
        for (String fragment : expectedFragments) {
            assertTrue(titles.stream().anyMatch(t -> t.contains(fragment)),
                    "the review page should list a section containing '" + fragment + "' but lists " + titles);
        }
    }
}
