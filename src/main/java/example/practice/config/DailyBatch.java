package example.practice.config;

public enum DailyBatch {
    BATCHES_PER_DAY(3);

    public final int value;
    
    DailyBatch(int value){
        this.value = value;
    }
}


