package example.practice.config;

public enum MaximumNumOfCharacters {
    MAXNUMCHAR(50);

    public final int value;

    MaximumNumOfCharacters(int value){
        this.value = value;
    }
}

