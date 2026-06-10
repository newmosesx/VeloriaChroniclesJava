package example.practice.events;

// The lingering wake of a disaster. A drought or flood doesn't just resolve in one
// log line - it leaves SCARCITY on the land, a 0..1 pressure that decays over the
// following days and, while it lasts, makes follow-on events (riots, gouging,
// raids on a weakened realm) more likely and more severe. This is what turns a run
// of bad luck into a CRISIS instead of a list of unrelated incidents.
//
// Kept deliberately tiny: one float per kingdom, ticked once a day. EventSystem
// reads it to bias fire chances and severity; nothing else needs to know it exists.
public final class EventAftermath {

    private EventAftermath() {}

    private static final float[] scarcity = new float[64];   // 0..1 per kingdom
    private static final float DECAY = 0.06f;                // eased off each day

    public static float scarcity(int kingdomId) {
        return (kingdomId >= 0 && kingdomId < scarcity.length) ? scarcity[kingdomId] : 0f;
    }

    // A shock adds to the wake (capped at 1). Bigger shocks leave deeper scars.
    public static void addScarcity(int kingdomId, float amount) {
        if (kingdomId < 0 || kingdomId >= scarcity.length) return;
        scarcity[kingdomId] = Math.min(1f, scarcity[kingdomId] + Math.max(0f, amount));
    }

    // Relief (a bountiful harvest, a gold strike) heals the wake faster.
    public static void easeScarcity(int kingdomId, float amount) {
        if (kingdomId < 0 || kingdomId >= scarcity.length) return;
        scarcity[kingdomId] = Math.max(0f, scarcity[kingdomId] - Math.max(0f, amount));
    }

    // Called once per kingdom per day by EventSystem before the scan.
    public static void decay(int kingdomId) {
        if (kingdomId < 0 || kingdomId >= scarcity.length) return;
        scarcity[kingdomId] = Math.max(0f, scarcity[kingdomId] - DECAY);
    }

    public static void resetAll() {
        for (int i = 0; i < scarcity.length; i++) scarcity[i] = 0f;
    }
}