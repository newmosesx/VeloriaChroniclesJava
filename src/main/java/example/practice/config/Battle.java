package example.practice.config;

public enum Battle {
    HOURLY_SKIRMISH_BASE_CHANCE_PERCENT(50),
    MORALE_LOSS_FROM_SKIRMISH(1),
    MORALE_GAIN_ON_VICTORY(5),
    MORALE_LOSS_ON_DEFEAT(10),
    REBELS_IN_SKIRMISH(0.35f),
    SOLDIERS_IN_SKIRMISH(0.25f);

    public final float value;

    Battle(float value){
        this.value = value;
    }
}

