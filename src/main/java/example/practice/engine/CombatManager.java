package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.*;
import example.practice.logger.Logger;
import java.util.List;

import static example.practice.logger.Logger.LogCategory.MILITARY;

public class CombatManager {

    private static class Battalion {
        float health, damage, defense;
        int initialCount;
        float hltPerPerson, dmgPerPerson, defPerPerson;

        Battalion(float h, float d, float def, int c) {
            this.initialCount = c;
            this.health = h;
            if (c > 0) {
                this.hltPerPerson = h / c;
                this.dmgPerPerson = d / c;
                this.defPerPerson = def / c;
            }
        }
        int getEffectiveCount() { return (int) Math.ceil(health / hltPerPerson); }
        float getEffectiveDamage() { return getEffectiveCount() * dmgPerPerson; }
        float getEffectiveDefense() { return getEffectiveCount() * defPerPerson; }
    }

    // --- SCRIPTED STORY BATTLES (Matches C code: run_battle in civil_war.c) ---
    public static void forceSkirmish(Kingdom kingdom, List<Human> population, int imperialCombatants, int rebelCombatants) {
        // [Unchanged from previous update - omitted for brevity, but stays here]
        if (imperialCombatants <= 0 || rebelCombatants <= 0) return;

        int generalCount = 0;
        int rebelLeaderCount = 0;

        for (Human h : population) {
            if (h.isAlive && h.isGeneral && h.kingdomId == kingdom.id) {
                if (h.job >= 6 && h.job <= 8) generalCount++;
                else if (h.job == 9) rebelLeaderCount++;
            }
        }

        float moraleModifier = 0.5f + (kingdom.armyMorale / 100.0f);
        float imperialStrength = imperialCombatants * moraleModifier;
        float rebelStrength = rebelCombatants * Combat.REBEL_BASE_STRENGTH_MODIFIER.value;

        if (generalCount > 0) {
            for (int i = 0; i < generalCount; i++) imperialStrength *= Combat.GENERAL_COMBAT_BONUS.value;
        }

        if (rebelLeaderCount > 0) {
            for (int i = 0; i < rebelLeaderCount; i++) rebelStrength *= Combat.REBEL_LEADER_COMBAT_BONUS.value;
        }

        int imperialCasualties = 0;
        int rebelCasualties = 0;
        String winnerTitle;

        if (imperialStrength > rebelStrength) {
            winnerTitle = "Imperial Victory!";
            rebelCasualties = (int)(rebelCombatants * (0.6 + (Math.random() * 30) / 100.0));
            imperialCasualties = (int)(imperialCombatants * (0.1 + (Math.random() * 20) / 100.0));
            kingdom.armyMorale += (int)Battle.MORALE_GAIN_ON_VICTORY.value;
            if (kingdom.armyMorale > 100) kingdom.armyMorale = 100;
        } else {
            winnerTitle = "Rebel Victory!";
            imperialCasualties = (int)(imperialCombatants * (0.5 + (Math.random() * 30) / 100.0));
            rebelCasualties = (int)(rebelCombatants * (0.2 + (Math.random() * 20) / 100.0));
            kingdom.armyMorale -= (int)Battle.MORALE_LOSS_ON_DEFEAT.value;
            if (kingdom.armyMorale < 0) kingdom.armyMorale = 0;
        }

        imperialCasualties = Math.min(imperialCasualties, imperialCombatants);
        rebelCasualties = Math.min(rebelCasualties, rebelCombatants);
        kingdom.inflictProportionalCasualties(population, kingdom.id, imperialCasualties, rebelCasualties);

        Logger.logEvent("Battle..!: " + winnerTitle, MILITARY);
        Logger.logEvent("Engaged: " + imperialCombatants + " Imperials vs. " + rebelCombatants + " Rebels", MILITARY);
        Logger.logEvent("Survivors: " + (imperialCombatants - imperialCasualties) + " Imperials | " + (rebelCombatants - rebelCasualties) + " Rebels", MILITARY);
    }

    // --- RANDOM HOURLY SKIRMISHES ---
    public static void triggerHourlySkirmish(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive || kingdom.storySkirmishOverride == -1) return;

        int chance = (int) Battle.HOURLY_SKIRMISH_BASE_CHANCE_PERCENT.value;
        if (kingdom.storySkirmishOverride == 1) chance = 100;

        if ((Math.random() * 100) > chance) return;
        kingdom.storySkirmishOverride = 0;

        Battalion soldiersFull = new Battalion(0,0,0,0);
        Battalion rebelsFull = new Battalion(0,0,0,0);
        int generals = 0, rebelLeaders = 0;

        int sCount = (int)(soldiersFull.initialCount * Battle.SOLDIERS_IN_SKIRMISH.value);
        int rCount = (int)(rebelsFull.initialCount * Battle.REBELS_IN_SKIRMISH.value);

        // Prevent ghost battles (0 vs X)
        if (sCount <= 0 || rCount <= 0) return;

        Battalion sBatch = new Battalion(
                soldiersFull.health * Battle.SOLDIERS_IN_SKIRMISH.value,
                soldiersFull.damage * Battle.SOLDIERS_IN_SKIRMISH.value,
                soldiersFull.defense * Battle.SOLDIERS_IN_SKIRMISH.value,
                (int)(soldiersFull.initialCount * Battle.SOLDIERS_IN_SKIRMISH.value)
        );

        Battalion rBatch = new Battalion(
                rebelsFull.health * Battle.REBELS_IN_SKIRMISH.value,
                rebelsFull.damage * Battle.REBELS_IN_SKIRMISH.value,
                rebelsFull.defense * Battle.REBELS_IN_SKIRMISH.value,
                (int)(rebelsFull.initialCount * Battle.REBELS_IN_SKIRMISH.value)
        );

        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            if (h.job >= 6 && h.job <= 8) {
                soldiersFull.health += h.health; soldiersFull.damage += h.damage; soldiersFull.defense += h.defense; soldiersFull.initialCount++;
                if (h.isGeneral) generals++;
            } else if (h.job == 9) {
                rebelsFull.health += h.health; rebelsFull.damage += h.damage; rebelsFull.defense += h.defense; rebelsFull.initialCount++;
                if (h.isGeneral) rebelLeaders++;
            }
        }

        float sDmg = sBatch.getEffectiveDamage();
        float rDmg = rBatch.getEffectiveDamage();
        if (generals > 0) sDmg *= (1.0f + (Combat.GENERAL_COMBAT_BONUS.value - 1.0f) * generals);
        if (rebelLeaders > 0) rDmg *= (1.0f + (Combat.REBEL_LEADER_COMBAT_BONUS.value - 1.0f) * rebelLeaders);

        Logger.logEvent("SKIRMISH! " + sBatch.initialCount + " Imperials vs " + rBatch.initialCount + " Rebels.", MILITARY);

        int residualSoldierCount = sBatch.initialCount;
        int residualRebelCount = rBatch.initialCount;
        int battleState = 3; // 3 means continue

        // Tactical Loop with C's declareResult logic
        while (battleState == 3) {
            // Surprise Attack
            sBatch.health -= (rDmg * Combat.ELEMENT_OF_SURPRISE.value);

            // Tank Mode
            float currentDef = sBatch.getEffectiveDefense();
            if (sBatch.health < (sBatch.hltPerPerson * sBatch.initialCount) * 0.5) {
                sBatch.health += (currentDef * 0.5f);
                currentDef *= 0.5f;
            }

            // Soldiers Strike
            if (rBatch.getEffectiveDefense() < (sDmg * Combat.ORGANIZED_COMMAND.value)) {
                rBatch.health -= ((sDmg * Combat.ORGANIZED_COMMAND.value) - rBatch.getEffectiveDefense());
            } else {
                rBatch.health -= sDmg;
            }

            // Evaluate state
            battleState = declareResult(rBatch.getEffectiveCount(), sBatch.getEffectiveCount(), residualRebelCount, residualSoldierCount);
        }

        int sLoss = residualSoldierCount - sBatch.getEffectiveCount();
        int rLoss = residualRebelCount - rBatch.getEffectiveCount();
        kingdom.inflictProportionalCasualties(population, kingdom.id, Math.max(0, sLoss), Math.max(0, rLoss));

        if (sLoss > rLoss) {
            kingdom.armyMorale -= (int)Battle.MORALE_LOSS_ON_DEFEAT.value;
            Logger.logEvent("The Empire suffered a setback.", MILITARY);
        } else {
            kingdom.armyMorale += (int)Battle.MORALE_GAIN_ON_VICTORY.value;
            // Only log if it was an actual large suppression to prevent bloat
            if (sBatch.initialCount > 10) {
                Logger.logEvent("A local rebellion was suppressed.", MILITARY);
            }
        }

        kingdom.modifyMorale(sLoss > rLoss ? -(int)Battle.MORALE_LOSS_ON_DEFEAT.value : (int)Battle.MORALE_GAIN_ON_VICTORY.value);

        if (kingdom.armyMorale < 0 && !kingdom.limitersDisabled) kingdom.armyMorale = 0;
        if (kingdom.armyMorale > 100 && !kingdom.limitersDisabled) kingdom.armyMorale = 100;
    }

    // --- BATTLE RESOLUTION STATE MACHINE (From unrest.c) ---
    private static int declareResult(int rebelCount, int soldierCount, int residualRebelCount, int residualSoldierCount) {
        if (soldierCount <= 0 && rebelCount <= 0) {
            Logger.logEvent("The battle ended with no victor.. Just blood..", MILITARY);
            return 2; // End battle
        }

        if (soldierCount >= rebelCount) {
            if (rebelCount >= residualSoldierCount * 0.2 && soldierCount < residualSoldierCount * 0.3) {
                return 3; // Continue battle
            } else {
                Logger.logEvent("Victory of the Empire!: Rebel Retreat", MILITARY);
                return 2; // End battle
            }
        } else {
            if (soldierCount >= residualRebelCount * 0.2 && rebelCount < residualRebelCount * 0.3) {
                return 3; // Continue battle
            } else {
                Logger.logEvent("Victory of the Rebels!: Soldiers Retreat", MILITARY);
                return 2; // End battle
            }
        }
    }
}