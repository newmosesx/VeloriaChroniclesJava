package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.RandomEvents;
import example.practice.config.EventSpecific;
import example.practice.logger.Logger;

import java.util.List;

import static example.practice.logger.Logger.LogCategory.*;

public class EventManager {

    public static void triggerRandomEvent(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive || kingdom.population < 50) return;

        // Roll for random event
        if ((Math.random() * 100) >= RandomEvents.DAILY_RANDOM_EVENT_CHANCE_PERCENT.value) {
            return; // Nothing happens today
        }

        int eventId = (int) (Math.random() * 6); // 0 to 5

        switch (eventId) {
            case 0:
                eventBountifulHarvest(kingdom);
                break;
            case 1:
                eventDiscoveryOfGold(kingdom, population);
                break;
            case 2:
                eventPlague(kingdom, population);
                break;
            case 3:
                eventDrought(kingdom);
                break;
            case 4:
                eventBarbarianRaid(kingdom, population);
                break;
            case 5:
                eventPoliticalIntrigue(kingdom);
                break;
        }
    }

    private static void eventBountifulHarvest(Kingdom kingdom) {
        if (DailyEventTracker.harvestTriggered) return; // CHECK
        int foodGain = (int) (EventSpecific.HARVEST_BASE_FOOD_GAIN.value + (kingdom.population * EventSpecific.HARVEST_POPULATION_FOOD_MULTIPLIER.value));
        kingdom.food += foodGain;
        Logger.logEvent("EVENT: A bountiful harvest in " + kingdom.name + "!", Logger.LogCategory.NATURAL);
        DailyEventTracker.harvestTriggered = true; // LOCK
    }

    private static void eventDiscoveryOfGold(Kingdom kingdom, List<Human> population) {
        if (DailyEventTracker.goldTriggered) return; // CHECK
        Logger.logEvent("EVENT: A vein of gold discovered in " + kingdom.name + "!", Logger.LogCategory.NATURAL);
        for (Human h : population) {
            if (h.isAlive && h.kingdomId == kingdom.id) {
                h.bronze += EventSpecific.GOLD_DISCOVERY_BRONZE_BONUS.value;
            }
        }
        DailyEventTracker.goldTriggered = true; // LOCK
    }

    private static void eventPlague(Kingdom kingdom, List<Human> population) {
        if (DailyEventTracker.plagueTriggered) return;
        Logger.logEvent("EVENT: Sickness and decay! A plague strikes " + kingdom.name + "!", Logger.LogCategory.NATURAL);
        int deaths = (int) (kingdom.population * RandomEvents.PLAGUE_POPULATION_LOSS_PERCENT.value);
        killRandomCivilians(kingdom, population, deaths);
        kingdom.unrestLevel += EventSpecific.PLAGUE_UNREST_GAIN.value;
        DailyEventTracker.plagueTriggered = true;
    }

    private static void eventDrought(Kingdom kingdom) {
        if (DailyEventTracker.droughtTriggered) return;
        Logger.logEvent("EVENT: Heat wave! The fields are dust in " + kingdom.name + "!", Logger.LogCategory.NATURAL);
        kingdom.food = (int) (kingdom.food * 0.15);
        if (kingdom.food < 0) kingdom.food = 0;
        kingdom.unrestLevel += EventSpecific.DROUGHT_UNREST_GAIN.value;
        DailyEventTracker.droughtTriggered = true;
    }

    private static void eventBarbarianRaid(Kingdom kingdom, List<Human> population) {
        if (DailyEventTracker.barbarianTriggered) return;
        Logger.logEvent("EVENT: Barbarians are raiding " + kingdom.name + "!", MILITARY);
        int deaths = (int) (kingdom.population * RandomEvents.BARBARIAN_POPULATION_LOSS_PERCENT.value);
        killRandomCivilians(kingdom, population, deaths);
        kingdom.wood = (int) (kingdom.wood * EventSpecific.BARBARIAN_RAID_RESOURCE_LOSS_PERCENT.value);
        kingdom.stone = (int) (kingdom.stone * EventSpecific.BARBARIAN_RAID_RESOURCE_LOSS_PERCENT.value);
        kingdom.unrestLevel += EventSpecific.BARBARIAN_RAID_UNREST_GAIN.value;
        DailyEventTracker.barbarianTriggered = true;
    }

    private static void eventPoliticalIntrigue(Kingdom kingdom) {
        if (DailyEventTracker.intrigueTriggered) return;
        Logger.logEvent("EVENT: A political plot is uncovered in the court of " + kingdom.name + "!", Logger.LogCategory.POLITICAL);
        kingdom.unrestLevel += EventSpecific.POLITICAL_INTRIGUE_UNREST_GAIN.value;
        DailyEventTracker.intrigueTriggered = true;
    }

    private static void killRandomCivilians(Kingdom kingdom, List<Human> population, int count) {
        if (count <= 0) return;
        int casualties = 0;
        int attempts = 0;

        while (casualties < count && attempts < population.size() * 2) {
            int randIdx = (int) (Math.random() * population.size());
            Human h = population.get(randIdx);

            // Civilian jobs are 1 to 5
            if (h.isAlive && h.kingdomId == kingdom.id && h.job >= 1 && h.job <= 5) {
                h.isAlive = false;
                casualties++;
            }
            attempts++;
        }
    }
}