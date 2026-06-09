package example.practice.engine;

import example.practice.agents.Agent;
import example.practice.agents.AgentRoster;
import example.practice.config.Battle;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;

import java.util.List;

import static example.practice.logger.Logger.LogCategory.MILITARY;
import static example.practice.logger.Logger.LogCategory.STORY;

/**
 * The "Attack" command turns the hourly skirmish grind into one real battle a day,
 * fought at a random hour, led by Castius (imperial) against Joric (rebel).
 *
 * Two command modes for the empire's side:
 *   - AUTO  : Castius resolves the whole battle on the sim thread (no UI).
 *   - MANUAL: the battle is parked as 'pending'; the GUI walks its 5 windows with
 *             the player's decisions, then calls applyManual().
 *
 * Leader fates (the template you asked for):
 *   - Lose the battle and your commander falls. If CASTIUS falls, automatic army
 *     control is lost (auto is disabled; ATTACK stands down). If JORIC falls, the
 *     rebellion is leaderless and will not give battle again until a new leader
 *     arrives -- which is your hook for introducing the next character.
 *
 * Skirmishes are disabled while posture == ATTACK (see ConflictManager.tryEngage).
 */
public final class BattleManager {

    private BattleManager() {}

    private static final String CASTIUS = "General Castius";
    private static final String JORIC   = "Joric";

    // scheduling
    private static int lastDay = -1;
    private static int battleHourToday = -1;
    private static boolean foughtToday = false;

    // command + leader state
    private static boolean manualMode = false;        // player commands the windows?
    private static boolean imperialCommandLost = false; // Castius has fallen
    private static boolean rebelsDisabled = false;      // Joric has fallen

    // GUI handoff slots
    private static FieldBattle pending;   // a manual battle awaiting the decision window
    private static BattleReport finished; // a resolved battle awaiting the cinematic

    // ---- player/GUI controls ----
    public static synchronized void setManual(boolean m) { manualMode = m && !imperialCommandLost; }
    public static synchronized boolean isManual()        { return manualMode; }
    public static synchronized boolean commandLost()     { return imperialCommandLost; }
    public static synchronized boolean rebelsDisabled()  { return rebelsDisabled; }

    public static synchronized FieldBattle consumePending() { FieldBattle f = pending; pending = null; return f; }
    public static synchronized BattleReport consumeFinished() { BattleReport r = finished; finished = null; return r; }

    /** Called hourly from the engine for the empire. Fires at most one battle per day. */
    public static void tick(Kingdom k, List<Human> pop, AgentRoster roster, int day, int hour) {
        if (k == null || !k.isActive) return;
        if (ConflictManager.posture(k) != MilitaryPosture.ATTACK) return;

        if (day != lastDay) { lastDay = day; battleHourToday = (int) (Math.random() * 24); foughtToday = false; }
        if (foughtToday || hour != battleHourToday) return;

        // Leaders gate the fight.
        boolean castius = alive(roster, CASTIUS);
        boolean joric   = alive(roster, JORIC);
        if (rebelsDisabled || !joric) {
            rebelsDisabled = true;
            foughtToday = true;
            Logger.logEvent("The rebellion has no field commander; they give no battle.", MILITARY);
            return;
        }
        if (!castius) {
            imperialCommandLost = true;
            if (!manualMode) {             // no Castius, no auto -- the order can't be carried out
                foughtToday = true;
                Logger.logEvent("With Castius gone, no one can command the assault. The army holds.", MILITARY);
                return;
            }
        }

        int soldiers = k.jobCounts[6] + k.jobCounts[7] + k.jobCounts[8];
        int rebels   = k.jobCounts[9];
        if (soldiers < 2 || rebels < 2) { foughtToday = true; return; }

        foughtToday = true;

        float frac = Battle.INITIAL_COMMIT_FRACTION.value;
        int impCommit = Math.max(1, (int) (soldiers * frac));
        int rebCommit = Math.max(1, (int) (rebels * frac));
        FieldBattle fb = new FieldBattle(
                "The Field", impCommit, soldiers - impCommit, ConflictManager.imperialMul(k),
                rebCommit, rebels - rebCommit, ConflictManager.rebelMul(k));

        if (manualMode && !imperialCommandLost) {
            synchronized (BattleManager.class) { pending = fb; }   // the GUI will run the windows
            Logger.logEvent("The army forms up for battle -- the Emperor's orders are awaited.", MILITARY);
            return;
        }

        // AUTO: Castius fights it through here and now.
        while (!fb.isOver()) fb.step(FieldBattle.AutoCommander.decide(fb.assess(FieldBattle.Side.IMPERIAL)));
        applyResult(k, pop, roster, fb.report);
    }

    /** Apply a battle the player resolved through the decision windows. Locks the engine. */
    public static void applyManual(SimulationEngine engine, Kingdom k, List<Human> pop, AgentRoster roster, FieldBattle fb) {
        engine.lock();
        try { applyResult(k, pop, roster, fb.report); }
        finally { engine.unlock(); }
    }

    // ---- outcome -> world ----
    private static void applyResult(Kingdom k, List<Human> pop, AgentRoster roster, BattleReport r) {
        // Casualties to the actual ranks.
        k.inflictProportionalCasualties(pop, k.id, r.impLosses(), r.rebLosses());

        // Morale follows the field.
        if (r.empireWon()) k.modifyMorale((int) Battle.MORALE_GAIN_ON_VICTORY.value);
        else               k.modifyMorale(-(int) Battle.MORALE_LOSS_ON_DEFEAT.value);

        // The loser's commander falls.
        if (Battle.LEADER_FALLS_ON_LOSS.value >= 1f && r.winner != null) {
            if (r.empireWon()) {
                r.rebelLeaderDown = fell(roster, JORIC);
                if (r.rebelLeaderDown) {
                    rebelsDisabled = true;
                    Logger.logEvent("Joric falls on the field. The rebellion is leaderless -- it will not attack again until another rises.", STORY);
                }
            } else {
                r.imperialLeaderDown = fell(roster, CASTIUS);
                if (r.imperialLeaderDown) {
                    imperialCommandLost = true;
                    manualMode = false;
                    ConflictManager.setPosture(k, MilitaryPosture.GARRISON); // the assault is called off
                    Logger.logEvent("General Castius falls. Automatic command of the army is lost to the Emperor.", STORY);
                }
            }
        }

        Logger.logEvent("BATTLE: " + (r.empireWon() ? "Imperial victory" : "Rebel victory")
                + (r.annihilation ? " (annihilation)" : " (the field is yielded)") + " -- "
                + r.impCommitted + " imperials (" + r.impSurvivors + " left) vs "
                + r.rebCommitted + " rebels (" + r.rebSurvivors + " left).", MILITARY);

        synchronized (BattleManager.class) { finished = r; }   // hand to the GUI for the cinematic
    }

    private static boolean alive(AgentRoster roster, String name) {
        if (roster == null) return false;
        Agent a = roster.get(name);
        return a != null && a.alive;
    }
    private static boolean fell(AgentRoster roster, String name) {
        if (roster == null) return false;
        Agent a = roster.get(name);
        if (a != null && a.alive) { a.alive = false; return true; }
        return false;
    }

    // ---- save / load ----
    public static String exportState() {
        return (manualMode ? 1 : 0) + "," + (imperialCommandLost ? 1 : 0) + "," + (rebelsDisabled ? 1 : 0);
    }
    public static void importState(String blob) {
        if (blob == null || blob.isEmpty()) return;
        String[] v = blob.split(",");
        if (v.length >= 3) {
            manualMode = "1".equals(v[0]);
            imperialCommandLost = "1".equals(v[1]);
            rebelsDisabled = "1".equals(v[2]);
        }
        lastDay = -1; foughtToday = false; pending = null; finished = null;
    }
}