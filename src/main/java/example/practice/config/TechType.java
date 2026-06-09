package example.practice.config;

// cost is paid up front in gold to begin research; days is the research time (longer
// for bigger payoffs); magnitude is the full bonus once implementation finishes;
// rolloutPerDay is how fast it ramps in after research (default 20%/day).
// Adding a technology is a single line - the only gate on the player is gold.
public enum TechType {
    HEAVY_PLOUGH("Heavy plough", TechEffect.FOOD, 250, 5, 0.20f),
    CROP_ROTATION("Crop rotation", TechEffect.FOOD, 300, 7, 0.25f),
    IRRIGATION("Irrigation works", TechEffect.ARABLE, 500, 15, 0.40f),
    DRILL_AND_DISCIPLINE("Drill and discipline", TechEffect.OFFENSE, 350, 14, 0.30f),
    FORTIFICATION("Fortification", TechEffect.DEFENSE, 450, 18, 0.35f),
    IMPROVED_TOOLS("Improved tools", TechEffect.RESOURCE, 300, 12, 0.30f),
    STATE_GRANARIES("State granaries", TechEffect.GRANARY, 400, 16, 0.50f),
    BUREAUCRACY("Bureaucracy", TechEffect.ORDER, 600, 24, 0.30f),

    // --- new ---
    FORCED_LEVY("Forced levy", TechEffect.RECRUIT, 350, 8, 0.50f),                 // +50% recruits per day
    DEEP_MINING("Deep mining", TechEffect.MINERAL, 400, 5, 0.60f, 0.10f),          // 5-day research, ramps 10%/day, +60% metal
    QUARTERMASTER_CORPS("Quartermaster corps", TechEffect.SUPPLY, 400, 10, 0.40f); // +40% army provisioning

    public final String title;
    public final TechEffect effect;
    public final int cost, days;
    public final float magnitude;
    public final float rolloutPerDay;   // fraction implemented per day after research finishes

    TechType(String title, TechEffect effect, int cost, int days, float magnitude){
        this(title, effect, cost, days, magnitude, 0.20f);
    }
    TechType(String title, TechEffect effect, int cost, int days, float magnitude, float rolloutPerDay){
        this.title = title; this.effect = effect; this.cost = cost; this.days = days;
        this.magnitude = magnitude; this.rolloutPerDay = rolloutPerDay;
    }
}