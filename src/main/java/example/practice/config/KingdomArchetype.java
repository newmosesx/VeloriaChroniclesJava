package example.practice.config;

// The seven shapes a successor kingdom can take when the Empire falls. The World
// Director / seeder picks among these based on HOW the Empire died, then bends
// each one's starting traits. This is your roadmap #6 "Kingdom Archetypes" as data.
public enum KingdomArchetype {
    //              descriptor                productionMod  startUnrest  moraleBonus  militancy  resourceBias
    WARLORD_STATE    ("the Warlord State",      1.00f,        250,         10,          0.15f,     "metal"),
    MERCHANT_REPUBLIC("the Merchant Republic",  1.10f,        150,          0,          0.04f,     "treasury"),
    THEOCRACY        ("the Theocracy",          0.95f,        100,          5,          0.06f,     "food"),
    AGRARIAN_COMMUNE ("the Agrarian Commune",   1.20f,         80,          0,          0.03f,     "food"),
    FORTRESS_REMNANT ("the Fortress Remnant",   0.90f,        200,         15,          0.20f,     "metal"),
    NOMAD_HORDE      ("the Nomad Horde",        1.05f,        300,          5,          0.12f,     "food"),
    FREE_CITY        ("the Free City",          1.15f,        120,          0,          0.05f,     "treasury");

    public final String descriptor;     // appended to the kingdom's name
    public final float productionMod;    // multiplies resource output
    public final int startUnrest;        // baseline unrest at birth
    public final int moraleBonus;        // added to starting morale
    public final float militancy;        // target fraction of population under arms
    public final String resourceBias;    // which resource it starts rich in

    KingdomArchetype(String descriptor, float productionMod, int startUnrest,
                     int moraleBonus, float militancy, String resourceBias) {
        this.descriptor = descriptor;
        this.productionMod = productionMod;
        this.startUnrest = startUnrest;
        this.moraleBonus = moraleBonus;
        this.militancy = militancy;
        this.resourceBias = resourceBias;
    }
}