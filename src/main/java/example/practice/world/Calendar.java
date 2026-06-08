package example.practice.world;

import example.practice.config.CalendarConfig;

// The master clock. Time advances; everything else is DERIVED from the day, not
// declared. Temperature, daylight, and precipitation are continuous functions of
// the day of year, so "temperature is rising" is a true statement you can read
// off the curve - never a flag someone set.
public class Calendar implements WorldSystem {

    // --- Raw state ---
    public int totalDays = 0;
    public int year = -1;          // forces a year-bias roll on the first recompute
    public int dayOfYear;
    public int month;              // 0..11
    public int dayOfMonth;         // 0..29

    // --- Derived each day ---
    public Season season;
    public float seasonProgress;   // 0..1 through the current season
    public float temperature;      // degrees C
    public float temperatureYesterday;
    public float daylightHours;
    public float precipitationTendency; // 0..1 climatological normal
    public boolean growingSeason;

    // --- This year's character (rolled per year; the Director's allowed band) ---
    public float yearTempBias;
    public float yearWetBias;

    public Calendar() {
        recompute();
    }

    @Override
    public void advanceDay(World world) {
        temperatureYesterday = temperature;
        totalDays++;
        recompute();
    }

    private void recompute() {
        int dpy = (int) CalendarConfig.DAYS_PER_YEAR.value;
        int newYear = totalDays / dpy;
        if (newYear != year) {
            year = newYear;
            rollYearCharacter();
        }
        dayOfYear = totalDays % dpy;

        int dpm = (int) CalendarConfig.DAYS_PER_MONTH.value;
        month = dayOfYear / dpm;
        dayOfMonth = dayOfYear % dpm;

        int dps = (int) CalendarConfig.DAYS_PER_SEASON.value;
        season = Season.values()[Math.min(3, dayOfYear / dps)];
        seasonProgress = (dayOfYear % dps) / (float) dps;

        temperature = computeTemperature(dayOfYear);
        daylightHours = computeDaylight(dayOfYear);
        precipitationTendency = clamp01(computePrecip(dayOfYear));
        growingSeason = temperature >= CalendarConfig.GROWING_SEASON_MIN_TEMP.value;
    }

    private float computeTemperature(int d) {
        return (float) (CalendarConfig.TEMP_ANNUAL_MEAN.value
                + CalendarConfig.TEMP_ANNUAL_AMPLITUDE.value * Math.cos(phase(d, CalendarConfig.TEMP_PEAK_DAY.value))
                + yearTempBias);
    }

    private float computeDaylight(int d) {
        return (float) (CalendarConfig.DAYLIGHT_MEAN_HOURS.value
                + CalendarConfig.DAYLIGHT_AMPLITUDE_HOURS.value * Math.cos(phase(d, CalendarConfig.TEMP_PEAK_DAY.value)));
    }

    private float computePrecip(int d) {
        return (float) (CalendarConfig.PRECIP_MEAN.value
                + CalendarConfig.PRECIP_AMPLITUDE.value * Math.cos(phase(d, CalendarConfig.PRECIP_PEAK_DAY.value))
                + yearWetBias);
    }

    private double phase(int d, float peakDay) {
        return 2 * Math.PI * (d - peakDay) / CalendarConfig.DAYS_PER_YEAR.value;
    }

    private void rollYearCharacter() {
        float tr = CalendarConfig.YEAR_TEMP_BIAS_RANGE.value;
        float wr = CalendarConfig.YEAR_WET_BIAS_RANGE.value;
        yearTempBias = (float) ((Math.random() * 2 - 1) * tr);
        yearWetBias = (float) ((Math.random() * 2 - 1) * wr);
    }

    // Positive = warming, negative = cooling. A derivation, not a stored flag.
    public float temperatureTrend() {
        return temperature - temperatureYesterday;
    }

    // The Director's ONLY lever on the calendar: shift this year's character, but
    // strictly within the band the world allows. It can deepen the coming season,
    // never invent one. Cost/plausibility is handled by the caller.
    public boolean applyDirectorBias(float tempDelta, float wetDelta) {
        float tr = CalendarConfig.YEAR_TEMP_BIAS_RANGE.value;
        float wr = CalendarConfig.YEAR_WET_BIAS_RANGE.value;
        yearTempBias = clamp(yearTempBias + tempDelta, -tr, tr);
        yearWetBias = clamp(yearWetBias + wetDelta, -wr, wr);
        recompute();
        return true;
    }

    public String yearCharacter() {
        String t = yearTempBias > 0.5f ? "warmer" : yearTempBias < -0.5f ? "cooler" : "average heat";
        String w = yearWetBias > 0.05f ? "wetter" : yearWetBias < -0.05f ? "drier" : "average rain";
        return String.format("%+.1f\u00B0C, %s, %s", yearTempBias, t, w);
    }

    @Override
    public String reportLine() {
        return String.format("Year %d, %s (%.0f%% in) - %.1f\u00B0C %s, %.1fh light, %s",
                year + 1, season.label, seasonProgress * 100,
                temperature, temperatureTrend() >= 0 ? "rising" : "falling",
                daylightHours, growingSeason ? "growing" : "dormant");
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}