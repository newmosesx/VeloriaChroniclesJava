package example.practice.story;

import example.practice.config.CouncilArchetype;

import java.util.EnumMap;
import java.util.Map;

// This is your "1 token = 1 sentence" model. Each constant is a whole line the
// Emperor can speak; its effect on a listener depends on that listener's
// archetype. No training, no inference - just a lookup table you author by hand,
// which is exactly the control you want for story branches.
public enum DialogueToken {
    AUTHORITY("\"I acted to secure the future.\""),
    DIPLOMACY("\"We must unite in this crisis.\""),
    CONCESSION("\"Fund the people from the treasury.\""),
    TYRANNY("\"Silence! Guards - arrest this fool.\""),
    REASSURANCE("\"You have my word - I would never betray this council.\""),
    SILENCE("... (the Emperor says nothing)");   // used for the debate timeout

    public final String line;
    private final Map<CouncilArchetype, RelationshipDelta> effects = new EnumMap<>(CouncilArchetype.class);

    DialogueToken(String line) { this.line = line; }

    private void set(CouncilArchetype a, RelationshipDelta d) { effects.put(a, d); }

    public RelationshipDelta deltaFor(CouncilArchetype a) {
        return effects.getOrDefault(a, new RelationshipDelta(0, 0, 0, 0));
    }

    // (tension, suspicion, trust, likeness)
    static {
        AUTHORITY.set(CouncilArchetype.HAWK,       new RelationshipDelta(-6, -5, 12,  0));
        AUTHORITY.set(CouncilArchetype.PRAGMATIST, new RelationshipDelta( 5,  5,  0, -3));
        AUTHORITY.set(CouncilArchetype.ZEALOT,     new RelationshipDelta( 3,  8,  0,  0));

        DIPLOMACY.set(CouncilArchetype.HAWK,       new RelationshipDelta( 0,  0, -4, -6));
        DIPLOMACY.set(CouncilArchetype.PRAGMATIST, new RelationshipDelta(-6, -6, 12, 12));
        DIPLOMACY.set(CouncilArchetype.ZEALOT,     new RelationshipDelta( 0,  0,  6,  4));

        CONCESSION.set(CouncilArchetype.HAWK,       new RelationshipDelta( 0, 10, -6,  0));
        CONCESSION.set(CouncilArchetype.PRAGMATIST, new RelationshipDelta(-5,  0,  0, 10));
        CONCESSION.set(CouncilArchetype.ZEALOT,     new RelationshipDelta( 0, -6,  0, 12));

        TYRANNY.set(CouncilArchetype.HAWK,       new RelationshipDelta(12, 10,  5,   0));
        TYRANNY.set(CouncilArchetype.PRAGMATIST, new RelationshipDelta(22, 25, -15, -15));
        TYRANNY.set(CouncilArchetype.ZEALOT,     new RelationshipDelta(20, 22,  0,  -12));

        RelationshipDelta warm = new RelationshipDelta(-8, -8, 12, 18);
        REASSURANCE.set(CouncilArchetype.HAWK,       warm);
        REASSURANCE.set(CouncilArchetype.PRAGMATIST, warm);
        REASSURANCE.set(CouncilArchetype.ZEALOT,     warm);

        SILENCE.set(CouncilArchetype.HAWK,       new RelationshipDelta(10,  8, -6, -5));  // hawks despise weakness most
        SILENCE.set(CouncilArchetype.PRAGMATIST, new RelationshipDelta( 8,  6, -4, -3));
        SILENCE.set(CouncilArchetype.ZEALOT,     new RelationshipDelta( 9,  7, -4, -3));
    }
}