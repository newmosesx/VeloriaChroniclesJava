package example.practice.config;

// The numbers you tuned on the sliders. These are the dials of the whole
// food <-> population loop - adjust them in the inspector and re-tune any time.
public enum SubsistenceConfig {
    FOOD_PER_FARMER(20),         // a working farmer's daily output in good conditions
    FOOD_PER_PERSON(1),         // daily ration each person eats

    BIRTH_RATE(0.010f),         // max daily birth fraction when the granary is full
    NATURAL_DEATH_RATE(0.005f), // baseline daily death fraction
    STARVATION_FRACTION(0.30f), // fraction of the unfed who die that day

    GRANARY_DAYS(120),          // days of food a realm can store, per person
    BIRTH_STORE_DAYS(30);       // days of stored food per person for the full birth rate

    public final float value;
    SubsistenceConfig(float value){ this.value = value; }
}