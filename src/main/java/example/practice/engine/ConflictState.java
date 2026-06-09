package example.practice.engine;

/**
 * The conflict posture of a kingdom. Nothing fights unless the state allows it,
 * which is what stops soldiers and rebels grinding each other down at peace.
 *
 *  PEACE      - no organic battles; army garrisons/drills; rebels don't form.
 *  TENSION    - unrest rising; rebels organize underground; still no battles.
 *  INSURGENCY - open revolt in contested sectors; throttled guerrilla battles.
 *  CIVIL_WAR  - full scale; more frequent, decisive battles; sectors change hands.
 *  RESOLUTION - suppressed or overthrown; decays back toward PEACE.
 */
public enum ConflictState {
    PEACE, TENSION, INSURGENCY, CIVIL_WAR, RESOLUTION
}