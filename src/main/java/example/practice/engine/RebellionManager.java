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
                    kingdom.modifyMorale(-5); // FIX: route through the clamp, not raw armyMorale -= 5
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

    public static boolean isMilitarilyOverwhelmed(Kingdom kingdom, List<Human> population) {
        if (kingdom.id != 0 || !kingdom.isActive) return false;
        int soldiers = 0, rebels = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            if (h.job >= 6 && h.job <= 8) soldiers++;
            else if (h.job >= 9) rebels++;
        }
        return rebels >= 6 * Math.max(1, soldiers); // 6:1 or worse — the army can't hold
    }

    public static void checkCivilWarTrigger(Kingdom[] kingdoms, List<Human> population, ShareData sharedData) {
        long totalSoldiers = population.stream().filter(h -> h.isAlive && h.job >= 6 && h.job <= 8).count();
        long totalRebels = population.stream().filter(h -> h.isAlive && h.job == 9).count();

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
        if (!empire.isActive) return;

        boolean unrestCollapse = empire.unrestLevel >= Rebellion.REBELLIONTHRESHOLD.value;
        boolean overwhelmed = isMilitarilyOverwhelmed(empire, population);

        if (unrestCollapse || overwhelmed) {
            Logger.logEvent(overwhelmed && !unrestCollapse
                    ? "!!! THE LEGIONS ARE OVERWHELMED - THE EMPIRE HAS FALLEN !!!"
                    : "!!! THE EMPIRE HAS FALLEN !!!", STORY);
            empire.isActive = false;

            EmpireAshes ashes = SuccessorSeeder.captureAshes(empire, population);
            SuccessorSeeder.seedSuccessors(kingdoms, population, ashes);
        }
    }
}