package example.practice.engine;

import example.practice.config.FactionType;

// One estate's standing in a realm. grievance is eased toward a derived target so
// anger has inertia (it lingers, like a held grudge); power is how much that anger
// can actually move. pressure = grievance x power is the estate's contribution to
// unrest.
public class Faction {
    public final FactionType type;
    public float grievance;   // 0..1, current (eased) anger
    public float target;      // 0..1, where the world says it should be today
    public float power;       // 0..1, capacity to act

    public Faction(FactionType type) {
        this.type = type;
        this.power = type.basePower;
    }

    public void ease(float rate) {
        grievance += (target - grievance) * rate;
        if (grievance < 0f) grievance = 0f;
        if (grievance > 1f) grievance = 1f;
    }

    // Event/story hook: a shock that spikes anger now; the easing relaxes it back
    // toward the derived target over the following days.
    public void aggravate(float amount) {
        grievance = Math.min(1f, grievance + Math.max(0f, amount));
    }

    public float pressure() { return grievance * power; }
}