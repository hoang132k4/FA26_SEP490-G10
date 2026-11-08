package com.jlpp.modules.notification.service.impl;

import com.jlpp.modules.notification.entity.Notification;
import com.jlpp.modules.notification.repository.NotificationRepository;
import com.jlpp.modules.notification.service.NotificationService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findAllRecentNotifications();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countAllUnread();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead();
    }

    @Override
    @Transactional
    public void resetUnreadForDemo() {
        List<Notification> list = notificationRepository.findAllRecentNotifications();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            Notification n = list.get(i);
            n.setRead(false);
            notificationRepository.save(n);
        }
    }

    @Override
    @Transactional
    public void initSampleNotificationsIfEmpty() {
        long currentCount = notificationRepository.count();
        log.info("Checking notifications table. Current count = {}", currentCount);
        if (currentCount >= 3) {
            return;
        }

        User defaultLearner = userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn").orElse(null);

        List<Notification> samples = List.of(
                Notification.builder()
                        .user(defaultLearner)
                        .title("Nhắc nhở ôn tập 25 từ vựng JLPT hôm nay")
                        .content("Bộ thẻ 'Từ vựng N5 Minna bài 1-5' đã đến lịch lặp lại ngắt quãng (SRS). Vào ôn ngay để không bị quên nhé!")
                        .type("FLASHCARD")
                        .icon("bi-card-text")
                        .iconColor("text-danger")
                        .targetUrl("/flashcards")
                        .read(false)
                        .build(),

                Notification.builder()
                        .user(defaultLearner)
                        .title("Đề thi thử JLPT N5 & N4 mới nhất đã mở")
                        .content("Phòng thi trực tuyến đã cập nhật đề thi chuẩn cấu trúc JLPT mới. Có tính giờ và chấm điểm tự động.")
                        .type("EXAM")
                        .icon("bi-award")
                        .iconColor("text-primary")
                        .targetUrl("/exams")
                        .read(false)
                        .build(),

                Notification.builder()
                        .user(null)
                        .title("Chào mừng bạn gia nhập nền tảng JEPS! 🌸")
                        .content("Chúc mừng bạn bắt đầu hành trình chinh phục tiếng Nhật. Hãy làm bài kiểm tra đầu vào để nhận lộ trình cá nhân.")
                        .type("SYSTEM")
                        .icon("bi-stars")
                        .iconColor("text-warning")
                        .targetUrl("/exams")
                        .read(false)
                        .build(),

                Notification.builder()
                        .user(null)
                        .title("Bài học mới: Bảng chữ cái Hiragana & Katakana")
                        .content("Bài học đầu tiên trong khóa học Nhập môn JLPT đã sẵn sàng. Dành 15 phút mỗi ngày để hoàn thành nhé.")
                        .type("COURSE")
                        .icon("bi-journal-bookmark")
                        .iconColor("text-success")
                        .targetUrl("/courses")
                        .read(true)
                        .build(),

                Notification.builder()
                        .user(null)
                        .title("Lộ trình học tập cá nhân đã được tối ưu")
                        .content("Hệ thống đã phân tích mục tiêu JLPT N4 của bạn và lập kế hoạch từng chặng kiến thức chi tiết.")
                        .type("REMINDER")
                        .icon("bi-diagram-3")
                        .iconColor("text-info")
                        .targetUrl("/learning-path")
                        .read(true)
                        .build()
        );

        notificationRepository.saveAll(samples);
        log.info("Successfully seeded {} sample notifications into database.", samples.size());
    }
}
