package example.practice.config;

public enum DivineIntervention {
    AI_DIVINE_INTERVENTION_TREASURY_THRESHOLD(3000),
    PENALTY_DURATION_DAYS(10);

    public final int value;

    DivineIntervention(int value){
        this.value = value;
    }
}


