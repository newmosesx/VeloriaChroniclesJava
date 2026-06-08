package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.*;

import java.util.List;

// Food and population are owned entirely by SubsistenceManager. EconomyManager
// keeps the rest: assigning work, the hourly grind that produces wood/stone/metal
// and wears people down, and pay. It no longer touches kingdom.food.
public class EconomyManager {

    public static void occupation(List<Human> batch) {
        for (Human h : batch) {
            if (h.isAlive && h.job == 0) {
                int r = (int) (Math.random() * 100);
                if (r < 45) h.job = 1;        // farmer
                else if (r < 55) h.job = 2;   // butcher
                else if (r < 68) h.job = 3;   // lumberjack
                else if (r < 81) h.job = 4;   // miner
                else if (r < 90) h.job = 5;   // blacksmith
                else if (r < 95) h.job = 6;   // swordsman
                else if (r < 98) h.job = 7;   // archer
                else h.job = 8;               // cavalry
            }
        }
    }

    public static void processBatchNeeds(Kingdom kingdom, List<Human> batch) {
        if (!kingdom.isActive) return;

        int woodProduced = 0, stoneProduced = 0, metalProduced = 0;

        for (Human h : batch) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;

            if (h.health > 10) {
                if ((int) (Math.random() * 2) == 1) {
                    switch (h.job) {
                        case 1: h.health -= 5;  h.hunger -= 10; break; // farmer  (food: daily)
                        case 2: h.health -= 10; h.hunger -= 15; break; // butcher (food: daily)
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
        }

        // Industry tech (improved tools) lifts wood/stone/metal yield.
        float prodMod = kingdom.storyProductionModifier * kingdom.divineProductionModifier
                * (1f + TechManager.bonus(TechEffect.RESOURCE));
        kingdom.wood += (int)(woodProduced * prodMod);
        kingdom.stone += (int)(stoneProduced * prodMod);
        kingdom.metal += (int)(metalProduced * prodMod);
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