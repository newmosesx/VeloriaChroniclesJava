package example.practice.engine;

import java.time.LocalDateTime;

public class TimeSpace {

    // Mimics get_sys_hour()
    public static int getSysHour() {
        return LocalDateTime.now().getHour();
    }

    // Mimics get_sys_day()
    public static int getSysDay() {
        return LocalDateTime.now().getDayOfMonth();
    }

    // Mimics get_sys_min()
    public static int getSysMin() {
        return LocalDateTime.now().getMinute();
    }
}