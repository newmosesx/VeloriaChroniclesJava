package example.practice.world;

// The contract every world system obeys. Stateful, ticked once per day in
// dependency order, reading its siblings through the World container.
public interface WorldSystem {

    // Advance this system one day, using other systems via the World container.
    void advanceDay(World world);

    // One line for the intelligence report.
    String reportLine();
}