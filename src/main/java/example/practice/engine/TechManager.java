package example.practice.engine;

import example.practice.config.TechEffect;
import example.practice.config.TechType;
import example.practice.kingdoms.Kingdom;

// The reigning empire's research. A tech is paid for up front, takes its research
// time to complete, then ramps in at +20% per day - so a fresh breakthrough isn't
// fully online for five days, and a disaster that lands during the ramp catches you
// only partly covered. No limit on how many you pursue; gold is the only ceiling.
//
// NOTE: treasury is read as emperor.gold - rename if your field differs.
public class TechManager {
    public enum State { LOCKED, RESEARCHING, IMPLEMENTING, ACTIVE }

    private static final int N = TechType.values().length;
    private static final State[] state = new State[N];
    private static final int[] daysLeft = new int[N];
    private static final float[] progress = new float[N];
    static { for (int i = 0; i < N; i++) state[i] = State.LOCKED; }

    public static State stateOf(TechType t) { return state[t.ordinal()]; }
    public static int daysLeft(TechType t)  { return daysLeft[t.ordinal()]; }
    public static float progressOf(TechType t) { return progress[t.ordinal()]; }

    public static boolean startResearch(TechType t, Kingdom emperor) {
        int i = t.ordinal();
        if (state[i] != State.LOCKED) return false;
        if (emperor.gold < t.cost) return false;
        emperor.gold -= t.cost;
        state[i] = State.RESEARCHING;
        daysLeft[i] = t.days;
        progress[i] = 0f;
        return true;
    }

    public static void processDaily() {
        for (int i = 0; i < N; i++) {
            if (state[i] == State.RESEARCHING) {
                if (--daysLeft[i] <= 0) { state[i] = State.IMPLEMENTING; progress[i] = 0f; }
            } else if (state[i] == State.IMPLEMENTING) {
                progress[i] += 0.20f; // +20% per day
                if (progress[i] >= 1f) { progress[i] = 1f; state[i] = State.ACTIVE; }
            }
        }
    }

    // 0 before research finishes, ramps through implementation, 1.0 when fully online.
    public static float strength(TechType t) {
        int i = t.ordinal();
        if (state[i] == State.ACTIVE) return 1f;
        if (state[i] == State.IMPLEMENTING) return progress[i];
        return 0f;
    }

    // Total live bonus for an effect: Σ magnitude × strength over the relevant techs.
    public static float bonus(TechEffect effect) {
        float sum = 0f;
        for (TechType t : TechType.values()) if (t.effect == effect) sum += t.magnitude * strength(t);
        return sum;
    }
}