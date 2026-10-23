package com.jlpp.modules.systemlog.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record SystemLogEntry(LocalDateTime occurredAt, String level, String source, String actor, String message) {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String getTimestamp() {
        return occurredAt.format(DISPLAY_FORMAT);
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getLevel() {
        return level;
    }

    public String getSource() {
        return source;
    }

    public String getActor() {
        return actor;
    }

    public String getMessage() {
        return message;
    }
}
