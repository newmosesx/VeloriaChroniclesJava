package example.practice.config;

public enum Teritium {
    TERITIUMHALMET(4),
    TERITIUMCHESTPLATE(4),
    TERITIUMPANTS(4),
    TERITIUMBOOTS(4);

    public final int value;

    Teritium(int value){
        this.value = value;
    }
}
