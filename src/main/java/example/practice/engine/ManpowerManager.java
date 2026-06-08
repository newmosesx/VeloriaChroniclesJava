package example.practice.engine;

import example.practice.config.MobilizationConfig;
import example.practice.config.SubsistenceConfig;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;

import java.util.List;

// Keeps the realm from putting every hand under arms. The army can only be as large
// as the economy can feed - except when Emergency Conscription is active, which
// lifts the ceiling for as long as you can pay for it. Soldiers over the ceiling
// demobilise; rebels stand down as grievances cool.
//
// NOTE on job ids: 6-8 are soldiers, 9+ are rebels.
public class ManpowerManager {

    private static boolean isSoldier(int job) { return job >= 6 && job <= 8; }
    private static boolean isRebel(int job)   { return job >= 9; }

    // The conscription edict lifts the levy ceiling while it is active and paid for.
    private static float ceiling(Kingdom k) {
        return MobilizationConfig.SOLDIER_CEILING.value + EdictManager.ceilingBonus(k);
    }

    public static void process(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive) return;

        int pop = 0, soldiers = 0, rebels = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            pop++;
            if (isSoldier(h.job)) soldiers++;
            else if (isRebel(h.job)) rebels++;
        }
        if (pop <= 0) return;

        int maxSoldiers = (int) (pop * ceiling(kingdom) * foodSecurity(kingdom, pop));
        if (soldiers > maxSoldiers)
            standDown(population, kingdom.id, soldiers - maxSoldiers, true);

        int maxRebels = (int) (pop * MobilizationConfig.REBEL_CAP.value);
        if (rebels > maxRebels)
            standDown(population, kingdom.id, rebels - maxRebels, false);

        if (kingdom.unrestLevel < MobilizationConfig.REBEL_STANDDOWN_UNREST.value && rebels > 0) {
            int back = Math.max(1, (int) (rebels * MobilizationConfig.REBEL_STANDDOWN_RATE.value));
            standDown(population, kingdom.id, back, false);
        }
    }

    public static boolean canRecruit(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive) return false;
        int pop = 0, soldiers = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            pop++;
            if (isSoldier(h.job)) soldiers++;
        }
        if (pop <= 0) return false;
        int maxSoldiers = (int) (pop * ceiling(kingdom) * foodSecurity(kingdom, pop));
        return soldiers < maxSoldiers;
    }

    private static float foodSecurity(Kingdom kingdom, int pop) {
        float days = kingdom.food / Math.max(1f, pop * SubsistenceConfig.FOOD_PER_PERSON.value);
        float s = days / MobilizationConfig.SECURE_GRANARY_DAYS.value;
        return Math.max(MobilizationConfig.MIN_FOOD_SECURITY.value, Math.min(1f, s));
    }

    private static void standDown(List<Human> population, int kingdomId, int count, boolean soldiersOnly) {
        int n = population.size();
        if (n == 0 || count <= 0) return;
        int done = 0, attempts = 0;
        while (done < count && attempts < n * 3) {
            Human h = population.get((int) (Math.random() * n));
            boolean match = h.isAlive && h.kingdomId == kingdomId
                    && (soldiersOnly ? isSoldier(h.job) : isRebel(h.job));
            if (match) { h.job = 0; done++; }
            attempts++;
        }
    }
}