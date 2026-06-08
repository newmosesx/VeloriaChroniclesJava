package example.practice.world;

import example.practice.config.ClimateConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// The weather process. Reads the Calendar's seasonal baselines, layers travelling
// fronts on top, and produces each sector's actual weather for the day. Fronts
// move along the prevailing wind (westerlies, by default), so weather sweeps the
// map W->E - and which fronts appear depends on the season.
public class Climate implements WorldSystem {

    private final Geography geo;
    private final List<WeatherFront> fronts = new ArrayList<>();

    // Per-sector daily weather (indexed by kingdom/sector id).
    public final float[] temperature;
    public final float[] precipitation;
    public final float[] windStrength;
    public final String[] condition;

    public float windDirX = 1f, windDirY = 0f; // toward the east (from the west)
    public float windSpeed;
    private float directorSpawnBias = 0f;

    public Climate(Geography geo) {
        this.geo = geo;
        int n = geo.count();
        temperature = new float[n];
        precipitation = new float[n];
        windStrength = new float[n];
        condition = new String[n];
        for (int i = 0; i < n; i++) condition[i] = "clear";
    }

    @Override
    public void advanceDay(World world) {
        Calendar cal = world.calendar;
        updateWind(cal);
        maybeSpawnFront(cal);

        Iterator<WeatherFront> it = fronts.iterator();
        while (it.hasNext()) {
            WeatherFront f = it.next();
            f.advance();
            if (!f.alive) it.remove();
        }

        recomputeSectors(cal);
        directorSpawnBias *= ClimateConfig.DIRECTOR_BIAS_DECAY.value;
    }

    private void updateWind(Calendar cal) {
        float base = ClimateConfig.PREVAILING_WIND_BASE.value;
        float seasonal = (cal.season == Season.WINTER || cal.season == Season.AUTUMN)
                ? ClimateConfig.WIND_SEASONAL_BOOST.value : 0f;
        windSpeed = clamp01(base + seasonal + (float) (Math.random() * 0.2 - 0.1));

        // Westerlies: blow toward the east, with some daily jitter.
        double jitter = Math.toRadians((Math.random() * 2 - 1) * ClimateConfig.WIND_DIRECTION_JITTER.value);
        windDirX = (float) Math.cos(jitter);
        windDirY = (float) Math.sin(jitter);
    }

    private void maybeSpawnFront(Calendar cal) {
        if (fronts.size() >= (int) ClimateConfig.MAX_ACTIVE_FRONTS.value) return;
        float chance = ClimateConfig.FRONT_SPAWN_BASE_CHANCE.value + directorSpawnBias;
        if (Math.random() * 100 >= chance) return;

        FrontType type = pickType(cal.season);
        WeatherFront f = new WeatherFront();
        f.type = type;
        f.dirX = windDirX;
        f.dirY = windDirY;
        f.sigma = ClimateConfig.FRONT_SIGMA.value;

        int minL = (int) ClimateConfig.FRONT_MIN_LIFE_DAYS.value;
        int maxL = (int) ClimateConfig.FRONT_MAX_LIFE_DAYS.value;
        f.life = minL + (int) (Math.random() * (maxL - minL + 1));
        f.intensity = ClimateConfig.FRONT_BASE_INTENSITY.value * (0.7f + (float) Math.random() * 0.6f);

        // Enter from the upwind edge and fully cross over the front's lifetime.
        float minP = geo.minProjection(f.dirX, f.dirY);
        float maxP = geo.maxProjection(f.dirX, f.dirY);
        f.center = minP - 2 * f.sigma;
        f.speed = (maxP - minP + 4 * f.sigma) / f.life;

        fronts.add(f);
    }

    private FrontType pickType(Season s) {
        FrontType[] types = FrontType.values();
        float total = 0;
        for (FrontType t : types) total += t.seasonalWeight(s);
        float r = (float) (Math.random() * total);
        for (FrontType t : types) {
            r -= t.seasonalWeight(s);
            if (r <= 0) return t;
        }
        return types[0];
    }

    private void recomputeSectors(Calendar cal) {
        for (int i = 0; i < geo.count(); i++) {
            float tDelta = 0, pDelta = 0, wDelta = 0;
            WeatherFront dominant = null;
            float domMag = 0;

            for (WeatherFront f : fronts) {
                float e = f.effectAt(geo.projection(i, f.dirX, f.dirY));
                tDelta += e * f.type.tempCoef;
                pDelta += e * f.type.precipCoef;
                wDelta += e * f.type.windCoef;
                float mag = Math.abs(e * f.type.tempCoef)
                        + Math.abs(e * f.type.precipCoef) * 20
                        + Math.abs(e * f.type.windCoef) * 5;
                if (mag > domMag) { domMag = mag; dominant = f; }
            }

            float tn = (float) ((Math.random() * 2 - 1) * ClimateConfig.LOCAL_TEMP_NOISE.value);
            float pn = (float) ((Math.random() * 2 - 1) * ClimateConfig.LOCAL_PRECIP_NOISE.value);

            temperature[i] = cal.temperature + tDelta + tn;
            precipitation[i] = clamp01(cal.precipitationTendency + pDelta + pn);
            windStrength[i] = clamp01(windSpeed + wDelta);
            condition[i] = (dominant != null && domMag > 0.4f) ? dominant.type.label : "clear";
        }
    }

    // --- The Director's hooks (bounded) ---

    // Deepen a front that already exists. Returns false if there is none of that
    // type - the Director cannot conjure weather from nothing.
    public boolean intensifyExistingFront(FrontType type, float delta) {
        for (WeatherFront f : fronts) {
            if (f.alive && f.type == type) {
                f.intensity += Math.max(0f, Math.min(delta, ClimateConfig.DIRECTOR_INTENSITY_CAP.value));
                return true;
            }
        }
        return false;
    }

    // Make new fronts a little likelier for the next while, capped and decaying.
    public void nudgeSpawnBias(float amount) {
        directorSpawnBias = Math.min(ClimateConfig.DIRECTOR_SPAWN_BIAS_CAP.value,
                directorSpawnBias + Math.max(0f, amount));
    }

    public int activeFrontCount() { return fronts.size(); }

    @Override
    public String reportLine() {
        if (fronts.isEmpty()) {
            return String.format("Clear skies; wind %.0f%% from the west", windSpeed * 100);
        }
        StringBuilder b = new StringBuilder("Weather: ");
        for (WeatherFront f : fronts) {
            b.append(f.type.label).append(" over ").append(geo.nearestSector(f).region()).append("; ");
        }
        b.append(String.format("wind %.0f%%", windSpeed * 100));
        return b.toString();
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}