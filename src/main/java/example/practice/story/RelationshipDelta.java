package example.practice.story;

// One "token's" effect on a single relationship: how much it moves each of the
// four core vectors. Order everywhere is (tension, suspicion, trust, likeness).
public class RelationshipDelta {
    public final int tension, suspicion, trust, likeness;

    public RelationshipDelta(int tension, int suspicion, int trust, int likeness) {
        this.tension = tension;
        this.suspicion = suspicion;
        this.trust = trust;
        this.likeness = likeness;
    }
}