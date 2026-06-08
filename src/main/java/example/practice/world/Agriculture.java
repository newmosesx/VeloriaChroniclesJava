package example.practice.world;

import example.practice.config.AgricultureConfig;
import example.practice.config.WaterConfig;
import example.practice.config.TechEffect;
import example.practice.engine.TechManager;

// Turns weather into harvest. Yield is derived from soil moisture, temperature,
// and the growing season; land density caps a crowded sector's output, giving the
// realm a carrying capacity. Irrigation tech expands that arable ceiling.
public class Agriculture implements WorldSystem {

    private final Geography geo;
    public final float[] soilMoisture;
    public final float[] yield;
    public final String[] cropState;
    private final float[] blight;

    public Agriculture(Geography geo) {
        this.geo = geo;
        int n = geo.count();
        soilMoisture = new float[n];
        yield = new float[n];
        cropState = new String[n];
        blight = new float[n];
        float init = AgricultureConfig.INITIAL_SOIL_MOISTURE.value;
        for (int i = 0; i < n; i++) { soilMoisture[i] = init; yield[i] = 1f; cropState[i] = "growing"; }
    }

    @Override
    public void advanceDay(World world) {
        Climate cl = world.climate;
        Calendar cal = world.calendar;

        for (int s = 0; s < geo.count(); s++) {
            float precip = cl.precipitation[s];
            float temp = cl.temperature[s];

            soilMoisture[s] += precip * AgricultureConfig.SOIL_RECHARGE_RATE.value;
            float evap = AgricultureConfig.EVAPORATION_BASE.value
                    + Math.max(0f, temp - AgricultureConfig.EVAPORATION_TEMP_BASE.value)
                    * AgricultureConfig.EVAPORATION_PER_DEGREE.value;
            float drain = AgricultureConfig.SOIL_DRAINAGE_RATE.value * soilMoisture[s];
            soilMoisture[s] = clamp01(soilMoisture[s] - evap - drain);

            float tempSuit = tempSuitability(temp);
            float moistSuit = moistureSuitability(soilMoisture[s]);
            float base = cal.growingSeason ? tempSuit * moistSuit : AgricultureConfig.DORMANT_YIELD.value;
            float floodMul = Math.max(0f, 1f - world.water.floodSeverity[s] * WaterConfig.FLOOD_CROP_PENALTY.value);
            yield[s] = clamp(base * (1f - blight[s]) * floodMul, 0f, AgricultureConfig.MAX_YIELD_FACTOR.value);

            cropState[s] = label(cal.growingSeason, soilMoisture[s], temp, yield[s]);
            if (world.water.floodSeverity[s] > 0.3f) cropState[s] = "flooded";
            blight[s] *= AgricultureConfig.BLIGHT_DECAY.value;
        }
    }

    private float tempSuitability(float t) {
        float ideal = AgricultureConfig.IDEAL_CROP_TEMP.value;
        float tol = AgricultureConfig.TEMP_TOLERANCE.value;
        float d = t - ideal;
        return (float) (1.1 * Math.exp(-(d * d) / (2 * tol * tol)));
    }

    private float moistureSuitability(float m) {
        float full = AgricultureConfig.MOISTURE_FOR_FULL_YIELD.value;
        float ms = Math.min(1f, m / full);
        float wl = AgricultureConfig.WATERLOGGING_THRESHOLD.value;
        if (m > wl) ms *= Math.max(0f, 1f - (m - wl) * 2f);
        return ms * 1.1f;
    }

    private String label(boolean growing, float soil, float temp, float y) {
        if (!growing) return "dormant";
        if (soil < 0.2f) return "drought-stressed";
        if (soil > AgricultureConfig.WATERLOGGING_THRESHOLD.value) return "waterlogged";
        if (temp > AgricultureConfig.IDEAL_CROP_TEMP.value + AgricultureConfig.TEMP_TOLERANCE.value) return "heat-stressed";
        if (temp < AgricultureConfig.IDEAL_CROP_TEMP.value - AgricultureConfig.TEMP_TOLERANCE.value) return "cold-stressed";
        return y > 1f ? "thriving" : "growing";
    }

    public float yieldForKingdom(int kingdomId) {
        if (kingdomId == 0) return realmAverageYield();
        if (kingdomId < 0 || kingdomId >= yield.length) return 1f;
        return yield[kingdomId];
    }

    public float realmAverageYield() {
        float sum = 0;
        for (float y : yield) sum += y;
        return yield.length > 0 ? sum / yield.length : 1f;
    }

    // Land pressure: 1.0 with room to spare, falling as people crowd the arable land.
    // Irrigation tech raises the arable ceiling, easing crowding directly.
    public float landDensityForKingdom(int kingdomId, int population) {
        if (population <= 0) return 1f;
        float cap = (kingdomId == 0)
                ? AgricultureConfig.ARABLE_LAND_CAPACITY.value * geo.count()
                : AgricultureConfig.ARABLE_LAND_CAPACITY.value;
        cap *= (1f + TechManager.bonus(TechEffect.ARABLE)); // irrigation works
        return Math.min(1f, cap / population);
    }

    public boolean applyBlight(int sectorId, float amount) {
        if (sectorId < 0 || sectorId >= blight.length) return false;
        blight[sectorId] = Math.min(AgricultureConfig.DIRECTOR_BLIGHT_CAP.value,
                blight[sectorId] + Math.max(0f, amount));
        return true;
    }

    @Override
    public String reportLine() {
        int worst = 0;
        for (int i = 1; i < yield.length; i++) if (yield[i] < yield[worst]) worst = i;
        return String.format("Harvest: realm yield %.0f%%, weakest %s at %.0f%% (%s)",
                realmAverageYield() * 100, geo.sector(worst).region(), yield[worst] * 100, cropState[worst]);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}