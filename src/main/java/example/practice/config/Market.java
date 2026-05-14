package example.practice.config;

public enum Market {
    // Iron
    IRON_HELMET(2),
    IRON_CHESTPLATE(2),
    IRON_PANTS(2),
    IRON_BOOTS(2),
    // Steel
    STEEL_HELMET(3),
    // Royalty
    GOLD_CROWN(10);

    public final int defenseValue;
    Market(int defenseValue) { this.defenseValue = defenseValue; }
}