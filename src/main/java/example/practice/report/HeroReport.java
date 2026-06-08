package example.practice.report;

// One notable figure. health/maxHealth are nullable on purpose: the Emperor and
// (eventually) tracked heroes have real health, but figures that don't track it
// yet - the Jokers as a static roster - report null instead of a fake number.
public class HeroReport {
    public String category;   // EMPEROR, COUNCIL, GENERAL, JOKER
    public String name;
    public String role;       // title / function
    public boolean alive;

    public Integer health;    // null = no live health tracked yet
    public Integer maxHealth;

    public String detail;     // stats line, disposition, description, etc.
}