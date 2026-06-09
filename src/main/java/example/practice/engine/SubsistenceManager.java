package example.practice.engine;

import example.practice.config.SubsistenceConfig;
import example.practice.config.TechEffect;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.world.World;
import example.practice.logger.Logger;

import java.util.List;

import static example.practice.logger.Logger.LogCategory.NATURAL;

// The self-regulating food <-> population loop, and the single authority on food.
// Production (farmers x land x weather) fills a granary; the granary feeds the
// people; stored surplus drives births; an empty granary drives starvation. Land
// density caps yield as the realm crowds, so population settles at carrying
// capacity instead of exploding or dying. No controller, no divine intervention.
public class SubsistenceManager {

    // Fractional carry-over so births/deaths below 1 per day still accumulate.
    private static final float[] birthAcc = new float[64];
    private static final float[] deathAcc = new float[64];

    // Farmers now produce far more than the people's ration; the surplus provisions
    // the army from its own granary. The people's share stays FOOD_PER_FARMER (6).
    private static final float ARMY_FOOD_PER_FARMER = 19f; // ~25 total per farmer; 6 to the people, the rest to the legions
    private static final int   MILITARY_RATION      = 2;   // a soldier eats more than a civilian
    private static final int   MIL_STORE_DAYS       = 30;  // how long the army can stockpile rations

    public static void process(Kingdom kingdom, List<Human> population, World world) {
        if (!kingdom.isActive) return;

        int pop = 0, farmers = 0, soldiers = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            pop++;
            if (h.job == 1 || h.job == 2) farmers++;        // farmers + butchers feed people
            else if (h.job >= 6 && h.job <= 8) soldiers++;  // the standing army
        }
        if (pop <= 0) return;

        float fpf = SubsistenceConfig.FOOD_PER_FARMER.value;
        float fpp = SubsistenceConfig.FOOD_PER_PERSON.value;

        float climateYield = world.agriculture.yieldForKingdom(kingdom.id);
        float landDensity = world.agriculture.landDensityForKingdom(kingdom.id, pop);

        // Agriculture tech (heavy plough, crop rotation) raises food per farmer.
        float foodTech = 1f + TechManager.bonus(TechEffect.FOOD);
        float factor = foodTech * climateYield * landDensity;
        // The people's share is unchanged (FOOD_PER_FARMER); the surplus beyond it is
        // farmed for the legions and routed to the military granary further down.
        float production = farmers * fpf * factor;
        float need = pop * fpp;
        float available = kingdom.food + production;

        if (available >= need) {
            // Everyone eats; the rest is stored, up to the granary's capacity.
            int stored = (int) (available - need);
            // State granaries tech deepens how much the realm can stock.
            int cap = (int) (pop * fpp * SubsistenceConfig.GRANARY_DAYS.value
                    * (1f + TechManager.bonus(TechEffect.GRANARY)));
            kingdom.food = Math.min(stored, cap);

            // Births scale with how well-stocked the granary is per head.
            float fullAt = fpp * SubsistenceConfig.BIRTH_STORE_DAYS.value;
            float drive = Math.min(1f, (kingdom.food / (float) pop) / fullAt);
            birthAcc[kingdom.id] += pop * SubsistenceConfig.BIRTH_RATE.value * drive;
            int births = (int) birthAcc[kingdom.id];
            if (births > 0) {
                birthAcc[kingdom.id] -= births;
                for (int i = 0; i < births; i++) {
                    Human baby = new Human(kingdom.id);
                    baby.job = 0; // unemployed; the occupation sweep assigns work
                    population.add(baby);
                }
            }
        } else {
            // Stores empty and production short: the unfed begin to starve.
            float deficit = need - available;
            kingdom.food = 0;
            int unfed = (int) (deficit / fpp);
            deathAcc[kingdom.id] += unfed * SubsistenceConfig.STARVATION_FRACTION.value;
            if (!DailyEventTracker.famineLogged && unfed > 0) {
                Logger.logEvent("Stores are empty in " + kingdom.name + " - the people begin to starve.", NATURAL);
                DailyEventTracker.famineLogged = true;
            }
        }

        // --- MILITARY RATIONS ---------------------------------------------------
        // Farmers yield far more than the people receive; that surplus feeds the army
        // from its own granary, so the legions stay provisioned even as the commons
        // starve -- which is exactly why the commons increasingly choose to defect.
        int armyProduction = (int) (farmers * ARMY_FOOD_PER_FARMER * factor
                * (1f + TechManager.bonus(TechEffect.SUPPLY)));
        kingdom.militaryFood += armyProduction;
        kingdom.militaryFood -= soldiers * MILITARY_RATION;   // the legions eat first, and well
        if (kingdom.militaryFood < 0) kingdom.militaryFood = 0;
        int milCap = (int) (soldiers * MILITARY_RATION * MIL_STORE_DAYS);
        if (kingdom.militaryFood > milCap) kingdom.militaryFood = milCap;

        // Baseline mortality always ticks.
        deathAcc[kingdom.id] += pop * SubsistenceConfig.NATURAL_DEATH_RATE.value;
        int deaths = (int) deathAcc[kingdom.id];
        if (deaths > 0) {
            deathAcc[kingdom.id] -= deaths;
            killCommoners(population, kingdom.id, deaths);
        }
    }

    private static void killCommoners(List<Human> population, int kingdomId, int count) {
        int n = population.size();
        if (n == 0) return;
        int killed = 0, attempts = 0;
        while (killed < count && attempts < n * 2) {
            Human h = population.get((int) (Math.random() * n));
            if (h.isAlive && h.kingdomId == kingdomId && h.job >= 0 && h.job <= 5) {
                h.isAlive = false;
                h.job = 0;
                killed++;
            }
            attempts++;
        }
    }
}