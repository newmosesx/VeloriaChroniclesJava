package example.practice.config;

public enum Morale {
    MORALE_FOOD_SURPLUS_MULTIPLIER(2),
    MORALE_GAIN_FROM_SURPLUS(1),
    MORALE_LOSS_FROM_UNREST(1),
    MINIMUM_MORALE_FOR_UNREST_LOSS(0.35f);

    public final float value;

    Morale(float value){
        this.value = value;
    }
}

