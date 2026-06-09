package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.Military;
import example.practice.logger.Logger;

import java.util.List;

import static example.practice.logger.Logger.LogCategory.STORY;

public class StoryManager {
    private static boolean ch1_p3 = false;
    private static boolean ch1_p7 = false;
    private static boolean ch1_p9 = false;
    private static boolean ch8_p0 = false;
    private static boolean ch8_p9 = false;
    private static boolean ch8_p19 = false;

    public static void applyStoryEffects(int chapterIndex, int paragraphIndex, Kingdom kingdom, List<Human> population) {
        if (chapterIndex == 0) { // Chapter 1
            if (paragraphIndex == 3 && !ch1_p3) {
                Logger.logEvent("STORY: The crossroads meeting stirs the populace...", STORY);
                kingdom.unrestLevel += 1;
                addPeopleWithJob(population, kingdom.id, Military.REBEL.value, 15);
                ch1_p3 = true;
            }
            else if (paragraphIndex == 7 && !ch1_p7) {
                Logger.logEvent("STORY: News of twisted beasts spreads panic!", STORY);
                kingdom.unrestLevel += 1;
                addPeopleWithJob(population, kingdom.id, Military.REBEL.value, 20);
                ch1_p7 = true;
            }
            else if (paragraphIndex == 9 && !ch1_p9) {
                Logger.logEvent("STORY: The discovery of created monsters terrifies the people!", STORY);
                kingdom.modifyMorale(-2); // FIX: clamped, not raw armyMorale -= 2
                kingdom.unrestLevel += 5;
                addPeopleWithJob(population, kingdom.id, Military.REBEL.value, 30);
                ch1_p9 = true;
            }
        }
        else if (chapterIndex == 7) { // Chapter 8: Embers in the North
            if (paragraphIndex == 0 && !ch8_p0) {
                Logger.logEvent("STORY: A new charismatic Rebel Leader emerges in the North!", STORY);
                kingdom.unrestLevel += 100;
                addPeopleWithJob(population, kingdom.id, Military.REBEL.value, 1200);
                kingdom.setSkirmishControl(-1, 1.0f); // halt random skirmishes for the story
                ch8_p0 = true;
            }
            else if (paragraphIndex >= 1 && paragraphIndex <= 9 && !ch8_p9) {
                kingdom.setSkirmishControl(-1, 1.0f); // keep them paused through the build-up
                ch8_p9 = true;
            }
            else if (paragraphIndex >= 10 && paragraphIndex <= 19 && !ch8_p19) {
                CombatManager.forceSkirmish(kingdom, population, 360, 480);
                kingdom.setSkirmishControl(0, 1.0f); // FIX: resume normal skirmishes after the scripted battle
                ch8_p19 = true;
            }
        }
    }

    private static void addPeopleWithJob(List<Human> population, int kingdomId, int newJobId, int count) {
        int converted = 0;
        for (Human h : population) {
            if (converted >= count) break;
            if (h.isAlive && h.kingdomId == kingdomId && h.job != newJobId) {
                h.job = newJobId;
                converted++;
            }
        }
    }
}