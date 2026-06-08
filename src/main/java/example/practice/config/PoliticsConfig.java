package example.practice.config;

public enum PoliticsConfig {
    SECURE_GRANARY_DAYS(45f),   // granary depth at which the Commons feel food-secure
    GRIEVANCE_EASE(0.08f),      // how fast anger builds and cools (inertia = memory)
    UNREST_FULL_SCALE(2500f);   // political pressure of 1.0 maps to this much unrest (collapse at 2000)

    public final float value;
    PoliticsConfig(float value){ this.value = value; }
}