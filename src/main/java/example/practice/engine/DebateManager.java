package example.practice.engine;

import example.practice.humans.Human;
import example.practice.logger.Logger;
import java.util.ArrayList;
import java.util.List;

public class DebateManager {

    // Stores the player's final choice to render in the chat
    public static String chosenResponse = null;

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

        if (chapterIndex == 8 && paragraphIndex == 18) {
            choices.add(new Choice("1. \"I was securing the future!\" (Authority)", () -> {
                engine.getKingdoms()[0].modifyMorale(15);
                engine.getKingdoms()[0].unrestLevel += 100;
                chosenResponse = "I was securing the future! We did what was necessary.";
                Logger.logEvent("Emperor asserts harsh authority! Morale rises, but unrest simmers.", Logger.LogCategory.POLITICAL);
            }));

            choices.add(new Choice("2. \"We must unite in this crisis.\" (Diplomacy)", () -> {
                engine.getKingdoms()[0].unrestLevel -= 200;
                if (engine.getKingdoms()[0].unrestLevel < 0) engine.getKingdoms()[0].unrestLevel = 0;
                engine.getKingdoms()[0].treasury -= 25000;
                chosenResponse = "We must unite in this crisis. Let funds be diverted to the people.";
                Logger.logEvent("Emperor appeases the council. Treasury funds diverted to public aid.", Logger.LogCategory.POLITICAL);
            }));

            // --- THE SEVERE TYRANNY CHOICE ---
            choices.add(new Choice("3. \"Silence! Guards, arrest this fool!\" (Tyranny)", () -> {
                engine.getKingdoms()[0].unrestLevel += 500;
                engine.getKingdoms()[0].limitersDisabled = true; // Disable boundary limits
                engine.getKingdoms()[0].modifyMorale(-25);
                engine.getKingdoms()[0].triggerMassDesertion(pop, 0.10); // 10% desert
                chosenResponse = "Silence! Guards, arrest this fool immediately!";
                Logger.logEvent("A council member is arrested! Limiters disabled! The political house is in an uproar!", Logger.LogCategory.POLITICAL);
            }));
        }
        return choices;
    }

    public static void handleTimeout(int chapterIndex, int paragraphIndex, SimulationEngine engine) {
        if (chapterIndex == 8 && paragraphIndex == 18) {
            engine.getKingdoms()[0].unrestLevel += 300;
            engine.getKingdoms()[0].modifyMorale(-20);
            chosenResponse = "... (The Emperor stammers in silence)";
            Logger.logEvent("The Emperor stammered in silence. The council sees him as weak and unfit!", Logger.LogCategory.POLITICAL);
        }
    }
}