package example.practice.config;

// cost is paid up front in gold to begin research; days is the research time (longer
// for bigger payoffs); magnitude is the full bonus once implementation finishes.
// Adding a technology is a single line - the only gate on the player is gold.
public enum TechType {
    HEAVY_PLOUGH("Heavy plough", TechEffect.FOOD, 250, 10, 0.20f),
    CROP_ROTATION("Crop rotation", TechEffect.FOOD, 300, 12, 0.25f),
    IRRIGATION("Irrigation works", TechEffect.ARABLE, 500, 20, 0.40f),
    DRILL_AND_DISCIPLINE("Drill and discipline", TechEffect.OFFENSE, 350, 14, 0.30f),
    FORTIFICATION("Fortification", TechEffect.DEFENSE, 450, 18, 0.35f),
    IMPROVED_TOOLS("Improved tools", TechEffect.RESOURCE, 300, 12, 0.30f),
    STATE_GRANARIES("State granaries", TechEffect.GRANARY, 400, 16, 0.50f),
    BUREAUCRACY("Bureaucracy", TechEffect.ORDER, 600, 24, 0.30f);

    public final String title;
    public final TechEffect effect;
    public final int cost, days;
    public final float magnitude;
    TechType(String title, TechEffect effect, int cost, int days, float magnitude){
        this.title = title; this.effect = effect; this.cost = cost; this.days = days; this.magnitude = magnitude;
    }
}