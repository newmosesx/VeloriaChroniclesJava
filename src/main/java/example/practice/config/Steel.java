package example.practice.config;

public enum Steel {
    STEELHALMET(3),
    STEELCHESTPLATE(3),
    STEELPANTS(3),
    STEELBOOTS(3);

    public final int value;

    Steel(int value){
        this.value = value;
    }
}

