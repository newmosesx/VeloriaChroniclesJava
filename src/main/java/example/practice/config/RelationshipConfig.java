package example.practice.config;

// All the magic numbers for the relationship system live here, matching the
// pattern used by Morale, Rebellion, EventSpecific, etc. Tune the whole feel
// of the council from this one file.
public enum RelationshipConfig {
    // --- The "memory lock" ---
    LOCK_SUSPICION_THRESHOLD(60),   // suspicion at/above this engages the lock
    LOCK_TENSION_THRESHOLD(65),     // tension at/above this engages the lock
    UNLOCK_SUSPICION_CEILING(45),   // both must fall below their ceilings to release
    UNLOCK_TENSION_CEILING(45),
    LOCKED_GOODWILL_DAMP(0.2f),     // while locked, positive trust/likeness gains scale to 20%
    LOCKED_DECAY_DAMP(0.3f),        // while locked, suspicion/tension reductions scale to 30%

    // --- Hard disposition overrides (the part a trained model could never guarantee) ---
    HARD_HOSTILE_SUSPICION(70),     // past this, they're Hostile no matter how much they "like" you
    HARD_HOSTILE_TENSION(75),

    // --- Disposition score cutoffs ---
    DISP_LOYAL(35),
    DISP_WARM(15),
    DISP_NEUTRAL(-10),
    DISP_WARY(-35),

    STAT_MIN(0),
    STAT_MAX(100);

    public final float value;
    RelationshipConfig(float value){ this.value = value; }
}