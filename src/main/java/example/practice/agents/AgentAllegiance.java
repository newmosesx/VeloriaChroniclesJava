package example.practice.agents;

// Who a character answers to. INDEPENDENT (the Jokers) hold no authority over
// anyone, but they climb fast and swing for or against the throne on their own
// terms. The rest are tied to a power bloc they can sway.
public enum AgentAllegiance {
    INDEPENDENT, // the Jokers - an adventurer band; a dagger or a shield
    ARMY,        // generals and commanders - sway the imperial army
    NAVY,        // admirals - sway the fleet (story: later)
    COUNCIL,     // courtiers and advisors at the seat of power
    CROWN        // sworn servants like the Speaker
}