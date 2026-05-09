package example.practice.config;

public enum LogTiming {
    LOGSECTIONSIZE(250),
    LOGCLEARFREQUENCY(5);

    public final int value;

    LogTiming(int value){
        this.value = value;
    }
}

