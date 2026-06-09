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

    // How strongly freedom beats loyalty when a desperate commoner decides to act.
    // The commons know the rebels eat while the empire's granaries sit empty, so the
    // road to the rebellion is the favoured one.
    private static final double REBEL_BASE_WEIGHT   = 0.25; // baseline pull toward revolt
    private static final double WEIGHT_EMPTY_GRANARY = 0.10; // +pull when the people's stores are bare
    private static final double WEIGHT_FED_ARMY      = 0.05; // +pull when the legions are visibly well-fed

    public static void handleRecruitmentAndDissent(Kingdom kingdom, List<Human> population) {
        // Gated by the conflict spine: no churn at peace.
        if (ConflictManager.stateOf(kingdom) == ConflictState.PEACE) return;
        if (!kingdom.isActive || kingdom.unrestLevel <= Rebellion.DISSENTTHRESHOLD.value) return;

        int newRebelsToday = 0;
        int maxNewRebels = (int) Rebellion.MAXNEWREBELSPERDAY.value;

        int unrestOverThreshold = kingdom.unrestLevel - (int) Rebellion.DISSENTTHRESHOLD.value;
        if (unrestOverThreshold > Rebellion.MAXUNRESTFORREBELCONVERSION.value) {
            unrestOverThreshold = (int) Rebellion.MAXUNRESTFORREBELCONVERSION.value;
        }

        // Is the army actually being fed from its granary? Well-supplied legions hold;
        // legions on empty rations break and walk over to the rebellion.
        int soldiers = 0;
        for (Human h : population)
            if (h.isAlive && h.kingdomId == kingdom.id && h.job >= 6 && h.job <= 8) soldiers++;
        boolean armyHungry = kingdom.militaryFood < soldiers;          // less than ~one ration each in store
        boolean armyWellFed = kingdom.militaryFood >= soldiers * 4;     // comfortably stocked

        // The commons' choice is weighted by what they can see: empty public granaries
        // and a well-fed army both push more of them toward the rebels.
        double rebelWeight = REBEL_BASE_WEIGHT;
        if (kingdom.food <= 0) rebelWeight += WEIGHT_EMPTY_GRANARY;
        if (kingdom.militaryFood > 0) rebelWeight += WEIGHT_FED_ARMY;
        if (rebelWeight > 0.92) rebelWeight = 0.92;                     // some always endure

        for (Human h : population) {
            if (!h.isAlive || h.job == Military.REBEL.value || h.kingdomId != kingdom.id) continue;
            if (newRebelsToday >= maxNewRebels) break;

            boolean isSoldier = (h.job >= Military.SWORDSMAN.value && h.job <= Military.CAVALRY.value);
            boolean becameRebel = false;

            if (isSoldier) {
                // A fed army holds the line; a starving one defects far more readily.
                double mult = armyHungry ? 1.1 : (armyWellFed ? 0.4 : 0.8);
                int defectionChance = (int) (unrestOverThreshold * Rebellion.SOLDIERDEFECTIONCHANCE.value);
                if ((Math.random() * Rebellion.REBELCHANCEDIVISOR.value) < defectionChance * mult) {
                    kingdom.modifyMorale(-5);
                    h.job = Military.REBEL.value;
                    becameRebel = true;
                }
            } else {
                // The commoner is pushed to act this tick (scales with how far past the
                // dissent line the realm has slid).
                if ((Math.random() * Rebellion.REBELCHANCEDIVISOR.value) < unrestOverThreshold) {
                    // TWO ROADS:
                    //   - fight for freedom  -> join the rebels (the favoured road)
                    //   - die for the empire -> endure, and remain exposed to the famine
                    if (Math.random() < rebelWeight) {
                        h.job = Military.REBEL.value;     // fight for freedom
                        becameRebel = true;
                    }
                    // else: they stay loyal and endure -- no job change. The granary, not
                    // this method, decides whether that loyalty costs them their life.
                }
            }

            if (becameRebel) {
                newRebelsToday++;
                if (Math.random() * 100 < Rebellion.REBELLEADERSPAWNCHANCE.value) {
                    h.isGeneral = true; // a field leader rises from the ranks
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