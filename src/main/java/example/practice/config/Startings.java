package example.practice.config;

public enum Startings {
    STARTING_ZERO(0),
    STARTING_ONE(1),
    STARTING_TWO(2),
    STARTING_THREE(3),
    STARTING_SIX(6),
    STARTING_TWELVE(12),
    STARTING_FORTHEEN(14),
    STARTING_TWENTY(20),
    STARTING_TWENTY_TWO(22);

    public final int value;


    Startings(int value){
        this.value = value;
    }
}

