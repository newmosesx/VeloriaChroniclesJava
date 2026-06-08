package example.practice.events;

// The events the world can surface. They aren't scripted - each one fires only
// when the condition it names is genuinely true in the simulation.
public enum EventType {
    DROUGHT,
    FLOOD,
    PLAGUE,
    BARBARIAN_RAID,
    INTRIGUE,
    BOUNTIFUL_HARVEST,
    GOLD_STRIKE
}