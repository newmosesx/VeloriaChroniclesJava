package example.practice.config;

public enum InterventionSustenance {
    DI_COST_SUSTENANCE(1500),
    DI_SUSTENANCE_FOOD_AMOUNT(1000),
    DI_PENALTY_PRODUCTION_MODIFIER(0.80f);

    public final float value;

    InterventionSustenance(float value){
        this.value = value;
    }
}

