package example.practice.story;

import example.practice.config.RelationshipConfig;

// The single readable summary of a relationship. The resolver encodes the rule
// you actually care about: high suspicion/tension OVERRIDE high trust/likeness.
// A death threat can't be undone by one "I love you" - that's the hard cap below.
public enum Disposition {
    HOSTILE, WARY, NEUTRAL, WARM, LOYAL;

    public static Disposition resolve(CouncilMember m) {
        // Hard override: past these, nothing else matters.
        if (m.suspicion >= RelationshipConfig.HARD_HOSTILE_SUSPICION.value
                || m.tension >= RelationshipConfig.HARD_HOSTILE_TENSION.value) {
            return HOSTILE;
        }
        // Locked grudge caps you at Wary even if the warm stats look good.
        if (m.locked) return WARY;

        float score = 0.35f * m.trust
                + 0.35f * m.likeness
                - 0.55f * m.suspicion
                - 0.45f * m.tension;

        if (score >= RelationshipConfig.DISP_LOYAL.value)   return LOYAL;
        if (score >= RelationshipConfig.DISP_WARM.value)    return WARM;
        if (score >= RelationshipConfig.DISP_NEUTRAL.value) return NEUTRAL;
        if (score >= RelationshipConfig.DISP_WARY.value)    return WARY;
        return HOSTILE;
    }
}