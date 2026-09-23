package com.elicitsoftware.e2e.multisite;

import com.elicitsoftware.e2e.survey.SectionPage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Household Survey the journey authors, and the deterministic way a respondent answers it.
 *
 * <p>Structure (Author UC-011/015/016/019, following the FHHS sibling pattern because the
 * Survey runtime cannot repeat a whole step): Step 1 "The house" asks how many people live in
 * the house and repeats "Name of person {Q#}" once per person (REPEAT rule); each name SHOWs
 * Step 2 "Household member" once, carrying the name as token {@code NAME} into the section
 * title "About {NAME|this person}" and the question texts. Revision 2 renames the sex question
 * to gender and its options to Woman/Man.</p>
 *
 * <p>Display keys (Survey {@code DisplayKey}: survey-step-stepInstance-section-sectionInstance-
 * question-questionInstance) start with the survey id, which differs per site and per run, so
 * the helpers match fields by the key's suffix.</p>
 */
final class HouseholdSurvey {

    static final String NAME = "Household Survey";
    static final String TITLE = "Household Survey";
    static final String STEP1 = "The house";
    static final String STEP1_SECTION = "The house";
    static final String Q_COUNT = "How many people live in your house?";
    static final String Q_NAME = "Name of person {Q#}";
    static final String STEP2 = "Household member";
    static final String STEP2_SECTION = "About {NAME|this person}";
    static final String Q_AGE = "How old is {NAME|this person}?";
    static final String Q_SEX_V1 = "What is {NAME|this person}'s sex?";
    static final String Q_SEX_V2 = "What is {NAME|this person}'s gender?";
    static final String Q_RELATION = "How is {NAME|this person} related to you?";
    static final String Q_CHILDREN = "How many children does {NAME|this person} have?";
    static final String SEX_LIST = "Sex";
    static final List<String> SEX_OPTIONS_V1 = List.of("Female", "Male");
    static final List<String> SEX_OPTIONS_V2 = List.of("Woman", "Man");

    /** The two household members every respondent reports. */
    record Person(String name, int age, int sexOption, String relation, int children) { }

    static final List<Person> PEOPLE = List.of(
            new Person("Alice", 41, 0, "spouse", 2),
            new Person("Bob", 12, 1, "son", 0));

    private HouseholdSurvey() { }

    /** A display-key suffix (everything after the survey-id group): step-stepInstance-section-sectionInstance-question-questionInstance. */
    static String suffix(int step, int stepInstance, int section, int sectionInstance, int question, int questionInstance) {
        return String.format("-%04d-%04d-%04d-%04d-%04d-%04d", step, stepInstance, section, sectionInstance, question, questionInstance);
    }

    static String countSuffix() {
        return suffix(1, 0, 1, 0, 1, 0);
    }

    static String nameSuffix(int person) {
        return suffix(1, 0, 1, 0, 2, person);
    }

    /** Step 2, instance {@code person} (1-based), question {@code q} (1 age, 2 sex, 3 relation, 4 children). */
    static String memberSuffix(int person, int q) {
        return suffix(2, person, 1, 0, q, 0);
    }

    /** Answers Step 1 (the count and both names) on the section the page is currently showing. */
    static void answerStep1(SectionPage section) {
        section.fillById(section.keyEndingWith(countSuffix()), String.valueOf(PEOPLE.size()));
        for (int i = 0; i < PEOPLE.size(); i++) {
            section.fillById(section.keyEndingWith(nameSuffix(i + 1)), PEOPLE.get(i).name());
        }
        assertEquals(PEOPLE.get(0).name(), section.valueOf(section.keyEndingWith(nameSuffix(1))), "first name should be saved");
    }

    /**
     * Answers the Step 2 instance for person {@code person} (1-based) that the page currently
     * shows, asserting the title carries the name and the sex/gender label matches
     * {@code expectSexWord} ("sex" for revision 1, "gender" for revision 2). Options are chosen
     * by position so both option lists work.
     */
    static void answerMember(SectionPage section, int person, String expectSexWord, List<String> options) {
        Person p = PEOPLE.get(person - 1);
        assertOnMember(section, person);
        String sexLabel = section.labelOf(section.keyEndingWith(memberSuffix(person, 2)));
        assertTrue(sexLabel.toLowerCase().contains(expectSexWord),
                "expected the sex/gender question to read '" + expectSexWord + "' but was: " + sexLabel);
        section.fillById(section.keyEndingWith(memberSuffix(person, 1)), String.valueOf(p.age()));
        section.fillById(section.keyEndingWith(memberSuffix(person, 2)), options.get(p.sexOption()));
        section.fillById(section.keyEndingWith(memberSuffix(person, 3)), p.relation());
        section.fillById(section.keyEndingWith(memberSuffix(person, 4)), String.valueOf(p.children()));
    }

    /** Asserts the section shown is Step 2 for {@code person}, with the name filled into its texts by the NAME token. */
    static void assertOnMember(SectionPage section, int person) {
        Person p = PEOPLE.get(person - 1);
        String ageLabel = section.labelOf(section.keyEndingWith(memberSuffix(person, 1)));
        assertEquals("How old is " + p.name() + "?", ageLabel, "the member step should carry the person's name");
    }

    /** The section title the review page must list for {@code person}. */
    static String reviewTitle(int person) {
        return "About " + PEOPLE.get(person - 1).name() + ":";
    }

    /** True when the section currently shown is Step 2 for {@code person}. */
    static boolean onMember(SectionPage section, int person) {
        return section.hasKeyEndingWith(memberSuffix(person, 1));
    }
}
