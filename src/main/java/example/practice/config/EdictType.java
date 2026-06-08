package example.practice.config;

// Standing policies. dailyCost is gold drained every day the edict is active - so
// governance is daily income versus what you're choosing to sustain. Each one eases
// one pressure and usually angers another; nothing here is free.
public enum EdictType {
    GRAIN_DOLE("Grain dole", 40, "Open the granary to the hungry. Calms the Commons; drains your stores."),
    PUBLIC_FESTIVAL("Public festival", 25, "Bread and circuses. Eases anger across the realm a little."),
    MARTIAL_LAW("Martial law", 50, "Put down unrest by force. Rebels stand down; fear hardens Commons and Nobility."),
    EMERGENCY_CONSCRIPTION("Emergency conscription", 35, "Lift the levy ceiling to rebuild the army fast. The Commons resent the draft."),
    TEMPLE_PATRONAGE("Temple patronage", 30, "Fund the temples. Eases the Clergy."),
    LAND_REFORM("Land reform", 45, "Break up the great estates. Calms the Commons; the Nobility turn on you."),
    TAX_RELIEF("Tax relief", 55, "Ease the burden. Calms Commons and Merchants; heavy on the treasury.");

    public final String title;
    public final int dailyCost;
    public final String description;
    EdictType(String title, int dailyCost, String description){
        this.title = title; this.dailyCost = dailyCost; this.description = description;
    }
}