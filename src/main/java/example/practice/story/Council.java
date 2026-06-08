package example.practice.story;

import example.practice.config.CouncilArchetype;

import java.util.ArrayList;
import java.util.List;

// A roster of members plus the bridge from micro (relationships) to macro
// (kingdom unrest). This is the seam your roadmap #5 wants: small-scale
// reactions feeding the high-level stats.
public class Council {
    public final List<CouncilMember> members = new ArrayList<>();

    public static Council imperialCouncil() {
        Council c = new Council();
        c.members.add(new CouncilMember("General Castius",  "The Hawk",       CouncilArchetype.HAWK,       20, 30, 45, 35));
        c.members.add(new CouncilMember("Magister Vela",    "The Pragmatist", CouncilArchetype.PRAGMATIST, 15, 25, 50, 50));
        c.members.add(new CouncilMember("High Cleric Orin", "The Zealot",     CouncilArchetype.ZEALOT,     25, 40, 40, 30));
        return c;
    }

    public void applyToAll(DialogueToken token) {
        for (CouncilMember m : members) m.apply(token);
    }

    // How much the room's current mood should push kingdom unrest.
    public int aggregateUnrestSwing() {
        int swing = 0;
        for (CouncilMember m : members) {
            switch (m.disposition()) {
                case HOSTILE: swing += 120; break;
                case WARY:    swing += 40;  break;
                case NEUTRAL: swing += 0;   break;
                case WARM:    swing -= 30;  break;
                case LOYAL:   swing -= 60;  break;
            }
        }
        return swing;
    }

    public int countOf(Disposition d) {
        int n = 0;
        for (CouncilMember m : members) if (m.disposition() == d) n++;
        return n;
    }
}