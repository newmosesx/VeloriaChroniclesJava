package example.practice.config;

public enum WarStatus {
    WARONGOING(0),
    EMPIREWINS(1),
    REBELSWIN(2);

    public final int value;

    WarStatus(int value){
        this.value = value;
    }
}

