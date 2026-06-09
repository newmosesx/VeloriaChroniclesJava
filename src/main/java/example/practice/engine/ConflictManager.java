package example.practice.engine;

import example.practice.agents.Agent;
import example.practice.agents.AgentRoster;
import example.practice.config.FactionType;
import example.practice.config.Rebellion;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.world.World;

import java.util.List;

/**
 * The spine of the military / rebel systems.
 *
 * Per kingdom it owns a ConflictState that gates all organic combat, a rebellion
 * "organization" meter, an army posture + readiness, and per-sector control of
 * the empire's land. Battles only happen in INSURGENCY/CIVIL_WAR, throttled to a
 * couple a day, located in contested sectors -- which is what turns 60 skirmishes
 * in 20 days into a handful of real engagements during an actual revolt.
 *
 * Leadership is wired to the agents: Mara Voss gates open revolt (organization),
 * Joric Fen multiplies rebel battle strength, General Castius multiplies imperial
 * strength. Kill a leader (agent panel) and that lever weakens.
 *
 * Called daily from SimulationEngine.processDay; queried hourly by CombatManager.
 */
public final class ConflictManager {

    private ConflictManager() {}

    private static final int K = 64;     // kingdom slots
    private static int SECTORS = 8;      // set on first run from geography

    // --- per-kingdom state ---
    private static final ConflictState[] state   = new ConflictState[K];
    private static final float[] organization     = new float[K];   // 0..1 rebellion organization
    private static final float[] readiness         = new float[K];   // 0..1 army readiness
    private static final MilitaryPosture[] posture = new MilitaryPosture[K];
    private static final boolean[] postureAuto     = new boolean[K];
    private static final int[] battlesToday        = new int[K];
    private static final float[] imperialMulCache  = new float[K];
    private static final float[] rebelMulCache     = new float[K];

    // --- the empire's land (shared 8 sectors) ---
    private static float[] sectorControl;   // +1 fully loyal .. -1 fully rebel
    private static float[] sectorPressure;  // 0..1 local rebellion pressure

    private static boolean inited = false;

    // tuning
    private static final double COMFORT_FOOD_DAYS = 30.0;
    private static final double PRESSURE_SMOOTH    = 0.20;

    // ---------------------------------------------------------------- queries
    public static ConflictState stateOf(Kingdom k) {
        ConflictState s = state[k.id];
        return s == null ? ConflictState.PEACE : s;
    }
    public static float organizationOf(Kingdom k) { return organization[k.id]; }
    public static float readinessOf(Kingdom k)    { return readiness[k.id]; }
    public static float sectorControl(int i)      { return (sectorControl != null && i < sectorControl.length) ? sectorControl[i] : 1f; }
    public static float sectorPressure(int i)     { return (sectorPressure != null && i < sectorPressure.length) ? sectorPressure[i] : 0f; }

    public static MilitaryPosture posture(Kingdom k) { return posture[k.id] == null ? MilitaryPosture.GARRISON : posture[k.id]; }
    public static boolean isAuto(Kingdom k)          { return postureAuto[k.id]; }
    public static void setAuto(Kingdom k, boolean auto) { postureAuto[k.id] = auto; }
    public static void setPosture(Kingdom k, MilitaryPosture p) { posture[k.id] = p; postureAuto[k.id] = false; }

    // multipliers cached each day, read hourly by CombatManager
    public static float imperialMul(Kingdom k) { float m = imperialMulCache[k.id]; return m <= 0 ? 1f : m; }
    public static float rebelMul(Kingdom k)    { float m = rebelMulCache[k.id];    return m <= 0 ? 1f : m; }

    /** Hourly gate for organic skirmishes. Returns true at most a couple times a day, only in revolt. */
    public static boolean tryEngage(Kingdom k) {
        if (posture(k) == MilitaryPosture.ATTACK) return false;
        ConflictState s = stateOf(k);
        if (s != ConflictState.INSURGENCY && s != ConflictState.CIVIL_WAR) return false;
        int cap = (s == ConflictState.CIVIL_WAR) ? 2 : 1;
        if (battlesToday[k.id] >= cap) return false;
        double intent = (s == ConflictState.CIVIL_WAR) ? 0.06 : 0.03;   // per hourly check
        if (Math.random() > intent) return false;
        battlesToday[k.id]++;
        return true;
    }

    // ---------------------------------------------------------------- daily tick
    public static void process(Kingdom k, List<Human> pop, World world, int day, AgentRoster roster) {
        if (!k.isActive) return;
        init(world);
        int id = k.id;
        battlesToday[id] = 0;   // fresh day

        // --- inputs ---
        double threshold = Rebellion.REBELLIONTHRESHOLD.value;
        double unrestNorm = clamp(k.unrestLevel / threshold, 0, 1);
        double foodDays = k.food / (k.population + 1.0);
        double hungerStress = foodDays < COMFORT_FOOD_DAYS ? clamp((COMFORT_FOOD_DAYS - foodDays) / COMFORT_FOOD_DAYS, 0, 1) : 0;
        double moraleNorm = clamp(k.armyMorale / 100.0, 0, 1);
        double armyLoyalty = armyLoyalty(k);

        int soldiers = k.jobCounts[6] + k.jobCounts[7] + k.jobCounts[8];
        int rebels = k.jobCounts[9];
        double armyShare = soldiers / (k.population + 1.0);
        double ratio = rebels / (soldiers + 1.0);

        boolean maraAlive = alive(roster, "Mara Voss");
        boolean joricAlive = alive(roster, "Joric");
        boolean castiusAlive = alive(roster, "General Castius");

        // --- per-sector pressure (this sector's land breeds its own revolt) ---
        double avgPressure = 0;
        int rebelSectors = 0;
        for (int i = 0; i < SECTORS; i++) {
            double yieldLack = 1 - clamp(world.agriculture.yield[i], 0, 1);
            double flood = clamp(world.water.floodSeverity[i], 0, 1);
            double drought = 1 - clamp(world.agriculture.soilMoisture[i], 0, 1);
            double raw = clamp(0.40 * unrestNorm + 0.25 * yieldLack + 0.20 * flood + 0.15 * drought + 0.30 * hungerStress, 0, 1);
            sectorPressure[i] += (raw - sectorPressure[i]) * PRESSURE_SMOOTH;
            avgPressure += sectorPressure[i];
        }
        avgPressure /= SECTORS;

        // --- rebellion organization ---
        double grow = avgPressure * 0.06 + unrestNorm * 0.04 + (k.limitersDisabled ? 0.03 : 0) + (maraAlive ? 0.02 : 0);
        double suppress = (posture(k) == MilitaryPosture.PATROL ? 0.5 : posture(k) == MilitaryPosture.MOBILIZE ? 0.8 : 0.1)
                * armyShare * moraleNorm * 0.15;
        double relief = suppress + 0.01 + ((hungerStress < 0.1 && unrestNorm < 0.2) ? 0.03 : 0) + (maraAlive ? 0 : 0.04);
        double cap = maraAlive ? 1.0 : 0.5;   // a leaderless movement can't fully cohere
        organization[id] = (float) clamp(organization[id] + grow - relief, 0, cap);

        // --- sector control drift + neighbour spread ---
        for (int i = 0; i < SECTORS; i++) {
            double loyalPush = (posture(k) == MilitaryPosture.PATROL || posture(k) == MilitaryPosture.MOBILIZE ? 0.04 : 0.01) * armyShare * 4;
            double rebelPush = sectorPressure[i] * organization[id] * (0.20 + (0.19 * ratio));
            sectorControl[i] = (float) clamp(sectorControl[i] + loyalPush - rebelPush, -1, 1);
        }
        float[] spread = sectorControl.clone();
        for (int i = 0; i < SECTORS; i++) {
            float nb = (sectorControl[(i - 1 + SECTORS) % SECTORS] + sectorControl[(i + 1) % SECTORS]) / 2f;
            spread[i] = (float) clamp(sectorControl[i] + (nb - sectorControl[i]) * 0.05, -1, 1);
        }
        sectorControl = spread;
        for (int i = 0; i < SECTORS; i++) if (sectorControl[i] < 0) rebelSectors++;
        double minControl = 1;
        for (int i = 0; i < SECTORS; i++) minControl = Math.min(minControl, sectorControl[i]);

        // --- state machine (hysteresis: escalate hard, de-escalate slow) ---
        ConflictState cur = stateOf(k);
        ConflictState next = cur;
        switch (cur) {
            case PEACE:
                if (organization[id] > 0.22 || unrestNorm > 0.30) next = ConflictState.TENSION;
                break;
            case TENSION:
                if (organization[id] > 0.50 && maraAlive && minControl < 0.0 || k.unrestLevel >= 400) next = ConflictState.INSURGENCY; // Mara opens the revolt
                else if (organization[id] < 0.12 && unrestNorm < 0.15) next = ConflictState.PEACE;
                break;
            case INSURGENCY:
                if (ratio > 1.0 || rebelSectors >= 2 || unrestNorm > 0.85) next = ConflictState.CIVIL_WAR;
                else if (organization[id] < 0.30 || rebels < 5) next = ConflictState.TENSION;
                break;
            case CIVIL_WAR:
                if (rebels < 10 || ratio < 0.30) next = ConflictState.INSURGENCY;
                break;
            case RESOLUTION:
                if (organization[id] < 0.10) next = ConflictState.PEACE;
                break;
        }
        if ((cur == ConflictState.INSURGENCY || cur == ConflictState.CIVIL_WAR) && rebels == 0) next = ConflictState.RESOLUTION;
        state[id] = next;

        // --- posture AI (respect override) ---
        if (postureAuto[id] || posture[id] == null) {
            if (next == ConflictState.PEACE)      posture[id] = readiness[id] < 0.5 ? MilitaryPosture.DRILL : MilitaryPosture.GARRISON;
            else if (next == ConflictState.TENSION) posture[id] = MilitaryPosture.PATROL;
            else                                   posture[id] = MilitaryPosture.MOBILIZE;
        }

        // --- readiness ---
        switch (posture(k)) {
            case DRILL:    readiness[id] += 0.03; break;
            case MOBILIZE: readiness[id] -= 0.01; break;
            default:       readiness[id] += (0.5f - readiness[id]) * 0.02f; break;
        }
        readiness[id] = (float) clamp(readiness[id] - hungerStress * 0.02, 0, 1);
        // A provisioned army sharpens; one on empty rations dulls. (militaryFood is
        // filled from the farming surplus in SubsistenceManager.)
        double fed = clamp(k.militaryFood / (soldiers * 2.0 + 1.0), 0, 1);
        readiness[id] = (float) clamp(readiness[id] + (fed - 0.5) * 0.02, 0, 1);

        // --- conversion: disloyal, underpaid soldiers defect to the rebellion ---
        if ((next == ConflictState.INSURGENCY || next == ConflictState.CIVIL_WAR)
                && armyLoyalty < 0.65 && organization[id] > 0.7) {
            double frac = clamp((1 - armyLoyalty) * organization[id] * 0.06, 0, 0.08);
            if (frac > 0.01) k.triggerMassDesertion(pop, frac);
        }

        // --- cache combat multipliers for the hourly skirmish ---
        imperialMulCache[id] = (float) clamp(1.0 + (castiusAlive ? 0.25 : 0.0) + (readiness[id] - 0.5) * 0.2, 0.7, 1.8);
        rebelMulCache[id]    = (float) clamp(1.0 + (joricAlive ? 0.25 : 0.0) + organization[id] * 0.2, 0.7, 1.8);
    }

    // ---------------------------------------------------------------- helpers
    private static void init(World world) {
        if (inited) return;
        SECTORS = world.geography.count();
        sectorControl = new float[SECTORS];
        sectorPressure = new float[SECTORS];
        for (int i = 0; i < SECTORS; i++) sectorControl[i] = 1f;   // start loyal
        for (int i = 0; i < K; i++) { readiness[i] = 0.5f; postureAuto[i] = true; }
        inited = true;
    }

    private static double armyLoyalty(Kingdom k) {
        try {
            Faction[] f = PoliticsManager.factionsOf(k);
            int idx = FactionType.ARMY.ordinal();
            if (f != null && idx < f.length && f[idx] != null) return clamp(1.0 - f[idx].grievance, 0, 1);
        } catch (Exception ignored) {}
        return 0.7;   // neutral-ish default
    }

    private static boolean alive(AgentRoster roster, String name) {
        if (roster == null) return false;
        Agent a = roster.get(name);
        return a != null && a.alive;
    }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }
    // ------------------------------------------------------------- save / load
    /** Snapshot of all persistent conflict state (used by SaveManager). */
    public static String exportState() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < K; i++) {
            if (i > 0) b.append(';');
            int st = state[i] == null ? 0 : state[i].ordinal();
            int po = posture[i] == null ? 0 : posture[i].ordinal();
            b.append(st).append(',').append(organization[i]).append(',').append(readiness[i])
                    .append(',').append(po).append(',').append(postureAuto[i] ? 1 : 0);
        }
        b.append('#');
        int len = sectorControl == null ? 0 : sectorControl.length;
        b.append(len);
        for (int i = 0; i < len; i++) b.append(',').append(sectorControl[i]).append(',').append(sectorPressure[i]);
        return b.toString();
    }

    /** Restore everything exportState() wrote; safe to call before any tick. */
    public static void importState(String blob) {
        if (blob == null || blob.isEmpty()) return;
        String[] halves = blob.split("#", 2);
        String[] kp = halves[0].split(";");
        for (int i = 0; i < kp.length && i < K; i++) {
            String[] v = kp[i].split(",");
            if (v.length < 5) continue;
            state[i] = ConflictState.values()[Integer.parseInt(v[0])];
            organization[i] = Float.parseFloat(v[1]);
            readiness[i] = Float.parseFloat(v[2]);
            posture[i] = MilitaryPosture.values()[Integer.parseInt(v[3])];
            postureAuto[i] = v[4].equals("1");
        }
        if (halves.length > 1 && !halves[1].isEmpty()) {
            String[] s = halves[1].split(",");
            int len = Integer.parseInt(s[0]);
            sectorControl = new float[len];
            sectorPressure = new float[len];
            SECTORS = len;
            for (int i = 0; i < len; i++) {
                int base = 1 + i * 2;
                if (base + 1 < s.length) {
                    sectorControl[i] = Float.parseFloat(s[base]);
                    sectorPressure[i] = Float.parseFloat(s[base + 1]);
                }
            }
        }
        inited = true;   // don't let init() wipe what we just restored
    }
}