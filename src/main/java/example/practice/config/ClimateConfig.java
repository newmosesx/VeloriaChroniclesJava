package example.practice.config;

public enum ClimateConfig {
    FRONT_SPAWN_BASE_CHANCE(18),     // percent per day a new front forms
    MAX_ACTIVE_FRONTS(3),
    FRONT_MIN_LIFE_DAYS(4),
    FRONT_MAX_LIFE_DAYS(9),
    FRONT_SIGMA(0.6f),               // spatial width of a front along its travel axis
    FRONT_BASE_INTENSITY(1.0f),

    LOCAL_TEMP_NOISE(1.5f),          // +/- degrees of daily local wobble per sector
    LOCAL_PRECIP_NOISE(0.05f),

    PREVAILING_WIND_BASE(0.4f),      // 0..1 baseline wind strength
    WIND_SEASONAL_BOOST(0.3f),       // windier in autumn/winter
    WIND_DIRECTION_JITTER(50),       // degrees of variation around the westerlies

    // The Director's leash on the weather: it can deepen a front that exists and
    // make new ones a little likelier - but only within these caps.
    DIRECTOR_INTENSITY_CAP(0.6f),
    DIRECTOR_SPAWN_BIAS_CAP(25),
    DIRECTOR_BIAS_DECAY(0.8f);

    public final float value;
    ClimateConfig(float value){ this.value = value; }
}