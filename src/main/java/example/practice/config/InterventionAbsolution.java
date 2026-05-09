package example.practice.config;

public enum InterventionAbsolution {
    DI_COST_ABSOLUTION(2500),
    DI_ABSOLUTION_UNREST_REDUCTION(20),
    DI_ABSOLUTION_UNREST_THRESHOLD(300),
    DI_PENALTY_MORALE_DROP(10);

    public final int value;

    InterventionAbsolution(int value){
        this.value = value;
    }
}
