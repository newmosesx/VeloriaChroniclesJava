package example.practice.report;

// A kingdom's headcount across every job, mapped from Kingdom.jobCounts.
// Index meaning: 0 unemployed, 1 farmer ... 9 rebel.
public class Census {
    public int unemployed;
    public int farmers, butchers, lumberjacks, miners, blacksmiths;
    public int swordsmen, archers, cavalry;
    public int rebels;
}