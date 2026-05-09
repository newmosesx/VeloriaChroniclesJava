package example.practice.shared;

import example.practice.config.Population;
import example.practice.kingdoms.Kingdom;

public class ShareData {
    public String worldPopulation;
    public String currentHour;
    public String CivilWarStatus;

    public int numkingdoms = (int)Population.NUMKINGDOMS.value;
    Kingdom[] kingdoms = new Kingdom[numkingdoms];

    int currentStoryChapter;
    int currentStoryParagraph;

}
