package example.practice.agents;

// A tracked character: identity, where they stand with the throne (the same four-
// vector your relationship card shows), combat stats derived from level + twist,
// and renown that climbs over time. Independent agents climb fastest and grow into
// daggers or shields. Adding a character is a single Agent.of(...) line.
public class Agent {
    public String name;
    public String epithet;
    public AgentAllegiance allegiance;
    public AgentTwist twist;

    public int level;
    public float renown;          // grows daily; Jokers grow fast

    public int maxHp, hp;
    public int attack, defense, speed, cunning;

    // Disposition toward the throne (0..100), same model as the council card.
    public int tension = 20, suspicion = 20, trust = 40, likeness = 40;
    public boolean lockedDisposition = false; // the Speaker never moves, whatever you say

    public boolean alive = true;
    public boolean refusesAtrocities = false; // Jokers won't end the world
    boolean usedSurvivor = false;             // SURVIVOR twist, once per fight

    public Agent(String name, String epithet, AgentAllegiance allegiance, AgentTwist twist, int level) {
        this.name = name; this.epithet = epithet; this.allegiance = allegiance;
        this.twist = twist; this.level = Math.max(1, level);
        this.refusesAtrocities = (allegiance == AgentAllegiance.INDEPENDENT);
        deriveStats();
        this.hp = this.maxHp;
    }

    public static Agent of(String name, String epithet, AgentAllegiance a, AgentTwist twist, int level) {
        return new Agent(name, epithet, a, twist, level);
    }

    public Agent disposition(int tension, int suspicion, int trust, int likeness) {
        this.tension = tension; this.suspicion = suspicion; this.trust = trust; this.likeness = likeness;
        return this;
    }
    public Agent locked() { this.lockedDisposition = true; return this; } // for the Speaker

    public void deriveStats() {
        float hpM = 1, atkM = 1, defM = 1, spdM = 1, cunM = 1;
        switch (twist) {
            case DUELIST:       spdM = 1.30f; atkM = 1.15f; break;
            case SHADOW:        cunM = 1.60f; spdM = 1.20f; hpM = 0.90f; break;
            case JUGGERNAUT:    hpM = 1.60f; defM = 1.50f; atkM = 0.90f; break;
            case GLASS_CANNON:  atkM = 1.55f; defM = 0.50f; break;
            case SILVER_TONGUE: cunM = 1.40f; atkM = 0.70f; break;
            case WARLORD:       atkM = 1.25f; hpM = 1.20f; break;
            case SURVIVOR:      hpM = 1.30f; break;
            case WILDCARD:      break; // chaos is applied in combat, not the sheet
            case IMMOVABLE:     defM = 2.0f; atkM = 0.40f; break;
        }
        maxHp   = Math.round((30 + level * 10)    * hpM);
        attack  = Math.round((8  + level * 2)     * atkM);
        defense = Math.round((4  + level * 1.5f)  * defM);
        speed   = Math.round((10 + level)         * spdM);
        cunning = Math.round((6  + level * 2)     * cunM);
    }

    public void gainRenown(float amount) {
        renown += amount;
        while (renown >= level * 100f) { renown -= level * 100f; levelUp(); }
    }
    public void levelUp() { level++; deriveStats(); hp = maxHp; }

    // Where they stand with the throne: a shield, a dagger, or neither.
    public Stance stance() {
        if (trust >= 60 && tension < 40) return Stance.SHIELD;
        if (tension >= 60 || suspicion >= 60) return Stance.DAGGER;
        return Stance.NEUTRAL;
    }

    // How much weight their stance throws around - the Jokers' grows fast with renown.
    public float power() {
        float base = level * 10 + renown * 0.1f;
        return allegiance == AgentAllegiance.INDEPENDENT ? base * 1.25f : base;
    }

    public enum Stance { SHIELD, NEUTRAL, DAGGER }
}