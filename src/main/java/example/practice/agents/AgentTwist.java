package example.practice.agents;

// The small twist - the thing that makes a character their own in a fight or a
// scheme. Stat effects are applied in Agent.deriveStats(); behaviour in AgentCombat.
public enum AgentTwist {
    DUELIST("Duelist", "First blade drawn, first blood spilled."),
    SHADOW("Shadow", "You never see the one that kills you."),
    JUGGERNAUT("Juggernaut", "Walls fall before this one yields."),
    GLASS_CANNON("Glass Cannon", "Hits like a god, breaks like glass."),
    SILVER_TONGUE("Silver Tongue", "Wins the room without a sword."),
    WARLORD("Warlord", "Strongest when the banners follow."),
    SURVIVOR("Survivor", "Has died once already. It didn't take."),
    WILDCARD("Wildcard", "Nobody knows what they'll do. Not even them."),
    IMMOVABLE("Immovable", "Speaks for all, sways for none.");

    public final String label, flavor;
    AgentTwist(String label, String flavor){ this.label = label; this.flavor = flavor; }
}