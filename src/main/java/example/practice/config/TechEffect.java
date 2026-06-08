package example.practice.config;

// What a technology improves once it's online. Systems read the summed bonus for
// each effect; adding a new effect is one line here plus one read at the use site.
public enum TechEffect {
    FOOD,      // food produced per farmer
    ARABLE,    // arable land capacity (the carrying-capacity ceiling)
    OFFENSE,   // army effectiveness in a fight
    DEFENSE,   // army resilience / fortification
    RESOURCE,  // wood / stone / metal yield
    GRANARY,   // how deep the granary can stock
    ORDER      // bureaucracy: less unrest per unit of grievance
}