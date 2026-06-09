package example.practice.engine;
import java.util.List;

public class DebateEventData {
    public int chapter;
    public int paragraph;
    public String timeoutResponse;
    public String timeoutLog;
    public int timeoutUnrest;
    public int timeoutMorale;

    public List<DebateChoiceData> choices;
}