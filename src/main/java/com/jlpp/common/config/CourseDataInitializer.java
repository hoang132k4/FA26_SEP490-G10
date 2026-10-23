package com.jlpp.common.config;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseDataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        seedUsers();
        seedCourses();
    }

    private void seedUsers() {
        if (userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn").isEmpty()) {
            User learner = User.builder()
                    .email("learner@jeps.edu.vn")
                    .username("learner")
                    .fullName("Nguyễn Văn Học Viên")
                    .password("Password123@")
                    .role(RoleEnum.LEARNER)
                    .status("ACTIVE")
                    .phone("0987654321")
                    .currentJlptLevel("N5")
                    .targetJlptLevel("N4")
                    .build();
            userRepository.save(learner);
            log.info("Initialized default learner: learner@jeps.edu.vn");
        }
    }

    private void seedCourses() {
        if (courseRepository.count() > 0) {
            return;
        }

        List<Course> courses = new ArrayList<>();

        // 1. Course N5
        Course n5 = Course.builder()
                .title("Khóa học Nhập môn Tiếng Nhật & JLPT N5 Toàn diện")
                .slug("nhap-mon-tieng-nhat-jlpt-n5")
                .code("JLPT-N5")
                .level("N5")
                .shortDescription("Nắm vững bảng chữ cái Hiragana, Katakana, 100 chữ Hán căn bản và 80 mẫu ngữ pháp giao tiếp đời sống.")
                .fullDescription("Khóa học Nhập môn Tiếng Nhật & JLPT N5 Toàn diện được thiết kế độc quyền theo chuẩn khung năng lực JLPT Nhật Bản. Học viên sẽ được trang bị hệ thống phát âm chuẩn Tokyo từ bảng chữ cái Hiragana, Katakana, cho đến làm chủ 100 chữ Hán thường gặp nhất và tự tin vượt qua kỳ thi JLPT N5 với điểm số cao.")
                .thumbnailUrl("https://images.unsplash.com/photo-1503899036084-c55cdd92da26?q=80&w=800&auto=format&fit=crop")
                .price(299000.0)
                .discountPrice(199000.0)
                .durationHours(40)
                .lessonCount(30)
                .coveredSkills("Hiragana/Katakana, Chữ Hán, Từ vựng, Ngữ pháp, Nghe hiểu N5")
                .objectives("• Làm chủ hoàn toàn 2 bảng chữ cái và các quy tắc biến âm, trường âm, âm ngắt.\n• Nắm vững 100 chữ Kanji và 800 từ vựng sơ cấp căn bản.\n• Nắm vững các mẫu ngữ pháp trợ từ, thì quá khứ, thể Te, thể Nai.\n• Tự tin làm bài thi thử JLPT N5 đạt trên 120/180 điểm.")
                .targetAudience("Người mới bắt đầu học tiếng Nhật từ con số 0 hoặc muốn củng cố lại toàn bộ nền tảng sơ cấp.")
                .prerequisites("Không yêu cầu kiến thức nền tảng trước đó. Chỉ cần sự chăm chỉ và kiên trì mỗi ngày 30 phút.")
                .status("PUBLISHED")
                .featured(true)
                .enrollmentCount(1280)
                .rating(4.9)
                .build();
        addLessons(n5, List.of(
                new Object[]{"Bài 01: Làm quen Bảng chữ cái Hiragana & Phát âm chuẩn Tokyo", "Chương 1: Bảng chữ cái & Ngữ âm", 25, "VIDEO", true},
                new Object[]{"Bài 02: Bảng chữ cái Katakana & Các từ mượn phổ biến", "Chương 1: Bảng chữ cái & Ngữ âm", 25, "VIDEO", true},
                new Object[]{"Bài 03: Quy tắc Biến âm, Âm ghép, Trường âm và Âm ngắt", "Chương 1: Bảng chữ cái & Ngữ âm", 30, "VIDEO", false},
                new Object[]{"Bài 04: Giới thiệu bản thân & Trợ từ cơ bản wa, ga, no", "Chương 2: Ngữ pháp cốt lõi N5", 35, "VIDEO", false},
                new Object[]{"Bài 05: 20 Chữ Hán đầu tiên: Số đếm, Ngày tháng & Gia đình", "Chương 2: Ngữ pháp cốt lõi N5", 30, "PRACTICE", false},
                new Object[]{"Bài 06: Thể Te và các ứng dụng diễn đạt yêu cầu, nối câu", "Chương 3: Đột phá Ngữ pháp hành động", 40, "VIDEO", false},
                new Object[]{"Bài 07: Kỹ năng nghe hiểu đoạn hội thoại ngắn đời thường", "Chương 4: Luyện kỹ năng Đọc - Nghe", 35, "VIDEO", false},
                new Object[]{"Bài 08: Đề thi thử JLPT N5 Chuẩn format kỳ thi thật", "Chương 5: Thi thử & Đánh giá", 60, "QUIZ", false}
        ));
        courses.add(n5);

        // 2. Course N4
        Course n4 = Course.builder()
                .title("Luyện thi Sơ cấp Cấp tốc JLPT N4 Đột phá")
                .slug("luyen-thi-so-cap-jlpt-n4")
                .code("JLPT-N4")
                .level("N4")
                .shortDescription("Mở rộng 300 Hán tự thông dụng, hệ thống thể điều kiện, bị động, sai khiến và phản xạ nghe hiểu hội thoại.")
                .fullDescription("Khóa học N4 là giai đoạn bản lề quan trọng nhất để chuyển giao từ sơ cấp sang trung cấp. Bạn sẽ được củng cố toàn bộ các dạng thể động từ phức tạp (bị động, sai khiến, thể kính ngữ sơ cấp) cùng ngân hàng đề thi bám sát cấu trúc mới nhất.")
                .thumbnailUrl("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=800&auto=format&fit=crop")
                .price(399000.0)
                .discountPrice(299000.0)
                .durationHours(50)
                .lessonCount(35)
                .coveredSkills("300 Kanji N4, 1500 Từ vựng, Thể bị động, Sai khiến, Đọc hiểu N4")
                .objectives("• Làm chủ 300 Hán tự thông dụng và cách ghép từ thuần thục.\n• Thành thạo thể Bị động (Ukemi), Sai khiến (Shieki) và Kính ngữ cơ bản.\n• Tăng tốc độ đọc hiểu văn bản ngắn và vừa dưới áp lực thời gian.\n• Tự tin đạt trên 130/180 điểm thi JLPT N4.")
                .targetAudience("Học viên đã hoàn thành trình độ N5 hoặc đã học hết 25 bài Minna no Nihongo.")
                .prerequisites("Đã có kiến thức nền tảng tương đương JLPT N5.")
                .status("PUBLISHED")
                .featured(true)
                .enrollmentCount(950)
                .rating(4.9)
                .build();
        addLessons(n4, List.of(
                new Object[]{"Bài 01: Ôn tập N5 cấp tốc & Khởi động lộ trình N4", "Chương 1: Củng cố & Nâng cao sơ cấp", 25, "VIDEO", true},
                new Object[]{"Bài 02: Thể khả năng (Kanoukei) & Các mẫu câu biểu thị năng lực", "Chương 1: Củng cố & Nâng cao sơ cấp", 30, "VIDEO", true},
                new Object[]{"Bài 03: 50 Chữ Hán chủ đề Đời sống công sở & Giao thông", "Chương 2: Chữ Hán & Từ vựng N4", 35, "PRACTICE", false},
                new Object[]{"Bài 04: Thể Bị động (Ukemi) và Bị động phiền toái", "Chương 3: Ngữ pháp then chốt N4", 45, "VIDEO", false},
                new Object[]{"Bài 05: Thể Sai khiến (Shieki) & Kết hợp Bị động sai khiến", "Chương 3: Ngữ pháp then chốt N4", 40, "VIDEO", false},
                new Object[]{"Bài 06: Chiến lược xử lý bài Đọc hiểu dạng tìm thông tin và email", "Chương 4: Kỹ năng Đọc hiểu Dokkai", 35, "READING", false},
                new Object[]{"Bài 07: Kỹ thuật bắt từ khóa trong bài Nghe hiểu Choukai", "Chương 5: Kỹ năng Nghe hiểu Choukai", 35, "VIDEO", false},
                new Object[]{"Bài 08: Đề thi thử JLPT N4 Mô phỏng thời gian thực", "Chương 6: Thi thử & Chữa đề", 75, "QUIZ", false}
        ));
        courses.add(n4);

        // 3. Course N3
        Course n3 = Course.builder()
                .title("Chinh phục Đột phá Điểm số JLPT N3 Trung cấp")
                .slug("chinh-phuc-jlpt-n3-trung-cap")
                .code("JLPT-N3")
                .level("N3")
                .shortDescription("Bước ngoặt trung cấp với 650 Kanji, 150 mẫu ngữ pháp then chốt và chiến lược làm bài đọc hiểu phân tích.")
                .fullDescription("JLPT N3 là tấm vé vàng mở ra cơ hội việc làm tại các doanh nghiệp Nhật Bản. Khóa học tập trung giải quyết dứt điểm các bẫy ngữ pháp dễ nhầm lẫn, nâng cao năng lực đọc hiểu đoạn văn dài và phản xạ nghe hiểu tức thì.")
                .thumbnailUrl("https://images.unsplash.com/photo-1528164344705-475426879c0d?q=80&w=800&auto=format&fit=crop")
                .price(549000.0)
                .discountPrice(449000.0)
                .durationHours(65)
                .lessonCount(42)
                .coveredSkills("650 Kanji N3, Ngữ pháp trung cấp, Đọc hiểu phân tích, Nghe hiểu phản xạ")
                .objectives("• Thuộc lòng 650 Kanji và 3000 từ vựng trung cấp thường gặp.\n• Phân biệt rõ ràng các cặp ngữ pháp tương đồng dễ nhầm lẫn.\n• Đọc hiểu nhanh các bài xã luận, bài bình luận ngắn.\n• Đạt chuẩn điểm số JLPT N3 trên 115/180 điểm.")
                .targetAudience("Học viên đã đỗ N4 hoặc hoàn thành 50 bài Minna no Nihongo.")
                .prerequisites("Kiến thức tiếng Nhật sơ cấp vững chắc (JLPT N4).")
                .status("PUBLISHED")
                .featured(true)
                .enrollmentCount(820)
                .rating(4.8)
                .build();
        addLessons(n3, List.of(
                new Object[]{"Bài 01: Tổng quan cấu trúc đề thi N3 & Chiến lược phân bổ thời gian", "Chương 1: Tổng quan & Khởi động", 30, "VIDEO", true},
                new Object[]{"Bài 02: 80 Kanji N3 chủ đề Xã hội, Kinh tế & Môi trường", "Chương 2: Chữ Hán trung cấp", 40, "PRACTICE", false},
                new Object[]{"Bài 03: Ngữ pháp nhóm nguyên nhân - kết quả (wake da, koto da, mono da)", "Chương 3: Ngữ pháp chuyên sâu N3", 45, "VIDEO", false},
                new Object[]{"Bài 04: Đọc hiểu trung văn: Cách nắm bắt luận điểm của tác giả", "Chương 4: Đột phá Đọc hiểu", 40, "READING", false},
                new Object[]{"Bài 05: Nghe hiểu phản xạ nhanh trong hội thoại công việc", "Chương 5: Nghe hiểu Choukai", 35, "VIDEO", false},
                new Object[]{"Bài 06: Đề thi thử JLPT N3 Tiêu chuẩn Bộ Giáo dục Nhật", "Chương 6: Thi thử toàn diện", 90, "QUIZ", false}
        ));
        courses.add(n3);

        // 4. Course N2
        Course n2 = Course.builder()
                .title("Luyện đề Chuyên sâu & Kỹ năng Làm bài JLPT N2")
                .slug("luyen-de-chuyen-sau-jlpt-n2")
                .code("JLPT-N2")
                .level("N2")
                .shortDescription("Khóa học trang bị tư duy giải đề chuẩn, phản xạ nghe hiểu tốc độ cao và mở rộng vốn từ vựng thương mại.")
                .fullDescription("Khóa học hướng đến mục tiêu giúp học viên vượt qua kỳ thi JLPT N2 với điểm số xuất sắc. Chương trình giảng dạy tích hợp hệ thống bài tập thực chiến, phân tích chuyên sâu các bài đọc hiểu trừu tượng và kỹ năng nghe hiểu hội thoại thương mại Nhật Bản.")
                .thumbnailUrl("https://images.unsplash.com/photo-1545569341-9eb8b30979d9?q=80&w=800&auto=format&fit=crop")
                .price(699000.0)
                .discountPrice(599000.0)
                .durationHours(75)
                .lessonCount(48)
                .coveredSkills("1000 Kanji N2, Từ vựng báo chí, Đọc hiểu trường văn, Nghe hiểu thông tin nhanh")
                .objectives("• Làm chủ 1000 chữ Hán và 6000 từ vựng trình độ trung cao cấp.\n• Tự tin đọc hiểu các bài báo, bài viết xã luận phức tạp.\n• Đạt điểm số JLPT N2 trên 120/180 điểm để xin việc, du học.")
                .targetAudience("Học viên đã có chứng chỉ N3 và muốn đạt trình độ làm việc trong môi trường nói tiếng Nhật.")
                .prerequisites("Đã hoàn thành chương trình trung cấp N3.")
                .status("PUBLISHED")
                .featured(false)
                .enrollmentCount(640)
                .rating(4.9)
                .build();
        addLessons(n2, List.of(
                new Object[]{"Bài 01: Phương pháp giải đề N2 đạt điểm tối đa", "Chương 1: Chiến thuật giải đề", 35, "VIDEO", true},
                new Object[]{"Bài 02: Phân tích 120 mẫu ngữ pháp diễn đạt cảm xúc & đánh giá", "Chương 2: Ngữ pháp học thuật N2", 50, "VIDEO", false},
                new Object[]{"Bài 03: Kỹ năng xử lý bài Đọc hiểu trường văn và so sánh quan điểm", "Chương 3: Đọc hiểu nâng cao", 45, "READING", false},
                new Object[]{"Bài 04: Nghe hiểu tổng hợp thông tin và phản xạ tức thì", "Chương 4: Nghe hiểu tốc độ cao", 40, "VIDEO", false},
                new Object[]{"Bài 05: Đề thi thử JLPT N2 Chuẩn cấu trúc đề thi chính thức", "Chương 5: Thi thử áp lực", 105, "QUIZ", false}
        ));
        courses.add(n2);

        // 5. Course N1
        Course n1 = Course.builder()
                .title("Chuyên gia Tiếng Nhật Học thuật JLPT N1 Đỉnh cao")
                .slug("chuyen-gia-tieng-nhat-jlpt-n1")
                .code("JLPT-N1")
                .level("N1")
                .shortDescription("Đỉnh cao ngôn ngữ với ngữ pháp văn viết cổ và văn phong xã luận, văn học hiện đại chuẩn đề Bộ Giáo dục Nhật.")
                .fullDescription("Khóa học N1 đỉnh cao dành cho các bạn muốn thành thạo tiếng Nhật như người bản xứ. Toàn bộ tài liệu được trích xuất từ các bài luận văn, tài liệu nghiên cứu học thuật và báo chí chính thống của Nhật Bản.")
                .thumbnailUrl("https://images.unsplash.com/photo-1492571350019-22de08371fd3?q=80&w=800&auto=format&fit=crop")
                .price(899000.0)
                .discountPrice(749000.0)
                .durationHours(90)
                .lessonCount(50)
                .coveredSkills("2000 Kanji N1, Văn phong cổ & xã luận, Đọc hiểu luận đề, Nghe hiểu tin tức")
                .objectives("• Hoàn thiện vốn 2000 Hán tự và 10.000 từ vựng cao cấp.\n• Tự tin viết luận, dịch thuật và nghiên cứu học thuật bằng tiếng Nhật.\n• Chinh phục kỳ thi JLPT N1 với thành tích ấn tượng.")
                .targetAudience("Phiên dịch viên, du học sinh, giảng viên và những người muốn chạm đến đỉnh cao tiếng Nhật.")
                .prerequisites("Đã có chứng chỉ hoặc trình độ vững vàng JLPT N2.")
                .status("PUBLISHED")
                .featured(false)
                .enrollmentCount(320)
                .rating(5.0)
                .build();
        addLessons(n1, List.of(
                new Object[]{"Bài 01: Tư duy ngôn ngữ và phong cách viết học thuật N1", "Chương 1: Dẫn nhập học thuật", 40, "VIDEO", true},
                new Object[]{"Bài 02: 150 Mẫu ngữ pháp cổ và văn phong trang trọng nhất", "Chương 2: Ngữ pháp thượng cấp", 55, "VIDEO", false},
                new Object[]{"Bài 03: Đọc hiểu luận đề triết học, kinh tế và tâm lý học", "Chương 3: Đọc hiểu học thuật", 50, "READING", false},
                new Object[]{"Bài 04: Nghe hiểu thời sự NHK và hội thảo khoa học", "Chương 4: Nghe hiểu chuyên sâu", 45, "VIDEO", false},
                new Object[]{"Bài 05: Đề thi thử JLPT N1 Toàn diện thời gian thực", "Chương 5: Thi thử N1", 110, "QUIZ", false}
        ));
        courses.add(n1);

        courseRepository.saveAll(courses);
        log.info("Successfully seeded 5 JLPT courses (N5 to N1) with detailed lessons and syllabus!");
    }

    private void addLessons(Course course, List<Object[]> lessonData) {
        int index = 1;
        for (Object[] row : lessonData) {
            Lesson lesson = Lesson.builder()
                    .course(course)
                    .title((String) row[0])
                    .sectionName((String) row[1])
                    .durationMinutes((Integer) row[2])
                    .lessonType((String) row[3])
                    .isFreePreview((Boolean) row[4])
                    .orderIndex(index++)
                    .description("Nội dung chi tiết bài học " + row[0] + " bám sát chương trình giảng dạy.")
                    .build();
            course.getLessons().add(lesson);
        }
    }
}
