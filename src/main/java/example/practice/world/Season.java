package example.practice.world;

public enum Season {
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    WINTER("Winter");

    public final String label;
    Season(String label){ this.label = label; }
}