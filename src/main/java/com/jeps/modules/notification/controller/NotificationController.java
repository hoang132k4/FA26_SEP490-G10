package com.jeps.modules.notification.controller;

import com.jeps.modules.notification.entity.Notification;
import com.jeps.modules.notification.service.NotificationService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
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
        return null;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Principal principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.ok(Map.of(
                    "notifications", List.of(),
                    "unreadCount", 0L
            ));
        }
        List<Notification> list = notificationService.getRecentNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of(
                "notifications", list,
                "unreadCount", unreadCount
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, Principal principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markAsRead(id);
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", unreadCount));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", 0L));
    }

    @PostMapping("/reset-demo")
    public ResponseEntity<Map<String, Object>> resetDemo() {
        notificationService.resetUnreadForDemo();
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", 3));
    }
}

