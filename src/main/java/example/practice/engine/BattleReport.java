package example.practice.engine;

import java.util.ArrayList;
import java.util.List;

/** The record of one resolved field battle -- enough to apply losses and play the cinematic. */
public final class BattleReport {
    public String title = "The Field";
    public int impCommitted, rebCommitted;   // total men each side fed into the fight
    public int impSurvivors, rebSurvivors;    // still standing at the end
    public FieldBattle.Side winner;           // null only if a true draw
    public boolean annihilation;              // a side was wiped out (vs a retreat)
    public boolean imperialLeaderDown;        // Castius fell
    public boolean rebelLeaderDown;           // Joric fell
    public final List<String> log = new ArrayList<>();

    public int impLosses() { return Math.max(0, impCommitted - impSurvivors); }
    public int rebLosses() { return Math.max(0, rebCommitted - rebSurvivors); }
    public boolean empireWon() { return winner == FieldBattle.Side.IMPERIAL; }
}