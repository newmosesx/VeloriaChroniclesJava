package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.*;
import example.practice.logger.Logger;

import java.util.List;

public class EconomyManager {

    // --- OCCUPATION SWEEP ---
    public static void occupation(List<Human> batch) {
        for (Human h : batch) {
            // Unemployed individuals randomly get a job from 1 to 8 (Civilian or Military)
            if (h.isAlive && h.job == 0) {
                h.job = (int)(Math.random() * 8) + 1;
            }
        }
    }

    public static void processBatchNeeds(Kingdom kingdom, List<Human> batch) {
        if (!kingdom.isActive) return;

        int foodProduced = 0, woodProduced = 0, stoneProduced = 0, metalProduced = 0, weaponsProduced = 0;
        boolean famineTriggered = false;

        for (Human h : batch) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;

            if (h.health > 10) {
                if ((int)(Math.random() * 2) == 1) {
                    switch (h.job) {
                        case 1: foodProduced += (int)(Math.random() * (ProductionCost.FARMERFOODPRODUCTION.value)); h.health -= 5; h.hunger -= 10; break;
                        case 2: foodProduced += (int)(Math.random() * (ProductionCost.BUTCHERPRODUCTION.value)); h.health -= 10; h.hunger -= 15; break;
                        case 3: woodProduced += (int)(Math.random() * (ProductionCost.LUMBERJACKPRODUCTION.value)) + 1; h.health -= 15; h.hunger -= 20; break;
                        case 4:
                            if (Math.random() * 100 < ProductionCost.MINERPRODUCTIONCHANCE.value)
                                metalProduced += (int)(Math.random() * (ProductionCost.MINERPRODUCTION.value)) + 1;
                            else
                                stoneProduced += (int)(Math.random() * (ProductionCost.MINERPRODUCTION.value)) + 1;
                            h.health -= 30; h.hunger -= 35; break;
                        case 5:
                            if (kingdom.metal >= (int)ProductionCost.BLACKSMITHNEED.value) {
                                kingdom.metal -= (int)ProductionCost.BLACKSMITHNEED.value;
                                h.health -= 20; h.hunger -= 25;
                            } else { h.hunger -= 10; } break;
                        case 6: case 7: case 8:
                            h.health -= 5; h.hunger -= 10; break;
                    }
                } else { h.health += 20; }
            } else { h.health += 5; }

            if (h.job != Military.REBEL.value) {
                if (kingdom.food > 1) {
                    if (h.hunger <= (int)ProductionCost.EATHUNGERTHRESHOLD.value) {
                        if (h.bronze >= (int)ProductionCost.FOODCOST.value) {
                            h.bronze -= (int)ProductionCost.FOODCOST.value;
                            int eatingAmount = 2;
                            if (h.job >= 6 && h.job <= 8) eatingAmount += (int)Combat.MILITARY_EXTRA_FOOD_CONSUMPTION.value;
                            kingdom.food -= eatingAmount;
                            h.hunger += (int)(Math.random() * 40) + 10;
                        } else { h.health -= 5; }
                    }
                } else {
                    if (!famineTriggered) {
                        // --- FAMINE METRIC LOGGING ---
                        int famineDeaths = Math.max(1, (int)(kingdom.population * (ProductionCost.FAMINEPOPULATIONLOSSPERCENT.value / 100.0f)));
                        if (!DailyEventTracker.famineLogged) {
                            Logger.logEvent("!!! FAMINE in " + kingdom.name + " !!!", Logger.LogCategory.NATURAL);
                            Logger.logEvent(" -> " + famineDeaths + " people have died from starvation!", Logger.LogCategory.NATURAL);
                            DailyEventTracker.famineLogged = true;
                        }

                        kingdom.unrestLevel += (int)Rebellion.UNRESTGAINFROMFAMINE.value;
                        kingdom.food = 0;
                        famineTriggered = true;
                    }
                    if (h.hunger > 0) { h.hunger -= 10; }
                    else if (h.hunger < 0) { h.isAlive = false; }
                }
            }
        }

        float prodMod = kingdom.storyProductionModifier * kingdom.divineProductionModifier;
        kingdom.food += (int)(foodProduced * prodMod);
        kingdom.wood += (int)(woodProduced * prodMod);
        kingdom.stone += (int)(stoneProduced * prodMod);
        kingdom.metal += (int)(metalProduced * prodMod);

        if (kingdom.storyFoodDailyCap > 0 && kingdom.food > kingdom.storyFoodDailyCap) {
            kingdom.food = kingdom.storyFoodDailyCap;
        }
    }

    public static void distributePayments(List<Human> batch) {
        for (Human h : batch) {
            if (h.isAlive) {
                int pay = 10; // Default
                switch (h.job) {
                    case 1: pay = (int)(Math.random() * 30) + 1; break; // Farmer
                    case 2: pay = (int)(Math.random() * 50) + 1; break; // Butcher
                    case 3: pay = (int)(Math.random() * 40) + 1; break; // Lumberjack
                    case 4: pay = (int)(Math.random() * 30) + 1; break; // Miner
                    case 5: pay = (int)(Math.random() * 85) + 1; break; // Blacksmith
                    case 6: case 7: case 8: pay = (int)(Math.random() * 41) + 20; break; // Military
                }
                h.bronze += pay;
            }
        }
    }
}