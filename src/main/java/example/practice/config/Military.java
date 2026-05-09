package example.practice.config;

public enum Military {
    SWORDSMAN(6),
    ARCHER(7),
    CAVALRY(8),
    REBEL(9);

    public final int value;

    Military(int value){
        this.value = value;
    }
}

