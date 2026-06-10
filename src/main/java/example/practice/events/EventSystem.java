package example.practice.events;

import example.practice.config.FactionType;
import example.practice.engine.Faction;
import example.practice.engine.PoliticsManager;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;
import example.practice.world.World;

import java.util.Arrays;
import java.util.List;

import static example.practice.logger.Logger.LogCategory.MILITARY;
import static example.practice.logger.Logger.LogCategory.NATURAL;
import static example.practice.logger.Logger.LogCategory.POLITICAL;

// Emergent events. Instead of a daily dice roll, each day we SCAN the world the
// simulation already produced and surface what is genuinely true - a drought when
// the soil is dry, a flood when the rivers are over, a plague when the land is
// overcrowded, a raid when the army is thin. Every effect acts THROUGH a system
// (soil, food, population, resources, faction grievance) and never pokes unrest.
// Each event type has a per-kingdom cooldown so the world breathes instead of spams.
//
// DEEPENED: events now have SEVERITY (how far past the threshold the world is
// scales both the odds and the bite), and they leave SCARCITY behind (EventAftermath)
// that decays over days and feeds two CHAINED events - bread riots and price
// gouging - so a bad season compounds into a crisis instead of resetting each day.
public class EventSystem {

    private static final int N = EventType.values().length;
    private static final int[][] lastFired = new int[64][N];
    static { for (int[] row : lastFired) Arrays.fill(row, -100); }

    public static void process(Kingdom k, List<Human> population, World world, int day) {
        if (!k.isActive || k.population < 50) return;

        // The wake of past disasters fades a little each day before we scan.
        EventAftermath.decay(k.id);

        int pop = 0, soldiers = 0, producers = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != k.id) continue;
            pop++;
            if (h.job >= 6 && h.job <= 8) soldiers++;
            else if (h.job >= 1 && h.job <= 5) producers++;
        }
        if (pop <= 0) return;
        float armyShare = soldiers / (float) pop;

        for (EventType e : EventType.values()) {
            if (day - lastFired[k.id][e.ordinal()] < (int) EventConfig.COOLDOWN_DAYS.value) continue;
            if (tryFire(e, k, population, world, pop, armyShare)) {
                lastFired[k.id][e.ordinal()] = day;
            }
        }
    }

    private static boolean tryFire(EventType e, Kingdom k, List<Human> pop, World world, int population, float armyShare) {
        switch (e) {
            case DROUGHT: {
                if (!world.calendar.growingSeason) return false;
                float soil = avgSoil(world);
                if (soil >= EventConfig.DROUGHT_SOIL_THRESHOLD.value) return false;
                // severity: how far BELOW the dryness line we are (0..1).
                float sev = severity(EventConfig.DROUGHT_SOIL_THRESHOLD.value - soil,
                        EventConfig.DROUGHT_SOIL_THRESHOLD.value);
                if (!rollWithSeverity(sev)) return false;
                float hit = EventConfig.DROUGHT_SOIL_HIT.value * scale(sev);
                for (int s = 0; s < world.agriculture.soilMoisture.length; s++)
                    world.agriculture.soilMoisture[s] = Math.max(0f, world.agriculture.soilMoisture[s] - hit);
                EventAftermath.addScarcity(k.id, EventConfig.SCARCITY_FROM_DROUGHT.value * scale(sev));
                Logger.logEvent(sevWord(sev) + " drought grips " + k.name + " - the soil cracks.", NATURAL);
                return true;
            }

            case FLOOD: {
                float flood = maxFlood(world);
                if (flood < EventConfig.FLOOD_SEVERITY_THRESHOLD.value) return false;
                float sev = severity(flood - EventConfig.FLOOD_SEVERITY_THRESHOLD.value,
                        1f - EventConfig.FLOOD_SEVERITY_THRESHOLD.value);
                if (!rollWithSeverity(sev)) return false;
                aggravate(k, FactionType.COMMONS, EventConfig.SHOCK_AGGRAVATE.value * scale(sev));
                EventAftermath.addScarcity(k.id, EventConfig.SCARCITY_FROM_FLOOD.value * scale(sev));
                Logger.logEvent(sevWord(sev) + " flood swamps the lowlands of " + k.name + ".", NATURAL);
                return true;
            }

            case PLAGUE: {
                float density = world.agriculture.landDensityForKingdom(k.id, population);
                if (density >= EventConfig.PLAGUE_DENSITY_THRESHOLD.value) return false;
                float sev = severity(EventConfig.PLAGUE_DENSITY_THRESHOLD.value - density,
                        EventConfig.PLAGUE_DENSITY_THRESHOLD.value);
                if (!rollWithSeverity(sev)) return false;
                killCivilians(pop, k.id, (int) (population * EventConfig.PLAGUE_DEATH_FRACTION.value * scale(sev)));
                aggravate(k, FactionType.CLERGY, EventConfig.SHOCK_AGGRAVATE.value * scale(sev));
                aggravate(k, FactionType.COMMONS, EventConfig.SHOCK_AGGRAVATE.value * scale(sev));
                EventAftermath.addScarcity(k.id, EventConfig.SCARCITY_FROM_PLAGUE.value * scale(sev));
                Logger.logEvent(sevWord(sev) + " plague spreads through the crowded quarters of " + k.name + ".", NATURAL);
                return true;
            }

            case BARBARIAN_RAID: {
                if (armyShare >= EventConfig.RAID_ARMY_SHARE_THRESHOLD.value) return false;
                // A weakened, scarce realm is a riper target.
                float sev = severity(EventConfig.RAID_ARMY_SHARE_THRESHOLD.value - armyShare,
                        EventConfig.RAID_ARMY_SHARE_THRESHOLD.value);
                sev = Math.min(1f, sev + 0.5f * EventAftermath.scarcity(k.id));
                if (!rollWithSeverity(sev)) return false;
                float loss = EventConfig.RAID_RESOURCE_LOSS.value * scale(sev);
                k.wood = (int) (k.wood * (1f - loss));
                k.stone = (int) (k.stone * (1f - loss));
                killCivilians(pop, k.id, (int) (population * EventConfig.RAID_DEATH_FRACTION.value * scale(sev)));
                aggravate(k, FactionType.NOBILITY, EventConfig.SHOCK_AGGRAVATE.value * scale(sev));
                EventAftermath.addScarcity(k.id, EventConfig.SCARCITY_FROM_RAID.value * scale(sev));
                Logger.logEvent(sevWord(sev) + " raid strikes the under-defended borders of " + k.name + "!", MILITARY);
                return true;
            }

            case INTRIGUE: {
                Faction hot = hottestFaction(k);
                if (hot == null || hot.grievance < EventConfig.INTRIGUE_GRIEVANCE_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                hot.aggravate(EventConfig.SHOCK_AGGRAVATE.value); // a plot deepens the rift
                Logger.logEvent("A plot stirs among " + hot.type.title + " in " + k.name + ".", POLITICAL);
                return true;
            }

            case BOUNTIFUL_HARVEST: {
                if (!world.calendar.growingSeason) return false;
                if (world.agriculture.realmAverageYield() < EventConfig.HARVEST_YIELD_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                k.food += (int) (population * EventConfig.HARVEST_FOOD_PER_HEAD.value);
                EventAftermath.easeScarcity(k.id, EventConfig.SCARCITY_RELIEF_HARVEST.value); // relief heals the land
                Logger.logEvent("A bountiful harvest fills the granaries of " + k.name + "!", NATURAL);
                return true;
            }

            case GOLD_STRIKE:
                if (!roll(EventConfig.GOLD_STRIKE_CHANCE.value)) return false;
                k.gold += (int) EventConfig.GOLD_STRIKE_AMOUNT.value;
                Logger.logEvent("A vein of gold is struck in " + k.name + "!", NATURAL);
                return true;

            // --- CHAINED: only when an earlier disaster left scarcity behind ---
            case BREAD_RIOT: {
                float scar = EventAftermath.scarcity(k.id);
                if (scar < EventConfig.RIOT_SCARCITY_THRESHOLD.value) return false;
                // hunger sharpens it - the granary, not the fields.
                float sev = Math.min(1f, scar);
                if (!rollWithSeverity(sev)) return false;
                killCivilians(pop, k.id, (int) (population * EventConfig.RIOT_DEATH_FRACTION.value * scale(sev)));
                aggravate(k, FactionType.COMMONS, EventConfig.SHOCK_AGGRAVATE.value * 1.5f * scale(sev));
                Logger.logEvent("Hunger boils over - bread riots erupt in the streets of " + k.name + ".", POLITICAL);
                return true;
            }

            case PRICE_GOUGING: {
                float scar = EventAftermath.scarcity(k.id);
                if (scar < EventConfig.GOUGING_SCARCITY_THRESHOLD.value) return false;
                float sev = Math.min(1f, scar);
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                k.gold += (int) (EventConfig.GOUGING_GOLD_GAIN.value * scale(sev)); // skimmed off relief
                aggravate(k, FactionType.MERCHANTS, EventConfig.SHOCK_AGGRAVATE.value * 0.5f);  // emboldened
                aggravate(k, FactionType.COMMONS, EventConfig.SHOCK_AGGRAVATE.value * scale(sev)); // and resented
                Logger.logEvent("Merchants profiteer on the shortage in " + k.name + " while the poor go without.", POLITICAL);
                return true;
            }
        }
        return false;
    }

    // --- severity helpers ---------------------------------------------------
    // How far past a threshold we are, normalised 0..1 by the headroom available.
    private static float severity(float overshoot, float headroom) {
        if (headroom <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, overshoot / headroom));
    }
    // Effect multiplier: a marginal event ~1.0x, a catastrophe up to 1 + MAX_BONUS.
    private static float scale(float sev) {
        return 1f + EventConfig.SEVERITY_MAX_BONUS.value * sev;
    }
    // Worse conditions also surface a little more readily.
    private static boolean rollWithSeverity(float sev) {
        float chance = EventConfig.FIRE_CHANCE.value + EventConfig.SEVERITY_FIRE_BONUS.value * sev;
        return Math.random() < Math.min(0.95f, chance);
    }
    // A label so the log reads "A severe drought" vs "A mild drought".
    private static String sevWord(float sev) {
        if (sev >= 0.66f) return "A catastrophic";
        if (sev >= 0.33f) return "A severe";
        return "A mild";
    }

    private static float avgSoil(World w) {
        float[] m = w.agriculture.soilMoisture;
        float s = 0; for (float v : m) s += v;
        return m.length > 0 ? s / m.length : 1f;
    }
    private static float maxFlood(World w) {
        float mx = 0; for (float v : w.water.floodSeverity) if (v > mx) mx = v;
        return mx;
    }
    private static boolean roll(float chance) { return Math.random() < chance; }
    private static void aggravate(Kingdom k, FactionType ft, float amt) {
        PoliticsManager.factionsOf(k)[ft.ordinal()].aggravate(amt);
    }
    private static Faction hottestFaction(Kingdom k) {
        Faction[] f = PoliticsManager.factionsOf(k);
        Faction top = null;
        for (Faction x : f) if (top == null || x.grievance > top.grievance) top = x;
        return top;
    }
    private static void killCivilians(List<Human> pop, int kid, int count) {
        if (count <= 0) return;
        int n = pop.size(), done = 0, att = 0;
        while (done < count && att < n * 2) {
            Human h = pop.get((int) (Math.random() * n));
            if (h.isAlive && h.kingdomId == kid && h.job >= 1 && h.job <= 5) { h.isAlive = false; done++; }
            att++;
        }
    }
}