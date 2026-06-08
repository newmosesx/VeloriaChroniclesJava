package example.practice.report;

import example.practice.engine.SimulationEngine;

// How to safely obtain a report relative to the simulation thread.
public class ReportService {

    // For the GUI / any outside thread: locks the engine so the snapshot is
    // consistent (no torn reads while the sim thread mutates the world).
    public static WorldReport snapshot(SimulationEngine engine) {
        engine.lock();
        try {
            return WorldReport.from(engine);
        } finally {
            engine.unlock();
        }
    }

    // For code that ALREADY holds the lock - e.g. the World Director running
    // inside processDay. (The engine's lock is reentrant, so snapshot() would
    // also work there, but this makes the intent explicit.)
    public static WorldReport snapshotUnlocked(SimulationEngine engine) {
        return WorldReport.from(engine);
    }
}