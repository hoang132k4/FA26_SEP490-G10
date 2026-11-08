package com.jlpp.modules.notification.controller;

import com.jlpp.modules.notification.entity.Notification;
import com.jlpp.modules.notification.service.NotificationService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private Long resolveUserId(Principal principal, HttpSession session) {
        if (principal != null && principal.getName() != null) {
            String identifier = principal.getName().trim();
            Optional<User> u = userRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> userRepository.findByUsername(identifier));
            if (u.isPresent()) return u.get().getId();
        }
        if (session != null && session.getAttribute("CURRENT_LEARNER_ID") != null) {
            return (Long) session.getAttribute("CURRENT_LEARNER_ID");
        }
        return 1L;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Principal principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        List<Notification> list = notificationService.getRecentNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of(
                "notifications", list,
                "unreadCount", unreadCount
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, Principal principal, HttpSession session) {
        notificationService.markAsRead(id);
        Long userId = resolveUserId(principal, session);
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", unreadCount));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", 0));
    }

    @PostMapping("/reset-demo")
    public ResponseEntity<Map<String, Object>> resetDemo() {
        notificationService.resetUnreadForDemo();
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", 3));
    }
}

