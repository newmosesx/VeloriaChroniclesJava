package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;
import example.practice.story.Council;
import example.practice.story.CouncilMember;
import example.practice.story.DialogueToken;
import example.practice.story.Disposition;

import java.util.ArrayList;
import java.util.List;

// Public API (chosenResponse, Choice, hasChoices, getChoices, handleTimeout) is
// unchanged so MainGUI keeps working. What changed: each choice now speaks a
// DialogueToken to the council, and the kingdom-level fallout (unrest, morale)
// is DERIVED from how the council feels afterward - not flat magic numbers.
public class DebateManager {

    public static String chosenResponse = null;

    private static Council council = Council.imperialCouncil();
    public static Council getCouncil() { return council; }
    public static void resetCouncil() { council = Council.imperialCouncil(); }

    public static class Choice {
        public String text;
        public Runnable action;
        public Choice(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }
    }

    public static boolean hasChoices(int chapterIndex, int paragraphIndex) {
        return chapterIndex == 8 && paragraphIndex == 18;
    }

    public static List<Choice> getChoices(int chapterIndex, int paragraphIndex, SimulationEngine engine) {
        List<Choice> choices = new ArrayList<>();
        List<Human> pop = engine.getWorldPopulation();

        if (!(chapterIndex == 8 && paragraphIndex == 18)) return choices;

        choices.add(new Choice("1. \"I was securing the future!\" (Authority)", () -> {
            council.applyToAll(DialogueToken.AUTHORITY);
            resolveCouncilOutcome(engine, "I was securing the future! We did what was necessary.");
        }));

        choices.add(new Choice("2. \"We must unite in this crisis.\" (Diplomacy)", () -> {
            council.applyToAll(DialogueToken.DIPLOMACY);
            engine.getKingdoms()[0].gold -= 25000; // funds diverted to public aid
            resolveCouncilOutcome(engine, "We must unite in this crisis. Let funds be diverted to the people.");
        }));

        choices.add(new Choice("3. \"Silence! Guards, arrest this fool!\" (Tyranny)", () -> {
            council.applyToAll(DialogueToken.TYRANNY);
            Kingdom empire = engine.getKingdoms()[0];
            empire.limitersDisabled = true;          // the edict still removes the safety rails
            empire.triggerMassDesertion(pop, 0.10);  // 10% of troops defect
            resolveCouncilOutcome(engine, "Silence! Guards, arrest this fool immediately!");
        }));

        return choices;
    }

    public static void handleTimeout(int chapterIndex, int paragraphIndex, SimulationEngine engine) {
        if (chapterIndex == 8 && paragraphIndex == 18) {
            council.applyToAll(DialogueToken.SILENCE);
            resolveCouncilOutcome(engine, "... (The Emperor stammers in silence)");
        }
    }

    // The micro -> macro bridge. Unrest and morale now come from the council's
    // mood, so the same choice can play out very differently depending on the
    // relationships you've built (or burned) up to this point.
    private static void resolveCouncilOutcome(SimulationEngine engine, String response) {
        chosenResponse = response;
        Kingdom empire = engine.getKingdoms()[0];

        int unrestSwing = council.aggregateUnrestSwing();
        empire.unrestLevel += unrestSwing;
        if (empire.unrestLevel < 0) empire.unrestLevel = 0;

        int loyal = council.countOf(Disposition.LOYAL);
        int hostile = council.countOf(Disposition.HOSTILE);
        empire.modifyMorale(loyal * 6 - hostile * 9);

        StringBuilder sb = new StringBuilder("Council reacts -> ");
        for (CouncilMember m : council.members) {
            sb.append(m.name).append(": ").append(m.disposition());
            if (m.locked) sb.append(" (locked)");
            sb.append("   ");
        }
        Logger.logEvent(sb.toString().trim(), Logger.LogCategory.POLITICAL);
        Logger.logEvent("Chamber unrest swing: " + (unrestSwing >= 0 ? "+" : "") + unrestSwing, Logger.LogCategory.POLITICAL);
    }
}