package example.practice.config;

public enum AIGovernor {
    AI_ACTION_THRESHOLD(0.4f),
    AI_CRITICAL_FOOD_DAYS_THRESHOLD(2),
    AI_ARMY_SIZE_GOAL_PERCENT(0.08f),
    AI_FESTIVAL_COST(500),
    AI_FESTIVAL_UNREST_REDUCTION(45),
    AI_FARMER_CONVERSION_COUNT(50),
    AI_FOOD_DAYS_THRESHOLD(1),
    AI_STABILITY_ACTION_THRESHOLD(0.1f);

    public final float value;

    AIGovernor(float value){
        this.value = value;
    }
}

