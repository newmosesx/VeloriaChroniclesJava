package example.practice.logger;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

public class Logger {
    public static final int MAX_LOG_ENTRIES = 100;

    public enum LogCategory {
        STORY,
        MILITARY,
        NATURAL,
        POLITICAL
    }

    private static class LogEntry {
        String message;
        long timestamp;
        LogEntry(String m) {
            this.message = m;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // A map holding a separate log list for each category
    private static final Map<LogCategory, LinkedList<LogEntry>> categorizedLogs = new EnumMap<>(LogCategory.class);
    private static final Map<LogCategory, Integer> readIndices = new EnumMap<>(LogCategory.class);

    static {
        for (LogCategory cat : LogCategory.values()) {
            categorizedLogs.put(cat, new LinkedList<>());
            readIndices.put(cat, 0);
        }
    }

    public static synchronized void logEvent(String message, LogCategory category) {
        LinkedList<LogEntry> logs = categorizedLogs.get(category);
        logs.addLast(new LogEntry(message));

        if (logs.size() > MAX_LOG_ENTRIES) {
            logs.removeFirst();
            int currentReadIdx = readIndices.get(category);
            if (currentReadIdx > 0) readIndices.put(category, currentReadIdx - 1);
        }
    }

    public static synchronized void pruneLogs() {
        long now = System.currentTimeMillis();
        for (LogCategory cat : LogCategory.values()) {
            LinkedList<LogEntry> logs = categorizedLogs.get(cat);
            int initialSize = logs.size();
            logs.removeIf(entry -> (now - entry.timestamp) > 300000); // 5 minutes
            int removed = initialSize - logs.size();
            readIndices.put(cat, Math.max(0, readIndices.get(cat) - removed));
        }
    }

    public static synchronized String getNewLogs(LogCategory category) {
        LinkedList<LogEntry> logs = categorizedLogs.get(category);
        int readIndex = readIndices.get(category);

        if (readIndex >= logs.size()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = readIndex; i < logs.size(); i++) {
            sb.append(logs.get(i).message).append("\n");
        }
        readIndices.put(category, logs.size());
        return sb.toString();
    }

    public static synchronized boolean hasUpdates(LogCategory category) {
        return readIndices.get(category) < categorizedLogs.get(category).size();
    }

    public static synchronized String peek(LogCategory category) {
        LinkedList<LogEntry> logs = categorizedLogs.get(category);
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : logs) sb.append(e.message).append("\n");
        return sb.toString();
    }
}