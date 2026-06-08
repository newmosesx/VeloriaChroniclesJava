package example.practice.config;

public enum WaterConfig {
    RAIN_TO_RIVER(0.5f),        // fraction of daily precipitation that reaches the rivers
    FLOW_DOWNSTREAM(0.35f),     // fraction of a river that moves to the next sector each day
    SEA_DRAINAGE(0.5f),         // fraction a coastal sector dumps to the sea each day
    RIVER_BASELINE_LOSS(0.1f),  // steady seepage/evaporation per day

    CHANNEL_CAPACITY(6f),       // river level a channel holds before it floods
    FLOOD_SCALE(4f),            // how far over capacity maps to full-severity flooding

    SNOW_TEMP(0f),              // below this, precipitation falls as snow (stored, not river)
    MELT_TEMP(4f),              // above this, snowpack melts into the rivers
    MELT_RATE(0.12f),           // share of snowpack that melts per degree above melt temp

    LEVEE_PER_UNIT(1.2f),       // flood threshold added per unit of levee height
    LEVEE_MAX(5f),              // tallest a levee can be built
    LEVEE_DECAY(0.015f),        // levee lost per day without maintenance
    LEVEE_BREACH_SEVERITY(0.65f),// flood this strong starts tearing the levee down
    LEVEE_BREACH_EROSION(0.5f), // levee height lost when a breach happens
    DIVERT_FRACTION(0.8f),      // share of a leveed overflow pushed onto the downstream sector
    FLOOD_RECEDE(0.34f),        // how fast flood severity falls once the river drops

    FLOOD_CROP_PENALTY(0.85f),  // max yield lost at full flood (read by Agriculture)

    SWELL_MIN_RIVER(2f),        // Director can only swell a river that already has real flow
    DIRECTOR_SWELL_CAP(3f);     // most the Director can add in one swell

    public final float value;
    WaterConfig(float value){ this.value = value; }
}