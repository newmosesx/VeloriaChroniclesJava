package example.practice.engine;

import example.practice.config.FactionType;
import example.practice.config.PoliticsConfig;
import example.practice.config.SubsistenceConfig;
import example.practice.config.TechEffect;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.world.World;

import java.util.List;

// Unrest = Σ(grievance × power) across five estates, all derived from world state.
// FIX: grievance now reads whether people are FED (granary), not whether the fields
// are green - so a dormant winter no longer floors unrest, and crowding only bites
// when you actually can't feed the crowd. A fed, peaceful realm can reach calm.
public class PoliticsManager {

    private static final Faction[][] byKingdom = new Faction[64][];

    public static Faction[] factionsOf(Kingdom kingdom) {
        if (byKingdom[kingdom.id] == null) {
            FactionType[] types = FactionType.values();
            Faction[] f = new Faction[types.length];
            for (int i = 0; i < types.length; i++) f[i] = new Faction(types[i]);
            byKingdom[kingdom.id] = f;
        }
        return byKingdom[kingdom.id];
    }

    public static void process(Kingdom kingdom, List<Human> population, World world) {
        if (!kingdom.isActive) return;
        Faction[] f = factionsOf(kingdom);

        int pop = 0, producers = 0, soldiers = 0, rebels = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            pop++;
            if (h.job >= 1 && h.job <= 5) producers++;
            else if (h.job >= 6 && h.job <= 8) soldiers++;
            else if (h.job >= 9) rebels++;
        }
        if (pop <= 0) return;

        // --- Real signals: are people FED, is the land overcrowded BEYOND feeding it ---
        float granaryDays = kingdom.food / Math.max(1f, pop * SubsistenceConfig.FOOD_PER_PERSON.value);
        float foodSecurity = clamp(granaryDays / PoliticsConfig.SECURE_GRANARY_DAYS.value);
        float hunger = 1f - foodSecurity;                       // the granary, not the dormant fields
        float crowding = clamp(1f - world.agriculture.landDensityForKingdom(kingdom.id, pop));
        float crowdPressure = crowding * hunger;                // crowding you can feed isn't a crisis
        float divineDispleasure = clamp(1f - kingdom.divineProductionModifier);
        float armyShare = soldiers / (float) pop;
        float rebelShare = rebels / (float) pop;
        float producerShare = producers / (float) pop;
        float disorder = clamp(2f * rebelShare + 0.4f * hunger);

        // --- Grievance targets ---
        f[FactionType.COMMONS.ordinal()].target   = clamp(0.65f * hunger + 0.30f * crowdPressure + 0.15f * armyShare);
        f[FactionType.ARMY.ordinal()].target      = clamp(0.55f * hunger + 0.25f * rebelShare);
        f[FactionType.CLERGY.ordinal()].target     = clamp(0.80f * divineDispleasure + 0.20f * disorder);
        f[FactionType.MERCHANTS.ordinal()].target   = clamp(0.60f * disorder + 0.30f * hunger);
        f[FactionType.NOBILITY.ordinal()].target    = clamp(0.50f * disorder + 0.30f * rebelShare);

        // --- Power: numbers for the masses, institution for the estates ---
        f[FactionType.COMMONS.ordinal()].power   = clamp(0.30f + 0.70f * producerShare);
        f[FactionType.ARMY.ordinal()].power      = clamp(0.20f + 3.0f * armyShare);
        f[FactionType.CLERGY.ordinal()].power     = FactionType.CLERGY.basePower;
        f[FactionType.MERCHANTS.ordinal()].power   = clamp(0.25f + 0.40f * foodSecurity);
        f[FactionType.NOBILITY.ordinal()].power    = FactionType.NOBILITY.basePower;

        float sumWeightedGrievance = 0f, sumPower = 0f;
        for (Faction fac : f) {
            fac.ease(PoliticsConfig.GRIEVANCE_EASE.value);
            sumWeightedGrievance += fac.grievance * fac.power;
            sumPower += fac.power;
        }
        float pressure = sumPower > 0f ? sumWeightedGrievance / sumPower : 0f;

        float order = 1f - TechManager.bonus(TechEffect.ORDER);
        if (order < 0.3f) order = 0.3f;
        kingdom.unrestLevel = (int) (pressure * PoliticsConfig.UNREST_FULL_SCALE.value * order);
    }

    public static FactionType dominantGrievance(Kingdom kingdom) {
        Faction[] f = factionsOf(kingdom);
        Faction top = f[0];
        for (Faction fac : f) if (fac.pressure() > top.pressure()) top = fac;
        return top.type;
    }

    public static String summary(Kingdom kingdom) {
        Faction[] f = factionsOf(kingdom);
        Faction a = f[0], b = f[0];
        for (Faction fac : f) {
            if (fac.pressure() > a.pressure()) { b = a; a = fac; }
            else if (fac.pressure() > b.pressure() && fac != a) b = fac;
        }
        return String.format("Unrest led by %s (%.0f%% angry) then %s (%.0f%%)",
                a.type.title, a.grievance * 100, b.type.title, b.grievance * 100);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}