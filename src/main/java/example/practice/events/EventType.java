package example.practice.events;

// The events the world can surface. They aren't scripted - each one fires only
// when the condition it names is genuinely true in the simulation. The last two
// are CHAINED events: they feed on the scarcity left behind by an earlier disaster
// (see EventAftermath), so hardship compounds into a crisis instead of resetting.
public enum EventType {
    DROUGHT,
    FLOOD,
    PLAGUE,
    BARBARIAN_RAID,
    INTRIGUE,
    BOUNTIFUL_HARVEST,
    GOLD_STRIKE,
    BREAD_RIOT,       // chained: scarcity + hunger -> the Commons take the streets
    PRICE_GOUGING     // chained: scarcity -> Merchants profiteer, Commons resent it
}