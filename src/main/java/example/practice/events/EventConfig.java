package example.practice.events;

// Dials for the emergent event system. COOLDOWN_DAYS keeps any one event from
// spamming; FIRE_CHANCE makes a met condition surface organically rather than
// instantly.
public enum EventConfig {
    COOLDOWN_DAYS(3),                  // per event type, per kingdom
    FIRE_CHANCE(0.30f),                // chance per day once the condition holds

    DROUGHT_SOIL_THRESHOLD(0.30f),     // realm soil below this is "dry"
    DROUGHT_SOIL_HIT(0.15f),           // how much drier a drought makes it

    FLOOD_SEVERITY_THRESHOLD(0.40f),

    PLAGUE_DENSITY_THRESHOLD(0.70f),   // land density below this = overcrowded
    PLAGUE_DEATH_FRACTION(0.03f),

    RAID_ARMY_SHARE_THRESHOLD(0.08f),  // army this thin invites raiders
    RAID_RESOURCE_LOSS(0.20f),
    RAID_DEATH_FRACTION(0.01f),

    INTRIGUE_GRIEVANCE_THRESHOLD(0.70f),

    HARVEST_YIELD_THRESHOLD(1.10f),
    HARVEST_FOOD_PER_HEAD(10f),

    GOLD_STRIKE_CHANCE(0.02f),
    GOLD_STRIKE_AMOUNT(500f),

    SHOCK_AGGRAVATE(0.05f);            // how hard a shock nudges a faction

    public final float value;
    EventConfig(float v){ this.value = v; }
}