package example.practice.story;

import example.practice.config.CouncilArchetype;
import example.practice.config.RelationshipConfig;

// One NPC's relationship state. This is the whole "agent": four ints and a
// boolean. It costs bytes, runs instantly, and is fully deterministic. The
// apply() method is a 1:1 port of the interactive demo's update rule.
public class CouncilMember {
    public final String name;
    public final String title;
    public final CouncilArchetype archetype;

    public int tension, suspicion, trust, likeness;
    public boolean locked = false;

    public CouncilMember(String name, String title, CouncilArchetype archetype,
                         int tension, int suspicion, int trust, int likeness) {
        this.name = name;
        this.title = title;
        this.archetype = archetype;
        this.tension = tension;
        this.suspicion = suspicion;
        this.trust = trust;
        this.likeness = likeness;
    }

    public void apply(DialogueToken token) {
        RelationshipDelta d = token.deltaFor(archetype);
        tension   = clamp(tension   + dampThreat(d.tension));
        suspicion = clamp(suspicion + dampThreat(d.suspicion));
        trust     = clamp(trust     + dampGoodwill(d.trust));
        likeness  = clamp(likeness  + dampGoodwill(d.likeness));
        updateLock();
    }

    // Positive trust/likeness barely lands while a grudge is active.
    private float dampGoodwill(int delta) {
        if (locked && delta > 0) return delta * RelationshipConfig.LOCKED_GOODWILL_DAMP.value;
        return delta;
    }

    // Reductions to suspicion/tension are slowed while locked - grudges fade hard.
    private float dampThreat(int delta) {
        if (locked && delta < 0) return delta * RelationshipConfig.LOCKED_DECAY_DAMP.value;
        return delta;
    }

    private void updateLock() {
        if (suspicion >= RelationshipConfig.LOCK_SUSPICION_THRESHOLD.value
                || tension >= RelationshipConfig.LOCK_TENSION_THRESHOLD.value) {
            locked = true;
        }
        if (suspicion < RelationshipConfig.UNLOCK_SUSPICION_CEILING.value
                && tension < RelationshipConfig.UNLOCK_TENSION_CEILING.value) {
            locked = false;
        }
    }

    private int clamp(float v) {
        int min = (int) RelationshipConfig.STAT_MIN.value;
        int max = (int) RelationshipConfig.STAT_MAX.value;
        return Math.max(min, Math.min(max, Math.round(v)));
    }

    public Disposition disposition() {
        return Disposition.resolve(this);
    }
}