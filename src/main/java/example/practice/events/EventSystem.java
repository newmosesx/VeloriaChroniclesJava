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
public class EventSystem {

    private static final int N = EventType.values().length;
    private static final int[][] lastFired = new int[64][N];
    static { for (int[] row : lastFired) Arrays.fill(row, -100); }

    public static void process(Kingdom k, List<Human> population, World world, int day) {
        if (!k.isActive || k.population < 50) return;

        int pop = 0, soldiers = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != k.id) continue;
            pop++;
            if (h.job >= 6 && h.job <= 8) soldiers++;
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
            case DROUGHT:
                if (!world.calendar.growingSeason) return false;
                if (avgSoil(world) >= EventConfig.DROUGHT_SOIL_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                for (int s = 0; s < world.agriculture.soilMoisture.length; s++)
                    world.agriculture.soilMoisture[s] = Math.max(0f,
                            world.agriculture.soilMoisture[s] - EventConfig.DROUGHT_SOIL_HIT.value);
                Logger.logEvent("A drought grips " + k.name + " - the soil cracks.", NATURAL);
                return true;

            case FLOOD:
                if (maxFlood(world) < EventConfig.FLOOD_SEVERITY_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                aggravate(k, FactionType.COMMONS, EventConfig.SHOCK_AGGRAVATE.value);
                Logger.logEvent("Floodwaters swamp the lowlands of " + k.name + ".", NATURAL);
                return true;

            case PLAGUE:
                if (world.agriculture.landDensityForKingdom(k.id, population) >= EventConfig.PLAGUE_DENSITY_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                killCivilians(pop, k.id, (int) (population * EventConfig.PLAGUE_DEATH_FRACTION.value));
                aggravate(k, FactionType.CLERGY, EventConfig.SHOCK_AGGRAVATE.value);
                aggravate(k, FactionType.COMMONS, EventConfig.SHOCK_AGGRAVATE.value);
                Logger.logEvent("Plague spreads through the crowded quarters of " + k.name + ".", NATURAL);
                return true;

            case BARBARIAN_RAID:
                if (armyShare >= EventConfig.RAID_ARMY_SHARE_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                k.wood = (int) (k.wood * (1f - EventConfig.RAID_RESOURCE_LOSS.value));
                k.stone = (int) (k.stone * (1f - EventConfig.RAID_RESOURCE_LOSS.value));
                killCivilians(pop, k.id, (int) (population * EventConfig.RAID_DEATH_FRACTION.value));
                aggravate(k, FactionType.NOBILITY, EventConfig.SHOCK_AGGRAVATE.value);
                Logger.logEvent("Barbarians raid the under-defended borders of " + k.name + "!", MILITARY);
                return true;

            case INTRIGUE: {
                Faction hot = hottestFaction(k);
                if (hot == null || hot.grievance < EventConfig.INTRIGUE_GRIEVANCE_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                hot.aggravate(EventConfig.SHOCK_AGGRAVATE.value); // a plot deepens the rift
                Logger.logEvent("A plot stirs among " + hot.type.title + " in " + k.name + ".", POLITICAL);
                return true;
            }

            case BOUNTIFUL_HARVEST:
                if (!world.calendar.growingSeason) return false;
                if (world.agriculture.realmAverageYield() < EventConfig.HARVEST_YIELD_THRESHOLD.value) return false;
                if (!roll(EventConfig.FIRE_CHANCE.value)) return false;
                k.food += (int) (population * EventConfig.HARVEST_FOOD_PER_HEAD.value);
                Logger.logEvent("A bountiful harvest fills the granaries of " + k.name + "!", NATURAL);
                return true;

            case GOLD_STRIKE:
                if (!roll(EventConfig.GOLD_STRIKE_CHANCE.value)) return false;
                k.gold += (int) EventConfig.GOLD_STRIKE_AMOUNT.value;
                Logger.logEvent("A vein of gold is struck in " + k.name + "!", NATURAL);
                return true;
        }
        return false;
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