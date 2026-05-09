package example.practice.config;

public enum RecruitmentCost {
    COSTSWORDSMAN(10), // metal
    COSTARCHER(10), // wood
    COSTCAVALRY(15), // metal
    COSTCAVALRYFOOD(10);

    public final int value;

    RecruitmentCost(int value){
        this.value = value;
    }
}

