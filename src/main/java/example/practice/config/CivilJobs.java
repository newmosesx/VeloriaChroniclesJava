package example.practice.config;

public enum CivilJobs {
    FARMER(1),
    BUTCHER(2),
    LUMBERJACK(3),
    MINER(4),
    BLACKSMITH(5);

    public final int value;

    CivilJobs(int value){
        this.value = value;
    }
}

