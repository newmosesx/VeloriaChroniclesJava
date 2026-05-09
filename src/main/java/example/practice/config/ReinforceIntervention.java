package example.practice.config;

public enum ReinforceIntervention {
    DI_COST_REINFORCEMENTS(2000),
    DI_REINFORCEMENTS_COUNT(20),
    DI_PENALTY_TAX_MODIFIER(0.75f);

    public final float value;

    ReinforceIntervention(float value){
        this.value = value;
    }
}
