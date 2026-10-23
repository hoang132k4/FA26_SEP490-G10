package com.jlpp.modules.systemlog.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

import com.jlpp.modules.systemlog.model.SystemLogEntry;

@Service
public class SystemLogService {

    private static final int MAX_ENTRIES = 500;

    private final ConcurrentLinkedDeque<SystemLogEntry> entries = new ConcurrentLinkedDeque<>();

    public SystemLogService() {
        record("System", "SystemLogService", "Bộ giám sát nhật ký trong bộ nhớ đã sẵn sàng.");
    }

    public void record(String actor, String source, String message) {
        entries.addFirst(new SystemLogEntry(LocalDateTime.now(), "INFO", source, actor, message));
        while (entries.size() > MAX_ENTRIES) {
            entries.pollLast();
        }
    }

    public List<SystemLogEntry> findRecent() {
        return List.copyOf(new ArrayList<>(entries));
    }

    public long count() {
        return entries.size();
    }
}
