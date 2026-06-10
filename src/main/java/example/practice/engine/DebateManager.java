package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;
import example.practice.story.Council;
import example.practice.story.CouncilMember;
import example.practice.story.DialogueToken;
import example.practice.story.Disposition;
import example.practice.story.StoryData;
import example.practice.story.StoryRouter;

import java.util.ArrayList;
import java.util.List;

// Public API (chosenResponse, Choice, hasChoices, getChoices, handleTimeout) is
// unchanged so MainGUI keeps working. What changed: choices are no longer
// hardcoded to chapter 8 - they come from story.json (any chapter, any
// paragraph, any number of routes). Each choice still speaks a DialogueToken to
// the council, and the kingdom-level fallout is DERIVED from how the council
// feels afterward - the micro -> macro bridge is untouched.
//
// EFFECTS vocabulary (the "effects" array on a choice in story.json):
//   "gold:-25000"     adjust the empire's treasury (negative = spend)
//   "morale:-10"      adjust army morale
//   "limitersOff"     remove the safety rails (limitersDisabled = true)
//   "desertion:0.10"  that fraction of troops defects
// Unknown effects are logged and skipped, never fatal - so a typo in the JSON
// can't crash the game mid-debate.
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
        StoryData.Chapter ch = StoryData.chapterAt(chapterIndex);
        return ch != null && ch.choiceAt == paragraphIndex && !ch.choices.isEmpty();
    }

    public static List<Choice> getChoices(int chapterIndex, int paragraphIndex, SimulationEngine engine) {
        List<Choice> out = new ArrayList<>();
        StoryData.Chapter ch = StoryData.chapterAt(chapterIndex);
        if (ch == null || ch.choiceAt != paragraphIndex) return out;

        for (StoryData.ChoiceSpec spec : ch.choices) {
            out.add(new Choice(spec.label, () -> {
                speak(spec.token);
                applyEffects(engine, spec.effects);
                if (spec.gotoId != null) StoryRouter.jumpTo(spec.gotoId);
                resolveCouncilOutcome(engine, spec.response);
            }));
        }
        return out;
    }

    public static void handleTimeout(int chapterIndex, int paragraphIndex, SimulationEngine engine) {
        StoryData.Chapter ch = StoryData.chapterAt(chapterIndex);
        if (ch == null || ch.choiceAt != paragraphIndex) return;

        StoryData.TimeoutSpec t = ch.timeout != null ? ch.timeout : new StoryData.TimeoutSpec();
        speak(t.token);
        if (t.gotoId != null) StoryRouter.jumpTo(t.gotoId);
        resolveCouncilOutcome(engine, t.response);
    }

    // ------------------------------------------------------------- internals
    private static void speak(String tokenName) {
        if (tokenName == null) return;
        try {
            council.applyToAll(DialogueToken.valueOf(tokenName));
        } catch (IllegalArgumentException ex) {
            Logger.logEvent("story.json: unknown DialogueToken '" + tokenName
                    + "' - the council hears nothing.", Logger.LogCategory.POLITICAL);
        }
    }

    private static void applyEffects(SimulationEngine engine, List<String> effects) {
        if (effects == null) return;
        Kingdom empire = engine.getKingdoms()[0];
        List<Human> pop = engine.getWorldPopulation();
        for (String e : effects) {
            try {
                String[] f = e.split(":", 2);
                switch (f[0].trim()) {
                    case "gold":        empire.gold += Integer.parseInt(f[1].trim()); break;
                    case "morale":      empire.modifyMorale(Integer.parseInt(f[1].trim())); break;
                    case "limitersOff": empire.limitersDisabled = true; break;
                    case "desertion":   empire.triggerMassDesertion(pop, Double.parseDouble(f[1].trim())); break;
                    default:
                        Logger.logEvent("story.json: unknown effect '" + e + "' - skipped.",
                                Logger.LogCategory.POLITICAL);
                }
            } catch (Exception ex) {
                Logger.logEvent("story.json: bad effect '" + e + "' - skipped.",
                        Logger.LogCategory.POLITICAL);
            }
        }
    }

    // The micro -> macro bridge. Unrest and morale come from the council's mood,
    // so the same choice plays out differently depending on the relationships
    // you've built (or burned) up to this point. Unchanged from before.
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
        Logger.logEvent("Chamber unrest swing: " + (unrestSwing >= 0 ? "+" : "") + unrestSwing,
                Logger.LogCategory.POLITICAL);
    }
}