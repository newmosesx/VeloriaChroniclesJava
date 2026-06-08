package example.practice.config;

// Tuning for the master clock and the natural baselines it produces. Everything
// downstream (climate, agriculture, water...) reads derived values that come
// from these - nothing here is a per-season "penalty", just the shape of the year.
public enum CalendarConfig {
    DAYS_PER_MONTH(30),
    MONTHS_PER_YEAR(12),
    DAYS_PER_SEASON(90),
    DAYS_PER_YEAR(360),

    // Temperature: a smooth annual cosine, peaking at mid-summer.
    TEMP_ANNUAL_MEAN(15),         // degrees C, yearly average
    TEMP_ANNUAL_AMPLITUDE(18),    // swing above/below mean -> ~ -3 to 33
    TEMP_PEAK_DAY(135),           // mid-summer (hottest day of year)

    // Daylight: same phase as temperature, gentler swing.
    DAYLIGHT_MEAN_HOURS(12),
    DAYLIGHT_AMPLITUDE_HOURS(4),  // ~8h midwinter to ~16h midsummer

    // Precipitation tendency (climatological normal, 0..1). Wettest midwinter,
    // driest midsummer -> summer naturally trends toward drought conditions.
    PRECIP_MEAN(0.5f),
    PRECIP_AMPLITUDE(0.3f),
    PRECIP_PEAK_DAY(315),         // mid-winter

    GROWING_SEASON_MIN_TEMP(8),   // below this, little grows

    // Year-to-year character. Rolled each new year so no two winters are alike,
    // and the band the Director is allowed to push within (it cannot exceed this).
    YEAR_TEMP_BIAS_RANGE(2),      // +/- degrees C
    YEAR_WET_BIAS_RANGE(0.15f);   // +/- precipitation tendency

    public final float value;
    CalendarConfig(float value){ this.value = value; }
}