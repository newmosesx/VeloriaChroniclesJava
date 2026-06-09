package example.practice.engine;

/**
 * One command a leader gives at a decision window during a real battle.
 *  HOLD           - press the attack with what's on the field.
 *  RETREAT        - break off; survivors pull back (this side loses the field).
 *  REINFORCE_ONE  - feed one reserve wave (5-10% of the army not yet engaged).
 *  REINFORCE_BOTH - commit both remaining waves at once (the all-in gamble).
 */
public enum BattleDecision {
    HOLD, RETREAT, REINFORCE_ONE, REINFORCE_BOTH
}