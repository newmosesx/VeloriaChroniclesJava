package example.practice.gui;

import example.practice.agents.Agent;
import example.practice.agents.AgentRoster;
import example.practice.config.EdictType;
import example.practice.config.KingdomArchetype;
import example.practice.engine.*;
import example.practice.events.EventSystem;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.shared.ShareData;
import example.practice.story.Council;
import example.practice.story.CouncilMember;
import example.practice.user.Player;
import example.practice.world.Agriculture;
import example.practice.world.Calendar;
import example.practice.world.Climate;
import example.practice.world.Season;
import example.practice.world.Water;
import example.practice.world.World;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A complete snapshot of a running game, written to a plain-text file and
 * restored exactly. Save and Load both take the engine lock, so they are safe
 * to call while the simulation thread is running -- Load mutates state in place
 * and the next tick simply continues from the restored world.
 *
 * Covered: time, shared/story state, player, all kingdoms (incl. jobCounts and
 * archetype), the full population, the world systems (calendar/climate/water/
 * agriculture), in-progress + active tech, active edicts, faction grievances,
 * granary accumulators, story flags, event cooldowns, council dispositions,
 * the chosen council response, and every agent (level/renown/hp/disposition).
 *
 * USAGE:  SaveManager.save(engine, file);   SaveManager.load(engine, file);
 */
public final class SaveManager {

    private SaveManager() {}
    private static final String VERSION = "1";

    // ===================================================================== SAVE
    public static void save(SimulationEngine engine, File file) throws Exception {
        StringBuilder b = new StringBuilder();
        engine.lock();
        try {
            World w = engine.getWorld();
            b.append("VSAVE|").append(VERSION).append('\n');
            b.append(row("ENG", engine.getDay(), engine.getHour()));

            b.append(row("PWX", PublicWorksManager.export()));

            ShareData sh = engine.sharedData;
            b.append(row("SHD", s(sh.civilWarStatus), sh.currentStoryChapter, sh.currentStoryParagraph,
                    s(sh.worldPopulation), s(sh.currentHour)));

            Player p = engine.getPlayer();
            b.append(row("PLR", s(p.userName), bit(p.isAlive),
                    p.statSmart, p.statDamage, p.statDefense, p.statHealth, p.statMaxHealth,
                    p.statSpeed, p.statHunger, p.statExp, p.statLevel,
                    p.strength, p.intellect, p.charisma, p.age,
                    s(p.userHead), s(p.userTorso), s(p.userLegs), s(p.userFoots),
                    s(p.rightHand), s(p.leftHand)));

            for (Kingdom k : engine.getKingdoms()) {
                b.append(row("K", k.id, s(k.name), k.population, k.unrestLevel, bit(k.isActive), bit(k.limitersDisabled),
                        k.food, k.wood, k.stone, k.metal, k.weapons, k.gold, k.armyMorale,
                        k.storySkirmishOverride, k.storySkirmishChanceModifier, k.storyProductionModifier, k.storyFoodDailyCap,
                        k.divineTaxModifier, k.divineProductionModifier, k.divinePenaltyTimerDays, bit(k.canUseDivineIntervention),
                        k.archetype == null ? "\\0" : k.archetype.name()));
                b.append(row("KJOB", k.id, ints(k.jobCounts)));
            }

            for (Human h : engine.getWorldPopulation()) {
                b.append(row("H", s(h.name), h.kingdomId, bit(h.isAlive), bit(h.isGeneral), h.job, h.bronze,
                        h.smart, h.damage, h.defense, h.speed, h.health, h.hunger, h.level, h.experience,
                        h.quirks[0], h.quirks[1], h.quirks[2],
                        h.head, h.torso, h.legs, h.feet, h.rightHand, h.leftHand));
            }

            // --- world systems ---
            Calendar cal = w.calendar;
            b.append(row("CAL", cal.totalDays, cal.year, cal.dayOfYear, cal.month, cal.dayOfMonth,
                    cal.season == null ? "\\0" : cal.season.name(), cal.seasonProgress, cal.temperature,
                    cal.temperatureYesterday, cal.daylightHours, cal.precipitationTendency, bit(cal.growingSeason),
                    cal.yearTempBias, cal.yearWetBias));
            Climate cl = w.climate;
            b.append(row("CLI", cl.windSpeed));
            b.append(row("CLIT", floats(cl.temperature)));
            b.append(row("CLIP", floats(cl.precipitation)));
            b.append(row("CLIW", floats(cl.windStrength)));
            b.append(row("CLIC", strs(cl.condition)));
            Water wa = w.water;
            b.append(row("WATR", floats(wa.riverLevel)));
            b.append(row("WATS", floats(wa.snowpack)));
            b.append(row("WATF", floats(wa.floodSeverity)));
            b.append(row("WATL", floats(wa.leveeHeight)));
            b.append(row("WATC", strs(wa.condition)));
            Agriculture ag = w.agriculture;
            b.append(row("AGRS", floats(ag.soilMoisture)));
            b.append(row("AGRY", floats(ag.yield)));
            b.append(row("AGRC", strs(ag.cropState)));

            // --- managers (reflection for private statics, public API elsewhere) ---
            b.append("TECH|").append(exportTech()).append('\n');
            b.append("EDI|").append(exportEdicts()).append('\n');
            b.append("POL|").append(exportPolitics(engine)).append('\n');
            b.append("SUB|").append(exportSubsistence()).append('\n');
            b.append("STO|").append(exportStory()).append('\n');
            b.append("EVT|").append(exportEvents(engine)).append('\n');

            // --- council + chosen response ---
            b.append(row("DBT", s(DebateManager.chosenResponse)));
            Council council = DebateManager.getCouncil();
            if (council != null) for (CouncilMember m : council.members) {
                b.append(row("C", s(m.name), m.tension, m.suspicion, m.trust, m.likeness, bit(m.locked)));
            }

            // --- agents ---
            for (Agent a : engine.getRoster().agents) {
                b.append(row("A", s(a.name), a.level, a.renown, bit(a.alive),
                        a.tension, a.suspicion, a.trust, a.likeness, bit(a.lockedDisposition), a.hp));
            }
        } finally {
            engine.unlock();
        }
        Files.write(file.toPath(), b.toString().getBytes("UTF-8"));
    }

    // ===================================================================== LOAD
    public static void load(SimulationEngine engine, File file) throws Exception {
        List<String> lines = Files.readAllLines(file.toPath());
        List<String[]> humanRows = new ArrayList<>();

        engine.lock();
        try {
            World w = engine.getWorld();
            Kingdom[] ks = engine.getKingdoms();
            Council council = DebateManager.getCouncil();
            AgentRoster roster = engine.getRoster();

            for (String line : lines) {
                int i = line.indexOf('|');
                if (i < 0) continue;
                String tag = line.substring(0, i);
                String rest = line.substring(i + 1);
                String[] f = split(rest);

                switch (tag) {
                    case "ENG": engine.setDayHour(ints1(f, 0), ints1(f, 1)); break;
                    case "SHD": {
                        ShareData sh = engine.sharedData;
                        sh.civilWarStatus = u(f[0]);
                        sh.currentStoryChapter = ints1(f, 1);
                        sh.currentStoryParagraph = ints1(f, 2);
                        sh.worldPopulation = u(f[3]);
                        sh.currentHour = u(f[4]);
                        break;
                    }
                    case "PLR": {
                        Player p = engine.getPlayer();
                        int x = 0;
                        p.userName = u(f[x++]); p.isAlive = b(f[x++]);
                        p.statSmart = fl(f[x++]); p.statDamage = fl(f[x++]); p.statDefense = fl(f[x++]);
                        p.statHealth = fl(f[x++]); p.statMaxHealth = fl(f[x++]); p.statSpeed = fl(f[x++]);
                        p.statHunger = fl(f[x++]); p.statExp = fl(f[x++]); p.statLevel = in(f[x++]);
                        p.strength = in(f[x++]); p.intellect = in(f[x++]); p.charisma = in(f[x++]); p.age = in(f[x++]);
                        p.userHead = u(f[x++]); p.userTorso = u(f[x++]); p.userLegs = u(f[x++]); p.userFoots = u(f[x++]);
                        p.rightHand = u(f[x++]); p.leftHand = u(f[x++]);
                        break;
                    }
                    case "PWX": PublicWorksManager.importBlob(rest); break;
                    case "K": {
                        int id = in(f[0]);
                        if (id < 0 || id >= ks.length) break;
                        Kingdom k = ks[id]; int x = 1;
                        k.name = u(f[x++]); k.population = in(f[x++]); k.unrestLevel = in(f[x++]);
                        k.isActive = b(f[x++]); k.limitersDisabled = b(f[x++]);
                        k.food = in(f[x++]); k.wood = in(f[x++]); k.stone = in(f[x++]); k.metal = in(f[x++]);
                        k.weapons = in(f[x++]); k.gold = in(f[x++]); k.armyMorale = in(f[x++]);
                        k.storySkirmishOverride = in(f[x++]); k.storySkirmishChanceModifier = fl(f[x++]);
                        k.storyProductionModifier = fl(f[x++]); k.storyFoodDailyCap = in(f[x++]);
                        k.divineTaxModifier = fl(f[x++]); k.divineProductionModifier = fl(f[x++]);
                        k.divinePenaltyTimerDays = in(f[x++]); k.canUseDivineIntervention = b(f[x++]);
                        String arch = u(f[x++]);
                        k.archetype = (arch == null) ? null : KingdomArchetype.valueOf(arch);
                        break;
                    }
                    case "KJOB": {
                        int id = in(f[0]);
                        if (id >= 0 && id < ks.length) {
                            int[] jc = intArr(f[1]);
                            for (int j = 0; j < ks[id].jobCounts.length && j < jc.length; j++) ks[id].jobCounts[j] = jc[j];
                        }
                        break;
                    }
                    case "H": humanRows.add(f); break;

                    case "CAL": {
                        Calendar c = w.calendar; int x = 0;
                        c.totalDays = in(f[x++]); c.year = in(f[x++]); c.dayOfYear = in(f[x++]);
                        c.month = in(f[x++]); c.dayOfMonth = in(f[x++]);
                        String sn = u(f[x++]); c.season = (sn == null) ? c.season : Season.valueOf(sn);
                        c.seasonProgress = fl(f[x++]); c.temperature = fl(f[x++]); c.temperatureYesterday = fl(f[x++]);
                        c.daylightHours = fl(f[x++]); c.precipitationTendency = fl(f[x++]); c.growingSeason = b(f[x++]);
                        c.yearTempBias = fl(f[x++]); c.yearWetBias = fl(f[x++]);
                        break;
                    }
                    case "CLI":  w.climate.windSpeed = fl(f[0]); break;
                    case "CLIT": setFloats(w.climate.temperature, f); break;
                    case "CLIP": setFloats(w.climate.precipitation, f); break;
                    case "CLIW": setFloats(w.climate.windStrength, f); break;
                    case "CLIC": setStrs(w.climate.condition, f); break;
                    case "WATR": setFloats(w.water.riverLevel, f); break;
                    case "WATS": setFloats(w.water.snowpack, f); break;
                    case "WATF": setFloats(w.water.floodSeverity, f); break;
                    case "WATL": setFloats(w.water.leveeHeight, f); break;
                    case "WATC": setStrs(w.water.condition, f); break;
                    case "AGRS": setFloats(w.agriculture.soilMoisture, f); break;
                    case "AGRY": setFloats(w.agriculture.yield, f); break;
                    case "AGRC": setStrs(w.agriculture.cropState, f); break;

                    case "TECH": importTech(rest); break;
                    case "EDI":  importEdicts(rest); break;
                    case "POL":  importPolitics(engine, rest); break;
                    case "SUB":  importSubsistence(rest); break;
                    case "STO":  importStory(rest); break;
                    case "EVT":  importEvents(rest); break;

                    case "DBT": DebateManager.chosenResponse = u(f[0]); break;
                    case "C": {
                        if (council != null) {
                            String nm = u(f[0]);
                            for (CouncilMember m : council.members) if (m.name.equals(nm)) {
                                m.tension = in(f[1]); m.suspicion = in(f[2]); m.trust = in(f[3]);
                                m.likeness = in(f[4]); m.locked = b(f[5]);
                            }
                        }
                        break;
                    }
                    case "A": {
                        Agent a = roster.get(u(f[0]));
                        if (a != null) {
                            a.level = in(f[1]); a.deriveStats();
                            a.renown = fl(f[2]); a.alive = b(f[3]);
                            a.tension = in(f[4]); a.suspicion = in(f[5]); a.trust = in(f[6]); a.likeness = in(f[7]);
                            a.lockedDisposition = b(f[8]); a.hp = in(f[9]);
                        }
                        break;
                    }
                    default: break;
                }
            }

            // rebuild population in place
            List<Human> pop = engine.getWorldPopulation();
            pop.clear();
            for (String[] f : humanRows) {
                int x = 0;
                Human h = new Human(in(f[1]));         // kingdomId is field index 1
                h.name = u(f[x++]); /* x now 1 (kingdomId already consumed) */ x++;
                h.isAlive = b(f[x++]); h.isGeneral = b(f[x++]); h.job = in(f[x++]); h.bronze = in(f[x++]);
                h.smart = in(f[x++]); h.damage = in(f[x++]); h.defense = in(f[x++]); h.speed = in(f[x++]);
                h.health = in(f[x++]); h.hunger = in(f[x++]); h.level = in(f[x++]); h.experience = db(f[x++]);
                h.quirks[0] = in(f[x++]); h.quirks[1] = in(f[x++]); h.quirks[2] = in(f[x++]);
                h.head = in(f[x++]); h.torso = in(f[x++]); h.legs = in(f[x++]); h.feet = in(f[x++]);
                h.rightHand = in(f[x++]); h.leftHand = in(f[x++]);
                pop.add(h);
            }
        } finally {
            engine.unlock();
        }
    }

    // ===================================================== manager export/import
    private static Field field(Class<?> c, String n) throws Exception {
        Field f = c.getDeclaredField(n); f.setAccessible(true); return f;
    }

    private static String exportTech() throws Exception {
        Class<?> c = example.practice.engine.TechManager.class;
        Object state = field(c, "state").get(null);
        int[] days = (int[]) field(c, "daysLeft").get(null);
        float[] prog = (float[]) field(c, "progress").get(null);
        int n = Array.getLength(state);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) b.append(';');
            Object st = Array.get(state, i);
            int ord = (st == null) ? 0 : ((Enum<?>) st).ordinal();
            b.append(ord).append(',').append(days[i]).append(',').append(prog[i]);
        }
        return b.toString();
    }
    private static void importTech(String blob) throws Exception {
        if (blob == null || blob.isEmpty()) return;
        Class<?> c = example.practice.engine.TechManager.class;
        Object state = field(c, "state").get(null);
        int[] days = (int[]) field(c, "daysLeft").get(null);
        float[] prog = (float[]) field(c, "progress").get(null);
        Object[] consts = state.getClass().getComponentType().getEnumConstants();
        String[] parts = blob.split(";");
        for (int i = 0; i < parts.length && i < Array.getLength(state); i++) {
            String[] t = parts[i].split(",");
            Array.set(state, i, consts[Integer.parseInt(t[0])]);
            days[i] = Integer.parseInt(t[1]);
            prog[i] = Float.parseFloat(t[2]);
        }
    }

    private static String exportEdicts() throws Exception {
        Object active = field(example.practice.engine.EdictManager.class, "active").get(null);
        StringBuilder b = new StringBuilder();
        for (int kid = 0; kid < Array.getLength(active); kid++) {
            Object set = Array.get(active, kid);
            if (set == null) continue;
            @SuppressWarnings("unchecked") EnumSet<EdictType> es = (EnumSet<EdictType>) set;
            if (es.isEmpty()) continue;
            if (b.length() > 0) b.append(';');
            b.append(kid).append('=');
            boolean first = true;
            for (EdictType e : es) { if (!first) b.append('~'); b.append(e.name()); first = false; }
        }
        return b.toString();
    }
    @SuppressWarnings("unchecked")
    private static void importEdicts(String blob) throws Exception {
        Object active = field(example.practice.engine.EdictManager.class, "active").get(null);
        for (int kid = 0; kid < Array.getLength(active); kid++) Array.set(active, kid, EnumSet.noneOf(EdictType.class));
        if (blob == null || blob.isEmpty()) return;
        for (String part : blob.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            int kid = Integer.parseInt(part.substring(0, eq));
            EnumSet<EdictType> es = EnumSet.noneOf(EdictType.class);
            String names = part.substring(eq + 1);
            if (!names.isEmpty()) for (String nm : names.split("~")) es.add(EdictType.valueOf(nm));
            if (kid >= 0 && kid < Array.getLength(active)) Array.set(active, kid, es);
        }
    }

    private static String exportPolitics(SimulationEngine engine) {
        StringBuilder b = new StringBuilder();
        Kingdom[] ks = engine.getKingdoms();
        for (Kingdom k : ks) {
            Faction[] fs = PoliticsManager.factionsOf(k);
            if (fs == null) continue;
            if (b.length() > 0) b.append(';');
            b.append(k.id).append('=');
            for (int j = 0; j < fs.length; j++) {
                if (j > 0) b.append('/');
                b.append(fs[j].grievance).append('~').append(fs[j].target).append('~').append(fs[j].power);
            }
        }
        return b.toString();
    }
    private static void importPolitics(SimulationEngine engine, String blob) {
        if (blob == null || blob.isEmpty()) return;
        Kingdom[] ks = engine.getKingdoms();
        for (String part : blob.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            int kid = Integer.parseInt(part.substring(0, eq));
            if (kid < 0 || kid >= ks.length) continue;
            Faction[] fs = PoliticsManager.factionsOf(ks[kid]);   // ensures init
            String[] facs = part.substring(eq + 1).split("/");
            for (int j = 0; j < facs.length && j < fs.length; j++) {
                String[] v = facs[j].split("~");
                fs[j].grievance = Float.parseFloat(v[0]);
                fs[j].target = Float.parseFloat(v[1]);
                fs[j].power = Float.parseFloat(v[2]);
            }
        }
    }

    private static String exportSubsistence() throws Exception {
        Class<?> c = example.practice.engine.SubsistenceManager.class;
        float[] birth = (float[]) field(c, "birthAcc").get(null);
        float[] death = (float[]) field(c, "deathAcc").get(null);
        return floats(birth) + "/" + floats(death);
    }
    private static void importSubsistence(String blob) throws Exception {
        if (blob == null || blob.isEmpty()) return;
        Class<?> c = example.practice.engine.SubsistenceManager.class;
        float[] birth = (float[]) field(c, "birthAcc").get(null);
        float[] death = (float[]) field(c, "deathAcc").get(null);
        String[] halves = blob.split("/", 2);
        setFloats(birth, halves[0].split(","));
        if (halves.length > 1) setFloats(death, halves[1].split(","));
    }

    private static final String[] STORY_FLAGS = { "ch1_p3", "ch1_p7", "ch1_p9", "ch8_p0", "ch8_p9", "ch8_p19" };
    private static String exportStory() throws Exception {
        Class<?> c = example.practice.engine.StoryManager.class;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < STORY_FLAGS.length; i++) {
            if (i > 0) b.append('~');
            b.append(field(c, STORY_FLAGS[i]).getBoolean(null) ? 1 : 0);
        }
        return b.toString();
    }
    private static void importStory(String blob) throws Exception {
        if (blob == null || blob.isEmpty()) return;
        Class<?> c = example.practice.engine.StoryManager.class;
        String[] v = blob.split("~");
        for (int i = 0; i < STORY_FLAGS.length && i < v.length; i++) {
            field(c, STORY_FLAGS[i]).setBoolean(null, v[i].equals("1"));
        }
    }

    private static String exportEvents(SimulationEngine engine) throws Exception {
        int[][] lf = (int[][]) field(EventSystem.class, "lastFired").get(null);
        int n = engine.getKingdoms().length;
        StringBuilder b = new StringBuilder();
        for (int kid = 0; kid < n && kid < lf.length; kid++) {
            if (kid > 0) b.append(';');
            b.append(kid).append('=');
            for (int j = 0; j < lf[kid].length; j++) { if (j > 0) b.append('~'); b.append(lf[kid][j]); }
        }
        return b.toString();
    }
    private static void importEvents(String blob) throws Exception {
        if (blob == null || blob.isEmpty()) return;
        int[][] lf = (int[][]) field(EventSystem.class, "lastFired").get(null);
        for (String part : blob.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            int kid = Integer.parseInt(part.substring(0, eq));
            if (kid < 0 || kid >= lf.length) continue;
            String[] v = part.substring(eq + 1).split("~");
            for (int j = 0; j < v.length && j < lf[kid].length; j++) lf[kid][j] = Integer.parseInt(v[j]);
        }
    }

    // ============================================================ tiny helpers
    private static String row(String tag, Object... fields) {
        StringBuilder b = new StringBuilder(tag);
        for (Object o : fields) b.append('|').append(o);
        return b.append('\n').toString();
    }
    private static String[] split(String rest) {
        String[] raw = rest.split("\\|", -1);
        return raw;
    }
    private static String bit(boolean v) { return v ? "1" : "0"; }
    private static String ints(int[] a) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); b.append(a[i]); }
        return b.toString();
    }
    private static String floats(float[] a) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); b.append(a[i]); }
        return b.toString();
    }
    private static String strs(String[] a) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i > 0) b.append(','); b.append(s(a[i])); }
        return b.toString();
    }
    private static int[] intArr(String csv) {
        String[] p = csv.split(",", -1);
        int[] a = new int[p.length];
        for (int i = 0; i < p.length; i++) a[i] = p[i].isEmpty() ? 0 : Integer.parseInt(p[i]);
        return a;
    }
    private static void setFloats(float[] target, String[] csvFields) {
        // csvFields is a single field containing the csv (index 0) OR already-split tokens
        String[] tok = (csvFields.length == 1) ? csvFields[0].split(",", -1) : csvFields;
        for (int i = 0; i < target.length && i < tok.length; i++)
            target[i] = tok[i].isEmpty() ? 0f : Float.parseFloat(tok[i]);
    }
    private static void setFloats(float[] target, String csv) { setFloats(target, csv.split(",", -1)); }
    private static void setStrs(String[] target, String[] csvFields) {
        String[] tok = (csvFields.length == 1) ? csvFields[0].split(",", -1) : csvFields;
        for (int i = 0; i < target.length && i < tok.length; i++) target[i] = u(tok[i]);
    }

    // primitives from string fields
    private static int in(String s) { return s.isEmpty() ? 0 : Integer.parseInt(s.trim()); }
    private static int ints1(String[] f, int i) { return in(f[i]); }
    private static float fl(String s) { return s.isEmpty() ? 0f : Float.parseFloat(s.trim()); }
    private static double db(String s) { return s.isEmpty() ? 0d : Double.parseDouble(s.trim()); }
    private static boolean b(String s) { return s.equals("1"); }

    // string escape (| , \ newline) with a null sentinel
    private static String s(String v) {
        if (v == null) return "\\0";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '|':  b.append("\\P"); break;
                case ',':  b.append("\\C"); break;
                case '\n': b.append("\\n"); break;
                case '\r': break;
                default:   b.append(c);
            }
        }
        return b.toString();
    }
    private static String u(String v) {
        if (v == null || v.equals("\\0")) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '\\' && i + 1 < v.length()) {
                char n = v.charAt(++i);
                switch (n) {
                    case '\\': b.append('\\'); break;
                    case 'P':  b.append('|'); break;
                    case 'C':  b.append(','); break;
                    case 'n':  b.append('\n'); break;
                    default:   b.append(n);
                }
            } else b.append(c);
        }
        return b.toString();
    }
}