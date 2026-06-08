package example.practice.engine;

import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.config.*;
import example.practice.logger.Logger;
import java.util.List;

import static example.practice.logger.Logger.LogCategory.*;

public class AIManager {

    public static void runEmpireAI(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive || kingdom.population < 100) return;

        // 1. Intelligence Gathering
        int dailyFoodConsumption = kingdom.population + 1;
        float foodDaysLeft = (float) kingdom.food / dailyFoodConsumption;

        long soldierCount = population.stream()
                .filter(h -> h.isAlive && h.kingdomId == kingdom.id && h.job >= 6 && h.job <= 8).count();
        long rebelCount = population.stream()
                .filter(h -> h.isAlive && h.kingdomId == kingdom.id && h.job == 9).count();

        float militaryRatio = (float) soldierCount / (rebelCount + 1);
        float foodUrgency = (foodDaysLeft < AIGovernor.AI_FOOD_DAYS_THRESHOLD.value) ? 1.0f - (foodDaysLeft / AIGovernor.AI_FOOD_DAYS_THRESHOLD.value) : 0.0f;
        float unrestUrgency = (float) kingdom.unrestLevel / Rebellion.REBELLIONTHRESHOLD.value;
        float militaryUrgency = (militaryRatio < 1.0f) ? 1.0f - militaryRatio : 0.0f;

        // --- TIER 0: Divine Intervention (AI Last Resort) ---
        if (kingdom.canUseDivineIntervention && kingdom.gold > (int)DivineIntervention.AI_DIVINE_INTERVENTION_TREASURY_THRESHOLD.value) {

            // Condition 1: Reinforcements (Military Collapse)
            if (militaryUrgency > 0.8f) {
                Logger.logEvent("GOVERNOR: Armies collapsing! Praying for divine reinforcements.", MILITARY);
                kingdom.gold -= (int)ReinforceIntervention.DI_COST_REINFORCEMENTS.value;
                for (int i = 0; i < (int)ReinforceIntervention.DI_REINFORCEMENTS_COUNT.value; i++) {
                    Human h = new Human(kingdom.id);
                    h.job = 6; // Swordsman
                    population.add(h);
                }
                kingdom.divineTaxModifier = ReinforceIntervention.DI_PENALTY_TAX_MODIFIER.value;
                kingdom.divinePenaltyTimerDays = (int)DivineIntervention.PENALTY_DURATION_DAYS.value;
                kingdom.canUseDivineIntervention = false;
                return;
            }

            // Condition 2: Absolution (Mass Unrest Threatening Collapse)
            if (kingdom.unrestLevel > InterventionAbsolution.DI_ABSOLUTION_UNREST_THRESHOLD.value && unrestUrgency > 0.7f) {
                Logger.logEvent("GOVERNOR: The people threaten to tear down the walls! We beg for divine absolution!", NATURAL);
                kingdom.gold -= InterventionAbsolution.DI_COST_ABSOLUTION.value;
                kingdom.unrestLevel -= InterventionAbsolution.DI_ABSOLUTION_UNREST_REDUCTION.value;
                kingdom.armyMorale -= InterventionAbsolution.DI_PENALTY_MORALE_DROP.value;
                if (kingdom.armyMorale < 0) kingdom.armyMorale = 0;

                kingdom.divinePenaltyTimerDays = (int)DivineIntervention.PENALTY_DURATION_DAYS.value;
                kingdom.canUseDivineIntervention = false;
                return;
            }

            // Condition 3: Sustenance (Catastrophic Famine Imminent)
            if (foodUrgency > 0.9f) {
                Logger.logEvent("GOVERNOR: The granaries are empty and the people starve! We plead for a miracle of sustenance!", STORY);
                kingdom.gold -= (int)InterventionSustenance.DI_COST_SUSTENANCE.value;
                kingdom.food += (int)InterventionSustenance.DI_SUSTENANCE_FOOD_AMOUNT.value;
                kingdom.divineProductionModifier = InterventionSustenance.DI_PENALTY_PRODUCTION_MODIFIER.value;

                kingdom.divinePenaltyTimerDays = (int)DivineIntervention.PENALTY_DURATION_DAYS.value;
                kingdom.canUseDivineIntervention = false;
                return;
            }
        }

        // --- TIER 1: Catastrophe Aversion (Starvation) ---
        if (foodDaysLeft < AIGovernor.AI_CRITICAL_FOOD_DAYS_THRESHOLD.value) {
            if (!DailyEventTracker.farmerConversionLogged) {
                Logger.logEvent("GOVERNOR: Reassigning all available workers to farms in " + kingdom.name, Logger.LogCategory.POLITICAL);
                DailyEventTracker.farmerConversionLogged = true;
            }
            int converted = 0;
            for (Human h : population) {
                if (converted >= (int)AIGovernor.AI_FARMER_CONVERSION_COUNT.value) break;
                if (h.isAlive && h.kingdomId == kingdom.id) {
                    // Convert non-essential jobs (Lumberjack, Miner, Blacksmith) to farmers
                    if (h.job == 3 || h.job == 4 || h.job == 5) {
                        h.job = 1;
                        converted++;
                    }
                }
            }
            return;
        }

        // --- TIER 2: Reactive Problem-Solving (Urgency Scores) ---
        float maxUrgency = Math.max(foodUrgency, Math.max(unrestUrgency, militaryUrgency));
        if (maxUrgency > AIGovernor.AI_ACTION_THRESHOLD.value) {
            if (maxUrgency == militaryUrgency) {
                if (!DailyEventTracker.recruitmentLogged) {
                    Logger.logEvent("GOVERNOR: Prioritizing recruitment in " + kingdom.name, Logger.LogCategory.POLITICAL);
                    DailyEventTracker.recruitmentLogged = true;
                }
                kingdom.recruitSoldiers(population);
            }
            else if (maxUrgency == unrestUrgency && kingdom.gold >= (int)AIGovernor.AI_FESTIVAL_COST.value) {
                if (!DailyEventTracker.festivalTriggered) {
                    Logger.logEvent("GOVERNOR: Hosting festivals to calm the populace in " + kingdom.name, Logger.LogCategory.POLITICAL);
                    DailyEventTracker.festivalTriggered = true;
                }
                kingdom.gold -= (int)AIGovernor.AI_FESTIVAL_COST.value;
                kingdom.unrestLevel -= (int)AIGovernor.AI_FESTIVAL_UNREST_REDUCTION.value;
                if (kingdom.unrestLevel < 0) kingdom.unrestLevel = 0;
            }
            else if (maxUrgency == foodUrgency) {
                if (!DailyEventTracker.farmerConversionLogged) {
                    Logger.logEvent("GOVERNOR: Assigning more workers to farms in " + kingdom.name, Logger.LogCategory.POLITICAL);
                    DailyEventTracker.farmerConversionLogged = true;
                }
                int converted = 0;
                for (Human h : population) {
                    // Less drastic than catastrophe response (divided by 2)
                    if (converted >= (int)AIGovernor.AI_FARMER_CONVERSION_COUNT.value / 2) break;
                    if (h.isAlive && h.kingdomId == kingdom.id && (h.job == 3 || h.job == 4)) {
                        h.job = 1;
                        converted++;
                    }
                }
            }
            return;
        }

        // --- TIER 3: Proactive Management (Army Goal: 8%) ---
        float idealArmy = kingdom.population * AIGovernor.AI_ARMY_SIZE_GOAL_PERCENT.value;
        if (soldierCount < idealArmy) {
            kingdom.recruitSoldiers(population); // Slow build up
        }
    }
}