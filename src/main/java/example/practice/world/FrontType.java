package example.practice.world;

// The kinds of weather front. Each carries a signature: how it bends temperature,
// precipitation, and wind where it passes. Coefficients multiply the front's
// local intensity (0..1+) to produce the actual deltas on a sector.
public enum FrontType {
    //         label         tempCoef  precipCoef  windCoef
    RAINBAND ("rain band",    0f,       0.5f,       0.1f),
    STORM    ("storm",       -2f,       0.6f,       0.8f),
    HEATWAVE ("heat wave",    8f,      -0.3f,       0.0f),
    COLDSNAP ("cold snap",   -9f,       0.1f,       0.3f),
    DRYSPELL ("dry spell",    3f,      -0.4f,       0.0f);

    public final String label;
    public final float tempCoef, precipCoef, windCoef;

    FrontType(String label, float tempCoef, float precipCoef, float windCoef) {
        this.label = label;
        this.tempCoef = tempCoef;
        this.precipCoef = precipCoef;
        this.windCoef = windCoef;
    }

    // Relative likelihood of spawning this season. Heat waves belong to summer,
    // cold snaps to winter, and so on - so what threatens you shifts with the year.
    public float seasonalWeight(Season s) {
        switch (this) {
            case HEATWAVE: return s == Season.SUMMER ? 3f : s == Season.SPRING ? 1f : 0.2f;
            case COLDSNAP: return s == Season.WINTER ? 3f : s == Season.AUTUMN ? 1f : 0.2f;
            case STORM:    return (s == Season.AUTUMN || s == Season.WINTER) ? 2f : 1f;
            case RAINBAND: return (s == Season.SPRING || s == Season.AUTUMN) ? 2f : 1f;
            case DRYSPELL: return s == Season.SUMMER ? 2.5f : s == Season.WINTER ? 0.3f : 1f;
        }
        return 1f;
    }
}