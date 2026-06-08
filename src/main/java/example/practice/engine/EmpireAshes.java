package example.practice.engine;

// A fingerprint of the Empire's death. The seeder reads this to decide what kind
// of successors rise - a famine collapse breeds different kingdoms than a
// rebel bloodbath. This is the "ashes" your roadmap talks about.
public class EmpireAshes {
    public int finalUnrest;
    public int finalMorale;
    public float foodDaysLeft;       // how close to starvation it was
    public float rebelFraction;      // what share of the people had taken up arms
    public int generalCount;         // surviving command - seeds militarist successors
    public int dominantCivilJob;     // 1..5, the trade the population leaned on
    public int survivingPopulation;

    // Derived collapse flavor (computed once at capture)
    public boolean famineCollapse;
    public boolean radicalCollapse;
    public boolean militaristCollapse;
}
