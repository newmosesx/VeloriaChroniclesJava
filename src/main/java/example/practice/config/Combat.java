package example.practice.config;

public enum Combat {
    MILITARY_EXTRA_FOOD_CONSUMPTION(1),
    GENERAL_SPAWN_CHANCE_PERCENT(2),
    INITIAL_GENERAL_LIMIT(3),
    GENERAL_COMBAT_BONUS(1.15f),
    REBEL_LEADER_COMBAT_BONUS(1.25f),
    REBEL_BASE_STRENGTH_MODIFIER(1.1f),
    ELEMENT_OF_SURPRISE(1.05f),
    ORGANIZED_COMMAND(1.1f);

    public final float value;

    Combat(float value){
        this.value = value;
    }
}


