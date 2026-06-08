package example.practice.engine;

import example.practice.config.EdictType;
import example.practice.config.FactionType;
import example.practice.config.SubsistenceConfig;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;

import java.util.EnumSet;
import java.util.List;

// The emperor's hands. Two edicts at a time, each paid for every day it stays on.
// Effects act THROUGH the systems - they ease or inflame faction grievances, send
// rebels home, lift the levy ceiling - never poke the unrest number directly.
//
// NOTE: the treasury is read/written as kingdom.gold. If your gold field is named
// differently (treasury, bronze...), change it everywhere k.gold appears.
public class EdictManager {
    public static final int MAX_ACTIVE = 2;

    @SuppressWarnings("rawtypes")
    private static final EnumSet[] active = new EnumSet[64];

    @SuppressWarnings("unchecked")
    private static EnumSet<EdictType> set(Kingdom k) {
        if (active[k.id] == null) active[k.id] = EnumSet.noneOf(EdictType.class);
        return (EnumSet<EdictType>) active[k.id];
    }

    public static boolean isActive(Kingdom k, EdictType e) { return set(k).contains(e); }
    public static EnumSet<EdictType> activeEdicts(Kingdom k) { return set(k); }

    // Manager-window switch: green when this returns true after a toggle, red when not.
    public static boolean toggle(Kingdom k, EdictType e) {
        if (isActive(k, e)) { set(k).remove(e); return false; }
        if (set(k).size() >= MAX_ACTIVE) return false; // only two slots
        set(k).add(e);
        return true;
    }
    public static void deactivate(Kingdom k, EdictType e) { set(k).remove(e); }

    // Read by ManpowerManager: emergency conscription lifts the levy ceiling.
    public static float ceilingBonus(Kingdom k) {
        return isActive(k, EdictType.EMERGENCY_CONSCRIPTION) ? 0.20f : 0f;
    }

    // Run once a day per kingdom, BEFORE PoliticsManager (so relief shapes today's
    // grievances and unrest). Unaffordable edicts lapse - their switch goes red.
    public static void processDaily(Kingdom kingdom, List<Human> population) {
        if (!kingdom.isActive) return;
        EnumSet<EdictType> s = set(kingdom);
        if (s.isEmpty()) return;

        int pop = 0, rebels = 0;
        for (Human h : population) {
            if (!h.isAlive || h.kingdomId != kingdom.id) continue;
            pop++;
            if (h.job >= 9) rebels++;
        }

        for (EdictType e : EnumSet.copyOf(s)) {
            if (kingdom.gold < e.dailyCost) { deactivate(kingdom, e); continue; } // can't sustain it
            kingdom.gold -= e.dailyCost;
            apply(kingdom, population, e, pop, rebels);
        }
    }

    private static void apply(Kingdom k, List<Human> pop, EdictType e, int population, int rebels) {
        switch (e) {
            case GRAIN_DOLE:
                relieve(k, FactionType.COMMONS, 0.030f);
                k.food -= (int) (population * SubsistenceConfig.FOOD_PER_PERSON.value * 0.5f);
                if (k.food < 0) k.food = 0;
                break;
            case PUBLIC_FESTIVAL:
                for (FactionType ft : FactionType.values()) relieve(k, ft, 0.012f);
                break;
            case MARTIAL_LAW:
                standDownRebels(pop, k.id, Math.max(1, (int) (rebels * 0.06f)));
                aggravate(k, FactionType.COMMONS, 0.015f);
                aggravate(k, FactionType.NOBILITY, 0.010f);
                break;
            case EMERGENCY_CONSCRIPTION:
                aggravate(k, FactionType.COMMONS, 0.020f); // ceiling lift is read by ManpowerManager
                break;
            case TEMPLE_PATRONAGE:
                relieve(k, FactionType.CLERGY, 0.040f);
                break;
            case LAND_REFORM:
                relieve(k, FactionType.COMMONS, 0.030f);
                aggravate(k, FactionType.NOBILITY, 0.030f);
                break;
            case TAX_RELIEF:
                relieve(k, FactionType.COMMONS, 0.020f);
                relieve(k, FactionType.MERCHANTS, 0.020f);
                break;
        }
    }

    private static void relieve(Kingdom k, FactionType ft, float amt) {
        Faction f = PoliticsManager.factionsOf(k)[ft.ordinal()];
        f.grievance = Math.max(0f, f.grievance - amt);
    }
    private static void aggravate(Kingdom k, FactionType ft, float amt) {
        PoliticsManager.factionsOf(k)[ft.ordinal()].aggravate(amt);
    }
    private static void standDownRebels(List<Human> population, int kid, int count) {
        int n = population.size();
        if (n == 0) return;
        int done = 0, att = 0;
        while (done < count && att < n * 3) {
            Human h = population.get((int) (Math.random() * n));
            if (h.isAlive && h.kingdomId == kid && h.job >= 9) { h.job = 0; done++; }
            att++;
        }
    }
}