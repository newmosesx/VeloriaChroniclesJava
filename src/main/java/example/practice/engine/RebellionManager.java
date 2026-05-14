package example.practice.engine;

import example.practice.config.CivilWar;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.Rebellion;
import example.practice.config.Military;
import example.practice.logger.Logger;
import example.practice.shared.ShareData;

import java.util.List;

import static example.practice.logger.Logger.LogCategory.STORY;

public class RebellionManager {

    public static void handleRecruitmentAndDissent(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive || kingdom.unrestLevel <= Rebellion.DISSENTTHRESHOLD.value) return;

        int newRebelsToday = 0;
        int maxNewRebels = (int) Rebellion.MAXNEWREBELSPERDAY.value;

        int unrestOverThreshold = kingdom.unrestLevel - (int) Rebellion.DISSENTTHRESHOLD.value;
        if (unrestOverThreshold > Rebellion.MAXUNRESTFORREBELCONVERSION.value) {
            unrestOverThreshold = (int) Rebellion.MAXUNRESTFORREBELCONVERSION.value;
        }

        for (Human h : population) {
            if (!h.isAlive || h.job == Military.REBEL.value || h.kingdomId != kingdom.id) continue;
            if (newRebelsToday >= maxNewRebels) break;

            boolean isSoldier = (h.job >= Military.SWORDSMAN.value && h.job <= Military.CAVALRY.value);
            boolean becameRebel = false;

            if (isSoldier) {
                int defectionChance = (int) (unrestOverThreshold * Rebellion.SOLDIERDEFECTIONCHANCE.value);
                if ((Math.random() * Rebellion.REBELCHANCEDIVISOR.value) < defectionChance) {
                    kingdom.armyMorale -= 5;
                    h.job = Military.REBEL.value;
                    becameRebel = true;
                }
            } else {
                if ((Math.random() * Rebellion.REBELCHANCEDIVISOR.value) < unrestOverThreshold) {
                    h.job = Military.REBEL.value;
                    becameRebel = true;
                }
            }

            if (becameRebel) {
                newRebelsToday++;
                if (Math.random() * 100 < Rebellion.REBELLEADERSPAWNCHANCE.value) {
                    h.isGeneral = true; // Rebel leader
                }
            }
        }
    }

    public static void checkCivilWarTrigger(Kingdom[] kingdoms, List<Human> population, ShareData sharedData) {
        long totalSoldiers = population.stream().filter(h -> h.isAlive && h.job >= 6 && h.job <= 8).count();
        long totalRebels = population.stream().filter(h -> h.isAlive && h.job == 9).count();

        // Condition: Rebels > 100 AND Rebels > 75% of Army (From C: civil_war.c)
        if (totalRebels > CivilWar.CIVIL_WAR_MINIMUM_REBELS.value &&
                totalRebels > (totalSoldiers * CivilWar.CIVIL_WAR_REBEL_TO_SOLDIER_RATIO.value)) {
            sharedData.civilWarStatus = "Status: CIVIL WAR ERUPTS";
        } else if (kingdoms[0].isActive) {
            sharedData.civilWarStatus = "Status: The Empire Reigns";
        } else {
            sharedData.civilWarStatus = "Status: Age of Kingdoms";
        }
    }

    public static void checkEmpireCollapse(Kingdom[] kingdoms, List<Human> population) {
        Kingdom empire = kingdoms[0];
        if (empire.isActive && empire.unrestLevel >= Rebellion.REBELLIONTHRESHOLD.value) {
            Logger.logEvent("!!! THE EMPIRE HAS FALLEN !!!", STORY);
            empire.isActive = false;

            // Activate successor kingdoms
            for (int i = 1; i < kingdoms.length; i++) {
                kingdoms[i].isActive = true;
                kingdoms[i].unrestLevel = 0;
                kingdoms[i].food = 500; // Starting resources for new kingdoms
            }

            // Reassign the people
            for (Human h : population) {
                if (h.isAlive) {
                    h.kingdomId = (int) (Math.random() * 7) + 1; // Assign to IDs 1-7
                    if (h.job == Military.REBEL.value || h.job > 5) {
                        h.job = 1; // Former rebels and soldiers become farmers in the new world
                    }
                }
            }
        }
    }
}