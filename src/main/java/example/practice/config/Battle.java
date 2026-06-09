package example.practice.config;

public enum Battle {
    HOURLY_SKIRMISH_BASE_CHANCE_PERCENT(50),
    MORALE_LOSS_FROM_SKIRMISH(1),
    MORALE_GAIN_ON_VICTORY(5),
    MORALE_LOSS_ON_DEFEAT(10),
    REBELS_IN_SKIRMISH(0.15f),
    SOLDIERS_IN_SKIRMISH(0.25f),

    // --- Real battles (the "Attack" command) ---
    INITIAL_COMMIT_FRACTION(0.35f), // share of each army that opens the battle
    REINFORCE_MIN_PCT(0.05f),       // a reinforcement wave is 5-10% of the army NOT engaged
    REINFORCE_MAX_PCT(0.10f),
    DECISION_WINDOWS(5),            // pauses where each side commits a decision
    ASSESS_DRIFT(0.20f),            // a commander's read of the enemy can be off by +/- this
    ROUND_LETHALITY(0.16f),         // how hard a single clash bites
    LEADER_FALLS_ON_LOSS(1);        // 1 = the losing side's leader dies

    public final float value;

    Battle(float value){
        this.value = value;
    }
}