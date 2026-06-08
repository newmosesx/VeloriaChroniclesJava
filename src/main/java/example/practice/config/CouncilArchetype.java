package example.practice.config;

// The "personality class" a council member belongs to.
// Dialogue tokens carry different effects per archetype, so this is reusable
// across any number of members (5, 50, 500 - it's all just lookups).
public enum CouncilArchetype {
    HAWK,        // military hardliner - despises weakness, respects decisiveness
    PRAGMATIST,  // economy/diplomacy - rewards unity, punishes cruelty
    ZEALOT       // religious - suspicious by default, slow to trust
}