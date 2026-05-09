package example.practice.config;

public enum RandomEvents {
    DAILY_RANDOM_EVENT_CHANCE_PERCENT(100),
    TOTAL_RANDOM_EVENTS(0.75f),
    PLAGUE_POPULATION_LOSS_PERCENT(0.1f),
    BARBARIAN_POPULATION_LOSS_PERCENT(0.02f),
    FAMINE_POPULATION_LOSS_PERCEN(5);

    public final float value;

    RandomEvents(float value){
        this.value = value;
    }
}

