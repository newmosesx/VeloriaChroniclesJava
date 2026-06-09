package example.practice.engine;

import example.practice.config.Battle;

/**
 * One real battle, resolved a window at a time.
 *
 * This is the shared core for BOTH command modes: the same object is stepped by
 * the AutoCommander (Castius, on the sim thread) and by the player's decision
 * windows (on the FX thread). It holds no engine state and touches no population
 * -- it just turns commands into casualties. BattleManager builds it, feeds it
 * leaders/multipliers, and applies the final BattleReport to the world.
 *
 * Rules (per the design):
 *   - A leader must lead each side; their strength multiplier is passed in.
 *   - Each side may reinforce twice: a wave is 5-10% of the army NOT in the fight.
 *   - Up to 5 decision windows; the battle ends only in retreat or annihilation
 *     (or, if it runs the full 5 windows, the weaker side breaks off).
 *   - A commander's read of the enemy drifts (assess()), so calls can misfire.
 */
public final class FieldBattle {

    public enum Side { IMPERIAL, REBEL }

    /** A leader's (fuzzed) read of the field at a decision window. */
    public static final class Assess {
        public final double ownStrength;     // true
        public final double estEnemyStrength; // drifted
        public final int reinforcementsLeft;
        public final int windowsLeft;
        Assess(double own, double est, int reinf, int windows) {
            this.ownStrength = own; this.estEnemyStrength = est;
            this.reinforcementsLeft = reinf; this.windowsLeft = windows;
        }
        public double ratio() { return estEnemyStrength / Math.max(1.0, ownStrength); }
    }

    // committed = total ever fed in; engaged = still alive on the field; reserve = untouched.
    private int impEngaged, rebEngaged;
    private int impCommitted, rebCommitted;
    private int impReserve, rebReserve;
    private int impReinfLeft = 2, rebReinfLeft = 2;
    private final double impMul, rebMul;

    private int window = 0;
    private final int maxWindows = (int) Battle.DECISION_WINDOWS.value;
    private boolean over = false;
    private Side winner = null;
    private boolean annihilation = false;

    public final BattleReport report = new BattleReport();

    public FieldBattle(String title, int impCommit, int impReserve, double impMul,
                       int rebCommit, int rebReserve, double rebMul) {
        this.report.title = title;
        this.impEngaged = impCommit; this.impCommitted = impCommit; this.impReserve = impReserve; this.impMul = impMul;
        this.rebEngaged = rebCommit; this.rebCommitted = rebCommit; this.rebReserve = rebReserve; this.rebMul = rebMul;
        report.log.add(impCommit + " imperials meet " + rebCommit + " rebels on " + title + ".");
    }

    public boolean isOver()        { return over; }
    public int window()            { return window; }
    public int maxWindows()        { return maxWindows; }
    public int impEngaged()        { return impEngaged; }
    public int rebEngaged()        { return rebEngaged; }
    public int impReinforcements() { return impReinfLeft; }
    public int rebReinforcements() { return rebReinfLeft; }

    private double strength(Side s) {
        return (s == Side.IMPERIAL ? impEngaged * impMul : rebEngaged * rebMul);
    }

    /** A commander's drifted read of the field. */
    public Assess assess(Side me) {
        double own = strength(me);
        double enemyTrue = strength(me == Side.IMPERIAL ? Side.REBEL : Side.IMPERIAL);
        double drift = Battle.ASSESS_DRIFT.value;
        double est = enemyTrue * (1.0 + (Math.random() * 2 - 1) * drift);
        int reinf = (me == Side.IMPERIAL) ? impReinfLeft : rebReinfLeft;
        return new Assess(own, Math.max(0, est), reinf, maxWindows - window);
    }

    /**
     * Advance one window. The imperial decision is supplied (player or Castius);
     * the rebel decision is taken by the AI here. Then both trade blows and the
     * end conditions are checked.
     */
    public void step(BattleDecision imperialDecision) {
        if (over) return;
        BattleDecision rebelDecision = AutoCommander.decide(assess(Side.REBEL));

        applyDecision(Side.IMPERIAL, imperialDecision);
        applyDecision(Side.REBEL, rebelDecision);

        // Someone called the retreat -> they break off, the other holds the field.
        if (imperialDecision == BattleDecision.RETREAT || rebelDecision == BattleDecision.RETREAT) {
            // If both retreat, the stronger remnant is judged to hold.
            if (imperialDecision == BattleDecision.RETREAT && rebelDecision == BattleDecision.RETREAT) {
                winner = strength(Side.IMPERIAL) >= strength(Side.REBEL) ? Side.IMPERIAL : Side.REBEL;
            } else {
                winner = (imperialDecision == BattleDecision.RETREAT) ? Side.REBEL : Side.IMPERIAL;
            }
            report.log.add((winner == Side.IMPERIAL ? "The rebels" : "The imperials") + " break and pull back.");
            end();
            return;
        }

        clash();
        window++;

        if (impEngaged <= 0 || rebEngaged <= 0) {
            annihilation = true;
            winner = impEngaged > 0 ? Side.IMPERIAL
                    : rebEngaged > 0 ? Side.REBEL
                    : (impCommitted >= rebCommitted ? Side.IMPERIAL : Side.REBEL);
            report.log.add("The field is annihilation. " + (winner == Side.IMPERIAL ? "Imperial" : "Rebel") + " line stands.");
            end();
            return;
        }
        if (window >= maxWindows) {
            winner = strength(Side.IMPERIAL) >= strength(Side.REBEL) ? Side.IMPERIAL : Side.REBEL;
            report.log.add("Both lines are spent; the weaker withdraws under cover of dark.");
            end();
        }
    }

    private void applyDecision(Side s, BattleDecision d) {
        int waves = (d == BattleDecision.REINFORCE_ONE) ? 1 : (d == BattleDecision.REINFORCE_BOTH) ? 2 : 0;
        for (int i = 0; i < waves; i++) reinforce(s);
    }

    private void reinforce(Side s) {
        boolean imp = (s == Side.IMPERIAL);
        int left = imp ? impReinfLeft : rebReinfLeft;
        if (left <= 0) return;
        int reserve = imp ? impReserve : rebReserve;
        if (reserve <= 0) { if (imp) impReinfLeft--; else rebReinfLeft--; return; }
        double pct = Battle.REINFORCE_MIN_PCT.value
                + Math.random() * (Battle.REINFORCE_MAX_PCT.value - Battle.REINFORCE_MIN_PCT.value);
        int wave = Math.max(1, (int) (reserve * pct));
        wave = Math.min(wave, reserve);
        if (imp) { impEngaged += wave; impCommitted += wave; impReserve -= wave; impReinfLeft--; }
        else     { rebEngaged += wave; rebCommitted += wave; rebReserve -= wave; rebReinfLeft--; }
        report.log.add((imp ? "Imperial" : "Rebel") + " reserves charge in (+" + wave + ").");
    }

    private void clash() {
        double impStr = strength(Side.IMPERIAL);
        double rebStr = strength(Side.REBEL);
        double lethal = Battle.ROUND_LETHALITY.value;
        // Each side's losses scale with the enemy's strength, eased by its own multiplier.
        int impLoss = (int) Math.round(rebStr * lethal / Math.max(0.5, impMul));
        int rebLoss = (int) Math.round(impStr * lethal / Math.max(0.5, rebMul));
        impLoss = Math.min(impLoss, impEngaged);
        rebLoss = Math.min(rebLoss, rebEngaged);
        impEngaged -= impLoss;
        rebEngaged -= rebLoss;
        report.log.add("Clash: imperials -" + impLoss + ", rebels -" + rebLoss + ".");
    }

    private void end() {
        over = true;
        report.winner = winner;
        report.annihilation = annihilation;
        report.impCommitted = impCommitted;
        report.rebCommitted = rebCommitted;
        report.impSurvivors = Math.max(0, impEngaged);
        report.rebSurvivors = Math.max(0, rebEngaged);
    }

    /** The Castius / Joric brain: read the field (with drift), then commit. */
    public static final class AutoCommander {
        private AutoCommander() {}
        public static BattleDecision decide(Assess a) {
            double ratio = a.ratio();
            if (ratio > 1.15) {                       // the enemy looks stronger
                if (a.reinforcementsLeft >= 2 && Math.random() < 0.5) return BattleDecision.REINFORCE_BOTH;
                if (a.reinforcementsLeft >= 1)                        return BattleDecision.REINFORCE_ONE;
                return ratio > 1.40 ? BattleDecision.RETREAT : BattleDecision.HOLD;
            }
            if (ratio < 0.85) {                        // the enemy looks weaker
                if (a.reinforcementsLeft >= 1 && Math.random() < 0.60) return BattleDecision.REINFORCE_ONE;
                return BattleDecision.HOLD;
            }
            return BattleDecision.HOLD;                // an even field: hold and grind
        }
    }
}