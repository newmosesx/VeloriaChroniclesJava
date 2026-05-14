package example.practice.engine;

public class DailyEventTracker {
    // Natural / Random Events
    public static boolean harvestTriggered = false;
    public static boolean goldTriggered = false;
    public static boolean plagueTriggered = false;
    public static boolean droughtTriggered = false;
    public static boolean barbarianTriggered = false;
    public static boolean intrigueTriggered = false;

    // Political / Governor Events
    public static boolean festivalTriggered = false;
    public static boolean recruitmentLogged = false;
    public static boolean farmerConversionLogged = false;

    // Economy Events
    public static boolean famineLogged = false;

    public static void resetDailyFlags() {
        harvestTriggered = false;
        goldTriggered = false;
        plagueTriggered = false;
        droughtTriggered = false;
        barbarianTriggered = false;
        intrigueTriggered = false;

        festivalTriggered = false;
        recruitmentLogged = false;
        farmerConversionLogged = false;

        famineLogged = false;
    }
}