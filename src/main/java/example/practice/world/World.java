package example.practice.world;

import java.util.ArrayList;
import java.util.List;

// The living world: a static board (Geography) plus the ticked systems, advanced
// every day in dependency order. Calendar sets the clock, Climate reads it for
// weather, Water reads the weather for rivers, Agriculture reads weather + water
// for the harvest. Each system derives its state from the ones before it.
public class World {

    public final Geography geography;
    public final Calendar calendar;
    public final Climate climate;
    public final Water water;
    public final Agriculture agriculture;

    private final List<WorldSystem> systems = new ArrayList<>();

    public World() {
        geography = new Geography();
        calendar = new Calendar();
        climate = new Climate(geography);
        water = new Water(geography);          // after climate (needs rain), before agriculture
        agriculture = new Agriculture(geography);

        systems.add(calendar);
        systems.add(climate);
        systems.add(water);
        systems.add(agriculture);
    }

    public void advanceDay() {
        for (WorldSystem s : systems) s.advanceDay(this);
    }

    public String environmentReport() {
        StringBuilder sb = new StringBuilder();
        for (WorldSystem s : systems) sb.append(s.reportLine()).append("\n");
        return sb.toString();
    }
}