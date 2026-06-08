package example.practice.agents;

import java.util.ArrayList;
import java.util.List;

// The cast. Adding a character is one register(...) line - that is the entire
// extension story. tick() climbs everyone's renown each day (adventurers fastest),
// growing them into power over time.
public class AgentRoster {
    public final List<Agent> agents = new ArrayList<>();

    public Agent register(Agent a) { agents.add(a); return a; }

    public Agent get(String name) {
        for (Agent a : agents) if (a.name.equalsIgnoreCase(name)) return a;
        return null;
    }
    public List<Agent> living() {
        List<Agent> r = new ArrayList<>();
        for (Agent a : agents) if (a.alive) r.add(a);
        return r;
    }
    public List<Agent> by(AgentAllegiance al) {
        List<Agent> r = new ArrayList<>();
        for (Agent a : agents) if (a.allegiance == al) r.add(a);
        return r;
    }

    public void tick() {
        for (Agent a : agents) {
            if (!a.alive) continue;
            float gain = (a.allegiance == AgentAllegiance.INDEPENDENT) ? 2.0f : 0.3f;
            a.gainRenown(gain);
        }
    }

    // Starter cast - edit freely, add as the story grows. Disposition is (tension,
    // suspicion, trust, likeness), 0..100.
    public static AgentRoster seedDefault() {
        AgentRoster r = new AgentRoster();

        // Institutional figures - they sway a power bloc.
        r.register(Agent.of("General Castius", "The Hawk", AgentAllegiance.ARMY, AgentTwist.WARLORD, 5)
                .disposition(20, 30, 45, 35));
        r.register(Agent.of("Mara Voss", "The Whisper", AgentAllegiance.COUNCIL, AgentTwist.SHADOW, 4)
                .disposition(30, 45, 30, 25));
        r.register(Agent.of("The Speaker", "The Unmoved", AgentAllegiance.CROWN, AgentTwist.IMMOVABLE, 2)
                .disposition(0, 0, 50, 50).locked()); // never moves, whatever you say

        // The Jokers - an adventurer band. No authority over anyone, but they climb fast.
        r.register(Agent.of("Joric", "The Stray", AgentAllegiance.INDEPENDENT, AgentTwist.WILDCARD, 3)
                .disposition(35, 35, 40, 45));
        r.register(Agent.of("Kaelen Duskbane", "The Blade", AgentAllegiance.INDEPENDENT, AgentTwist.DUELIST, 3)
                .disposition(25, 30, 40, 40));

        return r;
    }
}