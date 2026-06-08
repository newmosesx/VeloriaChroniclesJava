package example.practice.config;

public enum MobilizationConfig {
    SOLDIER_CEILING(0.25f),       // most of the population under regular arms in a secure realm
    REBEL_CAP(0.45f),             // most that can be in open revolt at once - never the whole realm
    SECURE_GRANARY_DAYS(45f),     // granary depth (days of food) at which the army is fully funded
    MIN_FOOD_SECURITY(0.35f),     // even starving, a remnant force can be kept
    REBEL_STANDDOWN_UNREST(600f), // below this unrest, rebels start drifting back to civilian life
    REBEL_STANDDOWN_RATE(0.04f);  // share of rebels reintegrating per calm day

    public final float value;
    MobilizationConfig(float value){ this.value = value; }
}