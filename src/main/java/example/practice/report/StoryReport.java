package example.practice.report;

public class StoryReport {
    public int chapter;
    public int paragraph;
    public String label;            // "Chapter 9, paragraph 18"
    public boolean choicePending;   // is a debate choice waiting?
    public String lastChoice;       // DebateManager.chosenResponse
}