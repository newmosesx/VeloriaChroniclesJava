package example.practice.world;

import example.practice.config.WaterConfig;

// Rivers, snowmelt, and floods - derived from the weather, never declared. Water
// rises in the northern highlands and cascades south to the coast, so the
// southern deltas carry the most water and flood first. Rain feeds the rivers;
// below freezing it banks as snowpack and releases in the spring thaw. Floods
// drag down the harvest (Agriculture reads floodSeverity). Levees raise a
// sector's flood threshold but shove the overflow downstream - protect yourself,
// drown your neighbour - and a big enough flood breaches them.
public class Water implements WorldSystem {

    private final Geography geo;

    public final float[] riverLevel;     // persistent water in each sector's channel
    public final float[] snowpack;       // frozen water waiting for the thaw
    public final float[] floodSeverity;  // 0..1, what consumers read
    public final float[] leveeHeight;    // player-built defences
    public final String[] condition;     // label for the inspector

    private final float[] elevation;     // north = high ground, south = coast
    private final boolean[] coastal;
    private final int[] flowOrder;       // sector ids, upstream (high) -> downstream (low)

    public Water(Geography geo) {
        this.geo = geo;
        int n = geo.count();
        riverLevel = new float[n];
        snowpack = new float[n];
        floodSeverity = new float[n];
        leveeHeight = new float[n];
        condition = new String[n];
        elevation = new float[n];
        coastal = new boolean[n];

        // Mirrors the 8-sector compass layout (id = CompassDirection ordinal:
        // N, NE, E, SE, S, SW, W, NW). Extra sectors default to flat inland.
        float[] elev = { 1.0f, 0.7f, 0.0f, -0.7f, -1.0f, -0.7f, 0.0f, 0.7f };
        boolean[] coast = { false, false, false, true, true, true, false, false };
        for (int i = 0; i < n; i++) {
            elevation[i] = i < elev.length ? elev[i] : 0f;
            coastal[i] = i < coast.length && coast[i];
            condition[i] = "normal";
        }

        // Precompute the downstream cascade: highest ground first.
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Float.compare(elevation[b], elevation[a]));
        flowOrder = new int[n];
        for (int i = 0; i < n; i++) flowOrder[i] = idx[i];
    }

    @Override
    public void advanceDay(World world) {
        Climate cl = world.climate;
        int n = riverLevel.length;

        // 1. Weather into the channels (or the snowpack, if it's freezing).
        for (int s = 0; s < n; s++) {
            float precip = cl.precipitation[s];
            float temp = cl.temperature[s];
            float input = precip * WaterConfig.RAIN_TO_RIVER.value;

            if (temp < WaterConfig.SNOW_TEMP.value) snowpack[s] += input;
            else riverLevel[s] += input;

            if (temp > WaterConfig.MELT_TEMP.value && snowpack[s] > 0f) {
                float melt = Math.min(snowpack[s],
                        snowpack[s] * WaterConfig.MELT_RATE.value * Math.min(3f, temp - WaterConfig.MELT_TEMP.value));
                riverLevel[s] += melt;
                snowpack[s] -= melt;
            }
            riverLevel[s] = Math.max(0f, riverLevel[s] - WaterConfig.RIVER_BASELINE_LOSS.value);
        }

        // 2. Cascade downhill, high ground to coast; the sea takes the rest.
        for (int k = 0; k < n; k++) {
            int s = flowOrder[k];
            float out = riverLevel[s] * WaterConfig.FLOW_DOWNSTREAM.value;
            riverLevel[s] -= out;
            int down = (k + 1 < n) ? flowOrder[k + 1] : -1;
            if (!coastal[s] && down >= 0) riverLevel[down] += out; // else: drains to sea
            if (coastal[s]) riverLevel[s] -= riverLevel[s] * WaterConfig.SEA_DRAINAGE.value;
        }

        // 3. Flooding, levees, and diversion - processed downstream so a diverted
        //    overflow reaches the next sector before its own flood is judged.
        for (int k = 0; k < n; k++) {
            int s = flowOrder[k];
            int down = (k + 1 < n) ? flowOrder[k + 1] : -1;
            float threshold = WaterConfig.CHANNEL_CAPACITY.value + leveeHeight[s] * WaterConfig.LEVEE_PER_UNIT.value;

            if (riverLevel[s] > threshold) {
                float over = riverLevel[s] - threshold;
                riverLevel[s] = threshold;

                if (leveeHeight[s] > 0f && down >= 0 && !coastal[s]) {
                    // The wall holds - but the water has to go somewhere.
                    riverLevel[down] += over * WaterConfig.DIVERT_FRACTION.value;
                    floodSeverity[s] = Math.max(0f, floodSeverity[s] - WaterConfig.FLOOD_RECEDE.value);
                } else {
                    floodSeverity[s] = Math.min(1f, over / WaterConfig.FLOOD_SCALE.value);
                    if (floodSeverity[s] > WaterConfig.LEVEE_BREACH_SEVERITY.value)
                        leveeHeight[s] = Math.max(0f, leveeHeight[s] - WaterConfig.LEVEE_BREACH_EROSION.value);
                }
            } else {
                floodSeverity[s] = Math.max(0f, floodSeverity[s] - WaterConfig.FLOOD_RECEDE.value);
            }

            leveeHeight[s] = Math.max(0f, leveeHeight[s] - WaterConfig.LEVEE_DECAY.value); // maintenance erodes
            condition[s] = label(s);
        }
    }

    private String label(int s) {
        if (floodSeverity[s] > 0.5f) return "flooding";
        if (floodSeverity[s] > 0.1f) return "swelling";
        if (riverLevel[s] > WaterConfig.CHANNEL_CAPACITY.value * 0.7f) return "high";
        if (snowpack[s] > 2f) return "frozen";
        if (riverLevel[s] < 0.5f) return "low";
        return "normal";
    }

    // --- PLAYER MECHANIC ---
    // Raise a sector's levee (the caller pays the stone/labour). Returns false if
    // the sector id is out of range.
    public boolean buildLevee(int sectorId, float amount) {
        if (sectorId < 0 || sectorId >= leveeHeight.length) return false;
        leveeHeight[sectorId] = Math.min(WaterConfig.LEVEE_MAX.value,
                leveeHeight[sectorId] + Math.max(0f, amount));
        return true;
    }

    // --- DIRECTOR'S BOUNDED LEVER ---
    // The Director can swell a river that already runs - it cannot conjure a flood
    // out of a dry channel. Returns false if there's nothing real to swell.
    public boolean swellRiver(int sectorId, float amount) {
        if (sectorId < 0 || sectorId >= riverLevel.length) return false;
        if (riverLevel[sectorId] < WaterConfig.SWELL_MIN_RIVER.value) return false;
        riverLevel[sectorId] += Math.min(WaterConfig.DIRECTOR_SWELL_CAP.value, Math.max(0f, amount));
        return true;
    }

    @Override
    public String reportLine() {
        int worst = 0;
        for (int i = 1; i < floodSeverity.length; i++) if (floodSeverity[i] > floodSeverity[worst]) worst = i;
        if (floodSeverity[worst] > 0.05f)
            return String.format("Rivers: %s flooding (%.0f%%), level %.1f, levee %.1f",
                    geo.sector(worst).region(), floodSeverity[worst] * 100, riverLevel[worst], leveeHeight[worst]);
        int high = 0;
        for (int i = 1; i < riverLevel.length; i++) if (riverLevel[i] > riverLevel[high]) high = i;
        return String.format("Rivers: calm, highest %s at %.1f", geo.sector(high).region(), riverLevel[high]);
    }
}