package example.practice.report;

import example.practice.config.Rebellion;
import example.practice.engine.DailyEventTracker;
import example.practice.engine.DebateManager;
import example.practice.engine.SimulationEngine;
import example.practice.humans.Human;
import example.practice.kingdoms.Kingdom;
import example.practice.story.CharacterData;
import example.practice.story.Council;
import example.practice.story.CouncilMember;
import example.practice.user.Player;
import example.practice.world.Calendar;

import java.util.ArrayList;
import java.util.List;

// The one snapshot. Build it with from(engine) while holding the sim lock (see
// ReportService). It reads only - it never mutates the world - so it's safe to
// hand to the GUI or the World Director.
public class WorldReport {
    public String time;
    public String civilWarStatus;

    public int worldPopulation, worldSoldiers, worldRebels, worldCivilians;
    public int activeKingdoms;
    public float worldStabilityIndex;     // 0..1, average across active kingdoms

    public StoryReport story;
    public HeroReport emperor;
    public List<HeroReport> figures = new ArrayList<>();
    public List<KingdomReport> kingdoms = new ArrayList<>();
    public EventReport events;

    public EnvironmentReport environment;

    public static WorldReport from(SimulationEngine engine) {
        WorldReport r = new WorldReport();
        List<Human> pop = engine.getWorldPopulation();
        Kingdom[] kingdoms = engine.getKingdoms();
        Player emperor = engine.getPlayer();
        float threshold = Rebellion.REBELLIONTHRESHOLD.value;

        // Environment slice, read off the Calendar.
        Calendar cal = engine.getWorld().calendar;
        EnvironmentReport env = new EnvironmentReport();
        env.year = cal.year + 1;
        env.dayOfYear = cal.dayOfYear;
        env.season = cal.season.label;
        env.seasonProgressPercent = Math.round(cal.seasonProgress * 100);
        env.temperature = cal.temperature;
        env.warming = cal.temperatureTrend() >= 0;
        env.daylightHours = cal.daylightHours;
        env.precipitationTendency = cal.precipitationTendency;
        env.growingSeason = cal.growingSeason;
        env.yearCharacter = cal.yearCharacter();
        r.environment = env;

        r.time = engine.getFormattedTime();
        r.civilWarStatus = engine.sharedData.civilWarStatus;

        int n = kingdoms.length;
        int[][] job = new int[n][10];
        int[] generals = new int[n];
        int[] rebelLeaders = new int[n];
        int[] alive = new int[n];

        // Single pass over the population for every per-kingdom and world tally.
        for (Human h : pop) {
            if (!h.isAlive) continue;
            r.worldPopulation++;
            if (h.job >= 6 && h.job <= 8) r.worldSoldiers++;
            else if (h.job == 9) r.worldRebels++;
            else if (h.job >= 1 && h.job <= 5) r.worldCivilians++;

            int kid = h.kingdomId;
            if (kid >= 0 && kid < n) {
                alive[kid]++;
                if (h.job >= 0 && h.job < 10) job[kid][h.job]++;
                if (h.isGeneral) {
                    if (h.job >= 6 && h.job <= 8) generals[kid]++;
                    else if (h.job == 9) rebelLeaders[kid]++;
                }
            }
        }

        int totalGenerals = 0;
        float stabilitySum = 0;
        int activeCount = 0;

        for (int i = 0; i < n; i++) {
            Kingdom k = kingdoms[i];
            KingdomReport kr = new KingdomReport();
            kr.id = k.id;
            kr.name = k.name;
            kr.active = k.isActive;
            kr.archetypeName = (k.archetype != null) ? k.archetype.name() : (k.id == 0 ? "EMPIRE" : "-");
            kr.population = alive[i];
            kr.unrest = k.unrestLevel;
            kr.stabilityPercent = Math.max(0, Math.round((1 - k.unrestLevel / threshold) * 100));
            kr.morale = k.armyMorale;
            kr.food = k.food; kr.wood = k.wood; kr.stone = k.stone; kr.metal = k.metal; kr.treasury = k.gold;
            kr.foodDaysLeft = (float) k.food / (alive[i] + 1);
            kr.limitersDisabled = k.limitersDisabled;
            kr.divineDaysLeft = k.divinePenaltyTimerDays;

            Census c = new Census();
            c.unemployed = job[i][0];
            c.farmers = job[i][1]; c.butchers = job[i][2]; c.lumberjacks = job[i][3];
            c.miners = job[i][4]; c.blacksmiths = job[i][5];
            c.swordsmen = job[i][6]; c.archers = job[i][7]; c.cavalry = job[i][8];
            c.rebels = job[i][9];
            kr.census = c;

            kr.soldiers = c.swordsmen + c.archers + c.cavalry;
            kr.rebels = c.rebels;
            kr.civilians = c.farmers + c.butchers + c.lumberjacks + c.miners + c.blacksmiths;
            kr.unemployed = c.unemployed;
            kr.generals = generals[i];
            kr.rebelLeaders = rebelLeaders[i];
            totalGenerals += generals[i];

            // Weakest pillar - what the Director would target (or shore up).
            float foodRisk = kr.foodDaysLeft < 3 ? (3 - kr.foodDaysLeft) / 3f : 0f;
            float milRisk = Math.min(1f, kr.rebels / (float) (kr.soldiers + 1));
            float unrestRisk = k.unrestLevel / threshold;
            kr.threatLevel = Math.max(foodRisk, Math.max(milRisk, unrestRisk));
            if (kr.threatLevel < 0.15f) kr.weakestPillar = "None";
            else if (kr.threatLevel == unrestRisk) kr.weakestPillar = "Unrest";
            else if (kr.threatLevel == milRisk) kr.weakestPillar = "Military";
            else kr.weakestPillar = "Food";

            r.kingdoms.add(kr);
            if (k.isActive) { activeCount++; stabilitySum += kr.stabilityPercent; }
        }
        r.activeKingdoms = activeCount;
        r.worldStabilityIndex = activeCount > 0 ? stabilitySum / activeCount / 100f : 0f;

        // Story
        StoryReport s = new StoryReport();
        s.chapter = engine.sharedData.currentStoryChapter;
        s.paragraph = engine.sharedData.currentStoryParagraph;
        s.choicePending = DebateManager.hasChoices(s.chapter, s.paragraph);
        s.lastChoice = DebateManager.chosenResponse;
        s.label = "Chapter " + (s.chapter + 1) + ", paragraph " + s.paragraph;
        r.story = s;

        // Emperor
        HeroReport emp = new HeroReport();
        emp.category = "EMPEROR";
        emp.name = emperor.userName;
        emp.role = "Emperor";
        emp.alive = emperor.isAlive;
        emp.health = (int) emperor.statHealth;
        emp.maxHealth = (int) emperor.statMaxHealth;
        emp.detail = "STR " + emperor.strength + "  INT " + emperor.intellect
                + "  CHA " + emperor.charisma + "  Age " + emperor.age;
        r.emperor = emp;

        // Council members (live relationship state)
        Council council = DebateManager.getCouncil();
        if (council != null) {
            for (CouncilMember m : council.members) {
                HeroReport h = new HeroReport();
                h.category = "COUNCIL";
                h.name = m.name;
                h.role = m.title;
                h.alive = true;
                h.detail = m.disposition() + (m.locked ? " (locked)" : "")
                        + "  [T" + m.tension + " S" + m.suspicion + " Tr" + m.trust + " L" + m.likeness + "]";
                r.figures.add(h);
            }
        }

        // Generals summary
        HeroReport gen = new HeroReport();
        gen.category = "GENERAL";
        gen.name = "Field generals";
        gen.role = "Command";
        gen.alive = true;
        gen.detail = totalGenerals + " active across all kingdoms";
        r.figures.add(gen);

        // Jokers - static roster, no live health yet (health stays null on purpose)
        for (CharacterData cd : CharacterData.CHARACTERS) {
            HeroReport h = new HeroReport();
            h.category = "JOKER";
            h.name = cd.name;
            h.role = cd.title;
            h.alive = true;
            h.detail = cd.description;
            r.figures.add(h);
        }

        // Events today
        EventReport e = new EventReport();
        if (DailyEventTracker.harvestTriggered) e.today.add("Bountiful harvest");
        if (DailyEventTracker.goldTriggered) e.today.add("Gold discovered");
        if (DailyEventTracker.plagueTriggered) e.today.add("Plague");
        if (DailyEventTracker.droughtTriggered) e.today.add("Drought");
        if (DailyEventTracker.barbarianTriggered) e.today.add("Barbarian raid");
        if (DailyEventTracker.intrigueTriggered) e.today.add("Political intrigue");
        if (DailyEventTracker.festivalTriggered) e.today.add("Festival held");
        if (DailyEventTracker.famineLogged) e.today.add("Famine");
        r.events = e;

        return r;
    }

    // Human-readable dump - handy for a console print, a log line, or an intel panel.
    public String toText() {
        StringBuilder b = new StringBuilder();
        b.append("=== VELORIA INTELLIGENCE REPORT ===\n");
        b.append(time).append("   |   ").append(civilWarStatus == null ? "" : civilWarStatus).append("\n");
        b.append("World pop ").append(worldPopulation)
                .append("  | soldiers ").append(worldSoldiers)
                .append("  | rebels ").append(worldRebels)
                .append("  | civilians ").append(worldCivilians).append("\n");
        b.append("Active kingdoms ").append(activeKingdoms)
                .append("  | world stability ").append(Math.round(worldStabilityIndex * 100)).append("%\n");

        if (environment != null) {
            b.append(String.format("Environment: %s, year %d - %.1f\u00B0C %s, %.0f%% rain, %s (%s)%n",
                    environment.season, environment.year, environment.temperature,
                    environment.warming ? "rising" : "falling",
                    environment.precipitationTendency * 100,
                    environment.growingSeason ? "growing" : "dormant",
                    environment.yearCharacter));
        }

        b.append("\n-- STORY --\n").append(story.label);
        if (story.choicePending) b.append("  [choice pending]");
        if (story.lastChoice != null) b.append("\n   last: \"").append(story.lastChoice).append("\"");
        b.append("\n");

        b.append("\n-- EMPEROR --\n");
        b.append(emperor.name).append("  HP ").append(emperor.health).append("/").append(emperor.maxHealth)
                .append("  ").append(emperor.detail).append("\n");

        b.append("\n-- FIGURES --\n");
        for (HeroReport h : figures) {
            b.append("  ").append(h.name).append(" (").append(h.role).append(") ");
            if (h.health != null) b.append("HP ").append(h.health).append("/").append(h.maxHealth).append(" ");
            if (h.detail != null) b.append("- ").append(h.detail);
            b.append("\n");
        }

        b.append("\n-- KINGDOMS --\n");
        for (KingdomReport k : kingdoms) {
            if (!k.active) continue;
            b.append(String.format("  [%d] %s%n", k.id, k.name));
            b.append(String.format("      pop %d | stability %d%% | morale %d | food-days %.1f%n",
                    k.population, k.stabilityPercent, k.morale, k.foodDaysLeft));
            b.append(String.format("      food %d  wood %d  stone %d  metal %d  treasury %d%n",
                    k.food, k.wood, k.stone, k.metal, k.treasury));
            b.append(String.format("      soldiers %d (gen %d) | rebels %d (led %d) | civ %d | idle %d%n",
                    k.soldiers, k.generals, k.rebels, k.rebelLeaders, k.civilians, k.unemployed));
            b.append(String.format("      weakest pillar: %s (threat %.0f%%)%n", k.weakestPillar, k.threatLevel * 100));
            if (k.limitersDisabled) b.append("      !! limiters disabled !!\n");
            if (k.divineDaysLeft > 0) b.append("      divine penalty: ").append(k.divineDaysLeft).append(" days left\n");
        }

        if (!events.today.isEmpty()) {
            b.append("\n-- EVENTS TODAY --\n   ").append(String.join(", ", events.today)).append("\n");
        }
        return b.toString();
    }
}