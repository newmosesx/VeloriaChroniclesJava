package example.practice.shared;

import example.practice.config.Population;
import example.practice.kingdoms.Kingdom;

public class ShareData {
    public String worldPopulation;
    public String currentHour;
    public String civilWarStatus;

    public int numKingdoms = (int) Population.NUMKINGDOMS.value;
    public Kingdom[] kingdoms = new Kingdom[numKingdoms];

    public int currentStoryChapter;
    public int currentStoryParagraph;
}