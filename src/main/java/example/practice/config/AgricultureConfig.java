package example.practice.config;

public enum AgricultureConfig {
    SOIL_RECHARGE_RATE(0.12f),       // fraction of a day's precipitation that soaks into soil
    SOIL_DRAINAGE_RATE(0.05f),       // soil lost to runoff/percolation each day, proportional to moisture
    EVAPORATION_BASE(0.02f),         // baseline daily soil loss
    EVAPORATION_PER_DEGREE(0.004f),  // extra loss per degree above the evaporation base
    EVAPORATION_TEMP_BASE(18),       // soil dries faster above this temperature

    IDEAL_CROP_TEMP(20),             // peak-growth temperature
    TEMP_TOLERANCE(12),              // width of the comfortable band (gaussian spread)

    MOISTURE_FOR_FULL_YIELD(0.45f),  // soil moisture at which yield tops out
    WATERLOGGING_THRESHOLD(0.9f),    // above this, too wet -> yield penalty
    DORMANT_YIELD(0.15f),            // yield outside the growing season
    INITIAL_SOIL_MOISTURE(0.5f),
    MAX_YIELD_FACTOR(1.4f),          // best-case bonus over a normal harvest

    ARABLE_LAND_CAPACITY(900),       // people one sector's land feeds before yield starts falling
    // (the carrying-capacity dial; empire farms all 8 sectors)

    DIRECTOR_BLIGHT_CAP(0.4f),       // max yield reduction the Director can inflict via blight
    BLIGHT_DECAY(0.85f);

    public final float value;
    AgricultureConfig(float value){ this.value = value; }
}