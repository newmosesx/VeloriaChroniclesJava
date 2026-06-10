package example.practice.engine;

import example.practice.config.FactionType;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;

import java.util.HashMap;
import java.util.Map;

// The estates stop being a thermometer and start bargaining. When an estate's
// pressure crosses BOIL_OVER and no demand is already standing for the empire, it
// raises ONE demand (DemandManager.process, run each day after PoliticsManager).
// The player answers it in the manager window:
//   - CONCEDE: pay gold/food, ease the estate, anger its rival.
//   - REFUSE : pay nothing, the estate's grievance spikes.
// Ignore it long enough and patience runs out -> it auto-refuses. Every effect
// rides the existing Faction grievance model; unrest is never touched directly.
//
// Scope: only the player's empire (kingdom 0) raises demands - the AI realms run
// on raw grievance as before, so this adds player agency without new AI work.
public final class DemandManager {

    private DemandManager() {}

    public static final float BOIL_OVER = 0.62f;   // pressure that triggers a demand

    // One active demand per kingdom id (only the empire uses it in practice).
    private static final Map<Integer, FactionDemand> active = new HashMap<>();
    // Cooldown so a refused estate doesn't re-demand the very next day.
    private static final Map<Integer, Integer> cooldownUntilDay = new HashMap<>();
    private static final int COOLDOWN_DAYS = 6;

    public static FactionDemand current(Kingdom k) { return active.get(k.id); }
    public static boolean hasDemand(Kingdom k) { return active.containsKey(k.id); }

    // Run AFTER PoliticsManager.process so today's pressures are settled. Raises a
    // demand if one is warranted, and ages any standing demand toward expiry.
    public static void process(Kingdom kingdom, int day) {
        if (!kingdom.isActive) return;
        lastSeenDay = day;

        FactionDemand standing = active.get(kingdom.id);
        if (standing != null) {
            standing.daysLeft--;
            if (standing.expired()) {
                Logger.logEvent("The " + standing.from.title
                        + " grow cold - their demand went unanswered.", Logger.LogCategory.POLITICAL);
                applyRefusal(kingdom, standing);
                active.remove(kingdom.id);
                cooldownUntilDay.put(kingdom.id, day + COOLDOWN_DAYS);
            }
            return; // never two demands at once
        }

        Integer until = cooldownUntilDay.get(kingdom.id);
        if (until != null && day < until) return;

        Faction[] f = PoliticsManager.factionsOf(kingdom);
        Faction hottest = null;
        for (Faction fac : f) if (hottest == null || fac.pressure() > hottest.pressure()) hottest = fac;
        if (hottest == null || hottest.pressure() < BOIL_OVER) return;

        FactionDemand d = forge(hottest.type);
        if (d == null) return;
        active.put(kingdom.id, d);
        Logger.logEvent("The " + d.from.title + " press a demand upon the throne: "
                + d.headline, Logger.LogCategory.POLITICAL);
    }

    // Player chose to give in.
    public static void concede(Kingdom kingdom) {
        FactionDemand d = active.get(kingdom.id);
        if (d == null) return;
        if (kingdom.gold < d.goldCost) {            // can't actually afford the bargain
            Logger.logEvent("The throne cannot meet the " + d.from.title
                    + "' price - the offer falls through.", Logger.LogCategory.POLITICAL);
            return;
        }
        kingdom.gold -= d.goldCost;
        if (d.foodCost > 0) { kingdom.food -= d.foodCost; if (kingdom.food < 0) kingdom.food = 0; }

        relieve(kingdom, d.from, d.concedeRelief);
        if (d.rival != null && d.rivalBacklash > 0f) aggravate(kingdom, d.rival, d.rivalBacklash);

        Logger.logEvent("The throne concedes to the " + d.from.title + "."
                        + (d.rival != null ? " The " + d.rival.title + " seethe." : ""),
                Logger.LogCategory.POLITICAL);
        active.remove(kingdom.id);
        cooldownUntilDay.put(kingdom.id, lastSeenDay + COOLDOWN_DAYS);
    }

    // Player chose to refuse outright.
    public static void refuse(Kingdom kingdom) {
        FactionDemand d = active.get(kingdom.id);
        if (d == null) return;
        applyRefusal(kingdom, d);
        Logger.logEvent("The throne refuses the " + d.from.title + ". Their anger hardens.",
                Logger.LogCategory.POLITICAL);
        active.remove(kingdom.id);
        cooldownUntilDay.put(kingdom.id, lastSeenDay + COOLDOWN_DAYS);
    }

    private static void applyRefusal(Kingdom kingdom, FactionDemand d) {
        aggravate(kingdom, d.from, d.refuseBacklash);
    }

    // The catalogue: each estate's signature demand. Tuned so conceding always
    // trades one estate's calm for another's resentment - never a free win.
    private static FactionDemand forge(FactionType from) {
        switch (from) {
            case COMMONS:
                return new FactionDemand(FactionType.COMMONS, FactionType.NOBILITY,
                        "Open the granaries and lower the rents.",
                        "Grant relief", 600, 400,
                        0.45f, 0.18f, 0.22f, 8);
            case ARMY:
                return new FactionDemand(FactionType.ARMY, FactionType.MERCHANTS,
                        "Pay the soldiers their arrears in full.",
                        "Pay the army", 1200, 0,
                        0.50f, 0.15f, 0.30f, 6);
            case CLERGY:
                return new FactionDemand(FactionType.CLERGY, FactionType.COMMONS,
                        "Endow the temples and proclaim a holy day.",
                        "Endow the temples", 700, 0,
                        0.45f, 0.12f, 0.20f, 8);
            case MERCHANTS:
                return new FactionDemand(FactionType.MERCHANTS, FactionType.COMMONS,
                        "Cut the tariffs strangling the trade roads.",
                        "Cut the tariffs", 900, 0,
                        0.45f, 0.15f, 0.20f, 7);
            case NOBILITY:
                return new FactionDemand(FactionType.NOBILITY, FactionType.COMMONS,
                        "Confirm the old estates and their privileges.",
                        "Confirm privileges", 800, 0,
                        0.45f, 0.20f, 0.25f, 7);
        }
        return null;
    }

    private static void relieve(Kingdom k, FactionType ft, float amt) {
        Faction f = PoliticsManager.factionsOf(k)[ft.ordinal()];
        f.grievance = Math.max(0f, f.grievance - amt);
    }
    private static void aggravate(Kingdom k, FactionType ft, float amt) {
        PoliticsManager.factionsOf(k)[ft.ordinal()].aggravate(amt);
    }

    // Concede/refuse cooldown keys off the day last seen by process(), so a
    // resolved estate doesn't re-demand the very next tick.
    private static int lastSeenDay = 0;

    // For save/load: clear all standing demands (rebuilt naturally as days tick).
    public static void resetAll() { active.clear(); cooldownUntilDay.clear(); }
}