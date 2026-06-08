package example.practice.report;

// Everything worth knowing about one kingdom at a moment in time.
public class KingdomReport {
    public int id;
    public String name;
    public boolean active;
    public String archetypeName;     // EMPIRE, WARLORD_STATE, etc.

    public int population;
    public int unrest;
    public int stabilityPercent;     // 100 = calm, 0 = at the collapse line
    public int morale;

    public int food, wood, stone, metal, treasury;
    public float foodDaysLeft;

    public Census census;
    public int soldiers, rebels, civilians, unemployed;
    public int generals, rebelLeaders;

    public boolean limitersDisabled;
    public int divineDaysLeft;

    // Derived for the World Director: where to apply (or relieve) pressure.
    public String weakestPillar;     // "Food", "Military", "Unrest", or "None"
    public float threatLevel;        // 0..1, the severity of that weakest pillar
}