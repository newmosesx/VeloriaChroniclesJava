package example.practice.engine;

import example.practice.config.*;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static example.practice.logger.Logger.LogCategory.NATURAL;
import static example.practice.logger.Logger.LogCategory.STORY;

// Turns a fallen Empire into seven new kingdoms. Two steps:
//   1. captureAshes()  - read the Empire's final state into an EmpireAshes
//   2. seedSuccessors() - generate archetypes + traits, then redistribute people
public class SuccessorSeeder {

    private static final String[] NAMES = {
            "Korvath", "Aeloria", "Sundermark", "Vael Tor", "Greymoor", "Ashen Reach", "Thornehold"
    };

    public static EmpireAshes captureAshes(Kingdom empire, List<Human> population) {
        EmpireAshes a = new EmpireAshes();
        int pop = Math.max(1, empire.population);

        long rebels = population.stream()
                .filter(h -> h.isAlive && h.kingdomId == empire.id && h.job == Military.REBEL.value).count();
        long soldiers = population.stream()
                .filter(h -> h.isAlive && h.kingdomId == empire.id && h.job >= 6 && h.job <= 8).count();
        int generals = (int) population.stream()
                .filter(h -> h.isAlive && h.kingdomId == empire.id && h.isGeneral).count();

        a.finalUnrest = empire.unrestLevel;
        a.finalMorale = empire.armyMorale;
        a.foodDaysLeft = (float) empire.food / (pop + 1);
        a.rebelFraction = (float) rebels / pop;
        a.generalCount = generals;
        a.survivingPopulation = (int) population.stream().filter(h -> h.isAlive).count();

        int domJob = 1, domCount = -1;
        for (int j = 1; j <= 5 && j < empire.jobCounts.length; j++) {
            if (empire.jobCounts[j] > domCount) { domCount = empire.jobCounts[j]; domJob = j; }
        }
        a.dominantCivilJob = domJob;

        a.famineCollapse = a.foodDaysLeft < 2.0f;
        a.radicalCollapse = a.rebelFraction > 0.25f;
        a.militaristCollapse = (soldiers + generals) > pop * 0.10f;

        Logger.logEvent("Ashes of the Empire - unrest " + a.finalUnrest
                + ", rebels " + Math.round(a.rebelFraction * 100) + "%, "
                + generals + " surviving generals.", NATURAL);
        return a;
    }

    public static void seedSuccessors(Kingdom[] kingdoms, List<Human> population, EmpireAshes ashes) {
        KingdomArchetype[] pool = pickArchetypes(ashes);

        for (int i = 1; i < kingdoms.length; i++) {
            Kingdom k = kingdoms[i];
            KingdomArchetype arch = pool[(i - 1) % pool.length];

            k.archetype = arch;
            k.isActive = true;
            k.name = successorName(i) + ", " + arch.descriptor;

            float prod = arch.productionMod;

            int baseFood = (int) InitialKingdomResources.INITIAL_SUCCESSOR_FOOD.value;
            int baseWood = (int) InitialKingdomResources.INITIAL_SUCCESSOR_WOOD.value;
            int baseTreasury = (int) InitialKingdomResources.INITIAL_SUCCESSOR_TREASURY.value;
            int baseMorale = (int) InitialKingdomResources.INITIAL_SUCCESSOR_MORALE.value;

            float foodMult = "food".equals(arch.resourceBias) ? 1.6f : 1.0f;
            float treasuryMult = "treasury".equals(arch.resourceBias) ? 1.8f : 1.0f;
            float metalMult = "metal".equals(arch.resourceBias) ? 2.0f : 1.0f;

            if (ashes.famineCollapse) foodMult *= 0.5f; // the land itself is exhausted

            k.food = Math.max(50, (int) (baseFood * foodMult * prod));
            k.wood = (int) (baseWood * prod);
            k.stone = (int) (baseWood * prod);
            k.metal = (int) (40 * metalMult);
            k.gold = (int) (baseTreasury * treasuryMult);
            k.storyProductionModifier = prod;

            int unrest = arch.startUnrest;
            if (ashes.radicalCollapse) unrest += 150;        // a violent birth carries over
            unrest += (int) (Math.random() * 80);            // chaos noise
            k.unrestLevel = unrest;

            k.armyMorale = clampMorale(baseMorale + arch.moraleBonus + (ashes.finalMorale - 60) / 4);

            // reset inherited Empire penalties
            k.limitersDisabled = false;
            k.divineProductionModifier = 1.0f;
            k.divineTaxModifier = 1.0f;
            k.divinePenaltyTimerDays = 0;
            k.canUseDivineIntervention = true;

            Logger.logEvent("Born from the ashes: " + k.name
                    + " (unrest " + k.unrestLevel + ", prod x" + String.format("%.2f", prod) + ")", STORY);
        }

        redistributePopulation(kingdoms, population, pool);
    }

    // Shuffle the seven archetypes, then force the one that matches the collapse
    // fingerprint into the lead slot so the "first" successor reflects the death.
    private static KingdomArchetype[] pickArchetypes(EmpireAshes ashes) {
        List<KingdomArchetype> list = new ArrayList<>(Arrays.asList(KingdomArchetype.values()));
        Collections.shuffle(list);

        KingdomArchetype lead;
        if (ashes.radicalCollapse) lead = KingdomArchetype.NOMAD_HORDE;
        else if (ashes.famineCollapse) lead = KingdomArchetype.AGRARIAN_COMMUNE;
        else if (ashes.militaristCollapse) lead = KingdomArchetype.FORTRESS_REMNANT;
        else lead = KingdomArchetype.MERCHANT_REPUBLIC;

        list.remove(lead);
        list.add(0, lead);
        return list.toArray(new KingdomArchetype[0]);
    }

    private static void redistributePopulation(Kingdom[] kingdoms, List<Human> population, KingdomArchetype[] pool) {
        int n = kingdoms.length - 1; // successor slots: ids 1..n

        for (Human h : population) {
            if (!h.isAlive) continue;

            int slot = (int) (Math.random() * n); // 0..n-1
            int kid = slot + 1;
            KingdomArchetype arch = pool[slot % pool.length];
            h.kingdomId = kid;

            boolean militant = arch == KingdomArchetype.WARLORD_STATE
                    || arch == KingdomArchetype.FORTRESS_REMNANT
                    || arch == KingdomArchetype.NOMAD_HORDE;

            if (h.job == Military.REBEL.value) {
                // Rebels keep fighting only where the new state is built on force.
                h.job = militant ? Military.SWORDSMAN.value : CivilJobs.FARMER.value;
                h.isGeneral = militant && h.isGeneral; // rebel leaders become officers in militant states
            } else if (h.job >= 6 && h.job <= 8) {
                // Standing soldiers disband to farms unless their new home stays armed.
                if (!militant) {
                    h.job = CivilJobs.FARMER.value;
                    h.isGeneral = false;
                }
            }
            // Civilians (jobs 1-5) and their stats/quirks carry over untouched.
        }

        for (int i = 1; i < kingdoms.length; i++) {
            final int id = i;
            kingdoms[i].population = (int) population.stream()
                    .filter(h -> h.isAlive && h.kingdomId == id).count();
            kingdoms[i].updateCensus(population);
        }
    }

    private static String successorName(int id) {
        return NAMES[(id - 1) % NAMES.length];
    }

    private static int clampMorale(int m) {
        return Math.max(0, Math.min(100, m));
    }
}