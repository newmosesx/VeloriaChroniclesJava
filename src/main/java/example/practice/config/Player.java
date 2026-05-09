package example.practice.config;

public enum Player {
    PLAYER_FESTIVAL_COST(150000),
    PLAYER_FESTIVAL_UNREST_REDUCTION(50),
    PLAYER_HEALTH(200);

    public final float value;

    Player(float value){
        this.value = value;
    }
}


