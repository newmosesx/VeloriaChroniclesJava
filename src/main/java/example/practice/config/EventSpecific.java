package example.practice.config;

public enum EventSpecific {
    HARVEST_BASE_FOOD_GAIN(500),
    HARVEST_POPULATION_FOOD_MULTIPLIER(0.5f),
    GOLD_DISCOVERY_BRONZE_BONUS(150),
    PLAGUE_UNREST_GAIN(8),
    DROUGHT_UNREST_GAIN(10),
    BARBARIAN_RAID_RESOURCE_LOSS_PERCENT(0.8f),
    BARBARIAN_RAID_UNREST_GAIN(5),
    POLITICAL_INTRIGUE_UNREST_GAIN(2);

    public final float value;

    EventSpecific(float value){
        this.value = value;
    }
}

