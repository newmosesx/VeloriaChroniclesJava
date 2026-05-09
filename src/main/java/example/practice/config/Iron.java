package example.practice.config;

public enum Iron {
    IRONHALMET(2),
    IRONCHESTPLATE(2),
    IRONPANTS(2),
    IRONBOOTS(2);

    public final int value;

    Iron(int value){
        this.value = value;
    }
}

