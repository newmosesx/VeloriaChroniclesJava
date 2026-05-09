package example.practice.config;

public enum InitialKingdomResources {
    INITIAL_EMPIRE_FOOD(800000),
    INITIAL_EMPIRE_WOOD(1000),
    INITIAL_EMPIRE_STONE(1000),
    INITIAL_EMPIRE_METAL(500),
    INITIAL_EMPIRE_TREASURY(1000),
    INITIAL_EMPIRE_MORALE(60),
    INITIAL_SUCCESSOR_FOOD(500),
    INITIAL_SUCCESSOR_WOOD(100),
    INITIAL_SUCCESSOR_TREASURY(1000),
    INITIAL_SUCCESSOR_MORALE(75);

    public final int value;

    InitialKingdomResources(int value){
        this.value = value;
    }
}

