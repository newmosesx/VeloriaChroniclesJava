package example.practice.engine;

/**
 * What the army is doing day to day. AI picks one automatically (Hybrid), but
 * the player can override per kingdom and the AI will respect it until released.
 *
 *  GARRISON - cheap, holds order, no campaigning.
 *  PATROL   - actively suppresses rebellion organization; light upkeep.
 *  DRILL    - raises readiness; no suppression.
 *  MOBILIZE - war footing; the only posture that actively presses skirmishes.
 *  ATTACK   - the offensive: hourly skirmishes stop and one REAL battle a day is
 *             fought (Castius vs Joric). Player-chosen only; the AI never auto-picks it.
 *
 * NOTE: ATTACK is appended last so saved posture ordinals stay stable.
 */
public enum MilitaryPosture {
    GARRISON, PATROL, DRILL, MOBILIZE, ATTACK
}