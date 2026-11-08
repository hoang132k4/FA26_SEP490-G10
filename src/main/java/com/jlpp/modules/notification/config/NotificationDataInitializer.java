package com.jlpp.modules.notification.config;

import com.jlpp.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class NotificationDataInitializer implements CommandLineRunner {

    private final NotificationService notificationService;

    @Override
    public void run(String... args) {
        try {
            notificationService.initSampleNotificationsIfEmpty();
        } catch (Exception ex) {
            log.warn("Could not seed initial notifications (table may be updating): {}", ex.getMessage());
        }
    }
}

