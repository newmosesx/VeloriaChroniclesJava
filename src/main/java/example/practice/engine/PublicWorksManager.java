package example.practice.engine;

import example.practice.events.EventAftermath;
import example.practice.kingdoms.Kingdom;
import example.practice.logger.Logger;
import example.practice.world.World;

// The throne's hands on the LAND. Three sector projects, each paid up front in
// wood/stone/gold and raised over days by labour you can watch on the map:
//
//   LEVEE      - on completion, raises Water.leveeHeight for that sector. The
//                water model already does the rest: a higher flood line that
//                shoves the overflow downstream (protect yourself, drown your
//                neighbour) and breaches if the flood is big enough.
//   IRRIGATION - standing structure: each day it draws river water onto dry
//                fields (riverLevel -> soilMoisture), so river sectors shrug
//                off droughts that crack the dryland sectors.
//   GRANARY    - standing structure: each one eases the realm's post-disaster
//                scarcity a little every day (EventAftermath), so a realm that
//                built ahead recovers from a bad year instead of rioting.
//
// Every effect rides an existing system array; nothing pokes unrest or yield
// directly. One project under construction per sector at a time.
public final class PublicWorksManager {

    private PublicWorksManager() {}

    public enum WorkType {
        LEVEE     ("Levee",      400, 600, 300, 10, "Raise the flood line. Overflow shoves downstream."),
        IRRIGATION("Irrigation", 300,   0, 400,  8, "Channel river water onto dry fields each day."),
        GRANARY   ("Granary",    500, 300, 350, 12, "Store the surplus; the realm recovers faster from a bad year.");

        public final String title;
        public final int wood, stone, gold, days;
        public final String description;
        WorkType(String t, int w, int s, int g, int d, String desc) {
            title = t; wood = w; stone = s; gold = g; days = d; description = desc;
        }
    }

    public enum State { NONE, BUILDING, BUILT }

    private static final float LEVEE_RAISE = 0.25f;          // added to Water.leveeHeight
    private static final float IRRIGATION_SOIL_TRIGGER = 0.55f;
    private static final float IRRIGATION_RIVER_FLOOR  = 0.15f;
    private static final float IRRIGATION_MAX_DRAW     = 0.05f;
    private static final float GRANARY_SCARCITY_EASE   = 0.008f; // per granary per day

    private static final int MAX_SECTORS = 16;
    private static final int T = WorkType.values().length;
    private static final boolean[][] built = new boolean[T][MAX_SECTORS];
    private static final int[][] daysLeft = new int[T][MAX_SECTORS];   // >0 while building

    // ------------------------------------------------------------- queries
    public static State stateOf(int sector, WorkType w) {
        if (bad(sector)) return State.NONE;
        if (built[w.ordinal()][sector]) return State.BUILT;
        if (daysLeft[w.ordinal()][sector] > 0) return State.BUILDING;
        return State.NONE;
    }

    public static float progressOf(int sector, WorkType w) {
        if (bad(sector) || daysLeft[w.ordinal()][sector] <= 0) return 0f;
        return 1f - (daysLeft[w.ordinal()][sector] / (float) w.days);
    }

    public static int daysLeftOf(int sector, WorkType w) {
        return bad(sector) ? 0 : daysLeft[w.ordinal()][sector];
    }

    public static boolean sectorBusy(int sector) {
        if (bad(sector)) return true;
        for (int t = 0; t < T; t++) if (daysLeft[t][sector] > 0) return true;
        return false;
    }

    public static boolean canStart(Kingdom k, int sector, WorkType w) {
        return !bad(sector)
                && stateOf(sector, w) == State.NONE
                && !sectorBusy(sector)
                && k.wood >= w.wood && k.stone >= w.stone && k.gold >= w.gold;
    }

    // ------------------------------------------------------------- commands
    // Pays the full cost up front and breaks ground. Call under the engine lock.
    public static boolean start(Kingdom k, int sector, WorkType w) {
        if (!canStart(k, sector, w)) return false;
        k.wood -= w.wood; k.stone -= w.stone; k.gold -= w.gold;
        daysLeft[w.ordinal()][sector] = w.days;
        Logger.logEvent("Ground is broken: a " + w.title.toLowerCase()
                + " rises in sector " + sector + ".", Logger.LogCategory.POLITICAL);
        return true;
    }

    // ------------------------------------------------------------- daily tick
    // Call once per day for the empire (kingdom 0), after world.advanceDay().
    public static void process(Kingdom k, World world) {
        if (!k.isActive) return;
        int n = Math.min(MAX_SECTORS, world.geography.count());

        // 1. Construction progresses; completion applies the one-time effect.
        for (WorkType w : WorkType.values()) {
            for (int s = 0; s < n; s++) {
                if (daysLeft[w.ordinal()][s] <= 0) continue;
                daysLeft[w.ordinal()][s]--;
                if (daysLeft[w.ordinal()][s] == 0) {
                    built[w.ordinal()][s] = true;
                    if (w == WorkType.LEVEE) world.water.leveeHeight[s] += LEVEE_RAISE;
                    Logger.logEvent("The " + w.title.toLowerCase() + " in sector " + s
                            + " stands complete.", Logger.LogCategory.POLITICAL);
                }
            }
        }

        // 2. Standing structures do their daily work, through the system arrays.
        float[] soil = world.agriculture.soilMoisture;
        float[] river = world.water.riverLevel;
        int granaries = 0;
        for (int s = 0; s < n; s++) {
            if (built[WorkType.IRRIGATION.ordinal()][s]
                    && soil[s] < IRRIGATION_SOIL_TRIGGER
                    && river[s] > IRRIGATION_RIVER_FLOOR) {
                float draw = Math.min(IRRIGATION_MAX_DRAW, river[s] * 0.15f);
                river[s] -= draw;
                soil[s] = Math.min(1f, soil[s] + draw * 0.8f);  // some water is lost to the ditch
            }
            if (built[WorkType.GRANARY.ordinal()][s]) granaries++;
        }
        if (granaries > 0) EventAftermath.easeScarcity(k.id, GRANARY_SCARCITY_EASE * granaries);
    }

    // ------------------------------------------------------------- save/load
    // One compact row: "type,sector,state;..." where state is B (built) or the
    // days remaining. Levee height itself is already persisted by SaveManager
    // (the WATL row), so only the flags and in-flight progress live here.
    public static String export() {
        StringBuilder b = new StringBuilder();
        for (int t = 0; t < T; t++) {
            for (int s = 0; s < MAX_SECTORS; s++) {
                if (built[t][s]) appendRow(b, t, s, "B");
                else if (daysLeft[t][s] > 0) appendRow(b, t, s, String.valueOf(daysLeft[t][s]));
            }
        }
        return b.toString();
    }
    private static void appendRow(StringBuilder b, int t, int s, String st) {
        if (b.length() > 0) b.append(';');
        b.append(t).append(',').append(s).append(',').append(st);
    }
    public static void importBlob(String blob) {
        resetAll();
        if (blob == null || blob.isEmpty()) return;
        for (String part : blob.split(";")) {
            String[] f = part.split(",");
            if (f.length != 3) continue;
            int t = Integer.parseInt(f[0]), s = Integer.parseInt(f[1]);
            if (t < 0 || t >= T || bad(s)) continue;
            if ("B".equals(f[2])) built[t][s] = true;
            else daysLeft[t][s] = Integer.parseInt(f[2]);
        }
    }
    public static void resetAll() {
        for (int t = 0; t < T; t++)
            for (int s = 0; s < MAX_SECTORS; s++) { built[t][s] = false; daysLeft[t][s] = 0; }
    }

    private static boolean bad(int sector) { return sector < 0 || sector >= MAX_SECTORS; }
}