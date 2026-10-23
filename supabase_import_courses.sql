-- ==============================================================================
-- JLPP SUPABASE IMPORT SCRIPT: COURSES, LESSONS, USERS, ENROLLMENTS
-- ==============================================================================

-- 1. Ensure Tables Exist
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100),
    username VARCHAR(50) UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'LEARNER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    phone_number VARCHAR(20),
    current_jlpt_level VARCHAR(20) DEFAULT 'N5',
    target_jlpt_level VARCHAR(20) DEFAULT 'N4',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250),
    code VARCHAR(50) UNIQUE,
    level VARCHAR(20) NOT NULL,
    short_description VARCHAR(500),
    full_description TEXT,
    thumbnail_url VARCHAR(500),
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_price DOUBLE PRECISION,
    duration_hours INTEGER DEFAULT 40,
    lesson_count INTEGER DEFAULT 30,
    covered_skills VARCHAR(255),
    objectives TEXT,
    target_audience VARCHAR(500),
    prerequisites VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    is_featured BOOLEAN DEFAULT FALSE,
    enrollment_count INTEGER DEFAULT 0,
    rating DOUBLE PRECISION DEFAULT 4.9,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    section_name VARCHAR(150),
    duration_minutes INTEGER DEFAULT 30,
    lesson_type VARCHAR(30) DEFAULT 'VIDEO',
    is_free_preview BOOLEAN DEFAULT FALSE,
    order_index INTEGER NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_registrations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    progress_percent INTEGER DEFAULT 0,
    paid_amount DOUBLE PRECISION DEFAULT 0.0,
    payment_method VARCHAR(50) DEFAULT 'FREE',
    note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_course UNIQUE (user_id, course_id)
);

-- 2. Insert Default Learner (if not exists)
INSERT INTO users (full_name, username, email, password, role, status, phone_number, current_jlpt_level, target_jlpt_level)
VALUES ('Nguyễn Văn Học Viên', 'learner', 'learner@jeps.edu.vn', 'Password123@', 'LEARNER', 'ACTIVE', '0987654321', 'N5', 'N4')
ON CONFLICT (email) DO UPDATE SET 
    full_name = EXCLUDED.full_name,
    current_jlpt_level = EXCLUDED.current_jlpt_level,
    target_jlpt_level = EXCLUDED.target_jlpt_level;

-- 3. Clean and Populate Courses
DELETE FROM lessons;
DELETE FROM course_registrations;
DELETE FROM courses;

-- Course 1: N5 Toàn diện
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (1, 'Khóa học Nhập môn Tiếng Nhật & JLPT N5 Toàn diện', 'nhap-mon-tieng-nhat-jlpt-n5', 'JLPT-N5', 'N5', 
'Nắm vững bảng chữ cái Hiragana, Katakana, 100 chữ Hán căn bản và 80 mẫu ngữ pháp giao tiếp đời sống.',
'Khóa học Nhập môn Tiếng Nhật & JLPT N5 Toàn diện được thiết kế độc quyền theo chuẩn khung năng lực JLPT Nhật Bản. Học viên sẽ được trang bị hệ thống phát âm chuẩn Tokyo từ bảng chữ cái Hiragana, Katakana, cho đến làm chủ 100 chữ Hán thường gặp nhất và tự tin vượt qua kỳ thi JLPT N5 với điểm số cao.',
'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?q=80&w=800&auto=format&fit=crop',
299000, 199000, 40, 30, 'Hiragana/Katakana, Chữ Hán, Từ vựng, Ngữ pháp, Nghe hiểu N5',
'• Làm chủ hoàn toàn 2 bảng chữ cái và các quy tắc biến âm, trường âm, âm ngắt.
• Nắm vững 100 chữ Kanji và 800 từ vựng sơ cấp căn bản.
• Nắm vững các mẫu ngữ pháp trợ từ, thì quá khứ, thể Te, thể Nai.
• Tự tin làm bài thi thử JLPT N5 đạt trên 120/180 điểm.',
'Người mới bắt đầu học tiếng Nhật từ con số 0 hoặc muốn củng cố lại toàn bộ nền tảng sơ cấp.',
'Không yêu cầu kiến thức nền tảng trước đó. Chỉ cần sự chăm chỉ và kiên trì mỗi ngày 30 phút.',
'PUBLISHED', TRUE, 1280, 4.9);

-- Course 2: N5 Kanji
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (2, 'Chiến thần Chữ Hán N5: Bí quyết nhớ 150 Kanji qua Câu chuyện', 'chien-than-chu-han-n5', 'KANJI-N5', 'N5',
'Phương pháp ghi nhớ 150 chữ Hán N5 qua hình ảnh minh họa độc đáo, nhớ sâu, không quên mặt chữ.',
'Khắc phục nỗi sợ chữ Hán của người học tiếng Nhật! Bằng phương pháp liên tưởng hình ảnh trực quan cùng các câu chuyện vui nhộn, học viên có thể nhận diện và viết chính xác 150 chữ Hán căn bản cùng 350 từ vựng phái sinh chỉ sau 2 tuần học.',
'https://images.unsplash.com/photo-1542051841857-5f90071e7989?q=80&w=800&auto=format&fit=crop',
199000, 99000, 25, 20, '150 Kanji N5, Bộ thủ Hán tự, Âm Hán Việt, Quy tắc bút thuận',
'• Nhận diện tức thì 150 Hán tự sơ cấp N5.
• Hiểu bản chất các bộ thủ cơ bản để học lên N4, N3 dễ dàng.
• Tự tin làm trọn điểm phần thi Moji-Goi (Chữ Hán - Từ vựng).',
'Học viên N5 đang gặp khó khăn khi nhớ chữ Hán hoặc hay bị nhầm lẫn các nét tương đồng.',
'Đã thuộc 2 bảng chữ cái Hiragana và Katakana.',
'PUBLISHED', FALSE, 840, 4.8);

-- Course 3: N4 Cấp tốc
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (3, 'Luyện thi Sơ cấp Cấp tốc JLPT N4 Đột phá', 'luyen-thi-so-cap-jlpt-n4', 'JLPT-N4', 'N4',
'Mở rộng 300 Hán tự thông dụng, hệ thống thể điều kiện, bị động, sai khiến và phản xạ nghe hiểu hội thoại.',
'Khóa học N4 là giai đoạn bản lề quan trọng nhất để chuyển giao từ sơ cấp sang trung cấp. Bạn sẽ được củng cố toàn bộ các dạng thể động từ phức tạp (bị động, sai khiến, thể kính ngữ sơ cấp) cùng ngân hàng đề thi bám sát cấu trúc mới nhất.',
'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=800&auto=format&fit=crop',
399000, 299000, 50, 35, '300 Kanji N4, 1500 Từ vựng, Thể bị động, Sai khiến, Đọc hiểu N4',
'• Làm chủ 300 Hán tự thông dụng và cách ghép từ thuần thục.
• Thành thạo thể Bị động (Ukemi), Sai khiến (Shieki) và Kính ngữ cơ bản.
• Tăng tốc độ đọc hiểu văn bản ngắn và vừa dưới áp lực thời gian.
• Tự tin đạt trên 130/180 điểm thi JLPT N4.',
'Học viên đã hoàn thành trình độ N5 hoặc đã học hết 25 bài Minna no Nihongo.',
'Đã có kiến thức nền tảng tương đương JLPT N5.',
'PUBLISHED', TRUE, 950, 4.9);

-- Course 4: N4 Kaiwa
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (4, 'Tiếng Nhật Giao tiếp & Phản xạ Hội thoại Đời sống (N5-N4)', 'tieng-nhat-giao-tiep-hoi-thoai-n5-n4', 'KAIWA-N4', 'N4',
'Bứt phá khả năng nói tiếng Nhật lưu loát, chuẩn ngữ điệu người Tokyo, tự tin bắt chuyện với người Nhật.',
'Học ngữ pháp nhiều nhưng không mở miệng nói được? Khóa học phản xạ Kaiwa ứng dụng phương pháp Shadowing chuyên sâu giúp bạn phá bỏ rào cản tâm lý ngại nói, tự tin giao tiếp trong các tình huống mua sắm, nhà ga, ăn uống và công việc.',
'https://images.unsplash.com/photo-1513407030348-c983a97b98d8?q=80&w=800&auto=format&fit=crop',
350000, 249000, 30, 24, 'Shadowing phản xạ, Phát âm Intonation, Hội thoại đời sống, Văn hóa ứng xử',
'• Tự tin hội thoại với người bản xứ trong hơn 50 tình huống sinh hoạt.
• Chữa triệt để lỗi ngập ngừng, phát âm sai ngữ điệu.
• Làm chủ các cách nói chuyện ngắn gọn, tự nhiên của người Nhật trẻ.',
'Học viên đang học N5-N4 muốn nói tiếng Nhật trôi chảy tự nhiên.',
'Biết ngữ pháp cơ bản đến bài 25 Minna no Nihongo.',
'PUBLISHED', FALSE, 620, 4.9);

-- Course 5: N3 Trung cấp
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (5, 'Chinh phục Đột phá Điểm số JLPT N3 Trung cấp', 'chinh-phuc-jlpt-n3-trung-cap', 'JLPT-N3', 'N3',
'Bước ngoặt trung cấp với 650 Kanji, 150 mẫu ngữ pháp then chốt và chiến lược làm bài đọc hiểu phân tích.',
'JLPT N3 là tấm vé vàng mở ra cơ hội việc làm tại các doanh nghiệp Nhật Bản. Khóa học tập trung giải quyết dứt điểm các bẫy ngữ pháp dễ nhầm lẫn, nâng cao năng lực đọc hiểu đoạn văn dài và phản xạ nghe hiểu tức thì.',
'https://images.unsplash.com/photo-1528164344705-475426879c0d?q=80&w=800&auto=format&fit=crop',
549000, 449000, 65, 42, '650 Kanji N3, Ngữ pháp trung cấp, Đọc hiểu phân tích, Nghe hiểu phản xạ',
'• Thuộc lòng 650 Kanji và 3000 từ vựng trung cấp thường gặp.
• Phân biệt rõ ràng các cặp ngữ pháp tương đồng dễ nhầm lẫn.
• Đọc hiểu nhanh các bài xã luận, bài bình luận ngắn.
• Đạt chuẩn điểm số JLPT N3 trên 115/180 điểm.',
'Học viên đã đỗ N4 hoặc hoàn thành 50 bài Minna no Nihongo.',
'Kiến thức tiếng Nhật sơ cấp vững chắc (JLPT N4).',
'PUBLISHED', TRUE, 820, 4.8);

-- Course 6: N3 Luyện đề Shinkanzen
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (6, 'Chiến thuật 60 ngày đỗ JLPT N3 - Bộ đề Shinkanzen Master', 'chien-thuat-60-ngay-do-jlpt-n3', 'CRAM-N3', 'N3',
'Khóa giải đề cấp tốc, tổng hợp 100 dạng câu hỏi bẫy ngữ pháp và mẹo làm nhanh phần Đọc - Nghe.',
'Được xây dựng đặc biệt cho giai đoạn nước rút 2 tháng trước kỳ thi JLPT. Bộ đề bám sát 100% đề thi thật các năm gần đây cùng video phân tích chi tiết từng phương án nhiễu.',
'https://images.unsplash.com/photo-1526772662000-3f88f10405ff?q=80&w=800&auto=format&fit=crop',
499000, 349000, 45, 30, 'Giải đề Shinkanzen, Xử lý bẫy ngữ pháp, Mẹo đọc lướt Skimming, Nghe hiểu điểm rơi',
'• Thành thục kỹ năng loại trừ đáp án gây nhiễu.
• Tăng tối thiểu 25-35 điểm tổng kết quả thi JLPT N3.
• Tối ưu thời gian làm bài, không bị thiếu giờ phần Dokkai.',
'Học viên chuẩn bị thi JLPT N3 cần tổng ôn và luyện đề thực chiến.',
'Đã học xong kiến thức nền tảng trình độ N3.',
'PUBLISHED', FALSE, 710, 4.9);

-- Course 7: N2 Luyện đề Chuyên sâu
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (7, 'Luyện đề Chuyên sâu & Kỹ năng Làm bài JLPT N2', 'luyen-de-chuyen-sau-jlpt-n2', 'JLPT-N2', 'N2',
'Khóa học trang bị tư duy giải đề chuẩn, phản xạ nghe hiểu tốc độ cao và mở rộng vốn từ vựng thương mại.',
'Khóa học hướng đến mục tiêu giúp học viên vượt qua kỳ thi JLPT N2 với điểm số xuất sắc. Chương trình giảng dạy tích hợp hệ thống bài tập thực chiến, phân tích chuyên sâu các bài đọc hiểu trừu tượng và kỹ năng nghe hiểu hội thoại thương mại Nhật Bản.',
'https://images.unsplash.com/photo-1545569341-9eb8b30979d9?q=80&w=800&auto=format&fit=crop',
699000, 599000, 75, 48, '1000 Kanji N2, Từ vựng báo chí, Đọc hiểu trường văn, Nghe hiểu thông tin nhanh',
'• Làm chủ 1000 chữ Hán và 6000 từ vựng trình độ trung cao cấp.
• Tự tin đọc hiểu các bài báo, bài viết xã luận phức tạp.
• Đạt điểm số JLPT N2 trên 120/180 điểm để xin việc, du học.',
'Học viên đã có chứng chỉ N3 và muốn đạt trình độ làm việc trong môi trường nói tiếng Nhật.',
'Đã hoàn thành chương trình trung cấp N3.',
'PUBLISHED', TRUE, 640, 4.9);

-- Course 8: N2 Tiếng Nhật Thương mại
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (8, 'Tiếng Nhật Thương mại & Kỹ năng Phỏng vấn Doanh nghiệp Nhật (BJT & N2)', 'tieng-nhat-thuong-mai-phong-van-n2', 'BIZ-N2', 'N2',
'Làm chủ nghệ thuật Kính ngữ Keigo, viết Email công sở chuyên nghiệp và chinh phục nhà tuyển dụng Nhật.',
'Khóa học chuẩn hóa tác phong công sở Nhật Bản (Horenso, Meishi, Aisatsu) và hệ thống Kính ngữ Keigo cao cấp. Hướng dẫn viết CV (Rirekisho), thư ngỏ và phỏng vấn trực tiếp bằng tiếng Nhật đạt tỷ lệ trúng tuyển cao.',
'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=800&auto=format&fit=crop',
750000, 549000, 35, 25, 'Kính ngữ Keigo, Email thương mại, Viết CV Nhật, Phỏng vấn xin việc',
'• Sử dụng nhuần nhuyễn Kính ngữ trong công việc, không bị sai lễ nghi.
• Tự tin viết Email chuyên nghiệp và báo cáo Horenso chuẩn tác phong.
• Vượt qua các vòng phỏng vấn gắt gao của tập đoàn Nhật Bản.',
'Học viên trình độ N3-N2 chuẩn bị đi làm tại công ty Nhật hoặc dự thi BJT.',
'Trình độ tương đương JLPT N3 trở lên.',
'PUBLISHED', FALSE, 510, 4.9);

-- Course 9: N1 Học thuật Đỉnh cao
INSERT INTO courses (id, title, slug, code, level, short_description, full_description, thumbnail_url, price, discount_price, duration_hours, lesson_count, covered_skills, objectives, target_audience, prerequisites, status, is_featured, enrollment_count, rating)
VALUES (9, 'Chuyên gia Tiếng Nhật Học thuật JLPT N1 Đỉnh cao', 'chuyen-gia-tieng-nhat-jlpt-n1', 'JLPT-N1', 'N1',
'Đỉnh cao ngôn ngữ với ngữ pháp văn viết cổ và văn phong xã luận, văn học hiện đại chuẩn đề Bộ Giáo dục Nhật.',
'Khóa học N1 đỉnh cao dành cho các bạn muốn thành thạo tiếng Nhật như người bản xứ. Toàn bộ tài liệu được trích xuất từ các bài luận văn, tài liệu nghiên cứu học thuật và báo chí chính thống của Nhật Bản.',
'https://images.unsplash.com/photo-1492571350019-22de08371fd3?q=80&w=800&auto=format&fit=crop',
899000, 749000, 90, 50, '2000 Kanji N1, Văn phong cổ & xã luận, Đọc hiểu luận đề, Nghe hiểu tin tức',
'• Hoàn thiện vốn 2000 Hán tự và 10.000 từ vựng cao cấp.
• Tự tin viết luận, dịch thuật và nghiên cứu học thuật bằng tiếng Nhật.
• Chinh phục kỳ thi JLPT N1 với thành tích ấn tượng.',
'Phiên dịch viên, du học sinh, giảng viên và những người muốn chạm đến đỉnh cao tiếng Nhật.',
'Đã có chứng chỉ hoặc trình độ vững vàng JLPT N2.',
'PUBLISHED', FALSE, 320, 5.0);

-- Reset sequence for courses
SELECT setval('courses_id_seq', (SELECT MAX(id) FROM courses));

-- 4. Insert Detailed Lessons for Courses
INSERT INTO lessons (course_id, title, section_name, duration_minutes, lesson_type, is_free_preview, order_index, description) VALUES
(1, 'Bài 01: Làm quen Bảng chữ cái Hiragana & Phát âm chuẩn Tokyo', 'Chương 1: Bảng chữ cái & Ngữ âm', 25, 'VIDEO', TRUE, 1, 'Làm quen bảng chữ cái Hiragana'),
(1, 'Bài 02: Bảng chữ cái Katakana & Các từ mượn phổ biến', 'Chương 1: Bảng chữ cái & Ngữ âm', 25, 'VIDEO', TRUE, 2, 'Làm quen bảng chữ cái Katakana'),
(1, 'Bài 03: Quy tắc Biến âm, Âm ghép, Trường âm và Âm ngắt', 'Chương 1: Bảng chữ cái & Ngữ âm', 30, 'VIDEO', FALSE, 3, 'Các quy tắc phát âm quan trọng'),
(1, 'Bài 04: Giới thiệu bản thân & Trợ từ cơ bản wa, ga, no', 'Chương 2: Ngữ pháp cốt lõi N5', 35, 'VIDEO', FALSE, 4, 'Trợ từ và mẫu câu giới thiệu'),
(1, 'Bài 05: 20 Chữ Hán đầu tiên: Số đếm, Ngày tháng & Gia đình', 'Chương 2: Ngữ pháp cốt lõi N5', 30, 'PRACTICE', FALSE, 5, '20 chữ Hán căn bản'),
(1, 'Bài 06: Thể Te và các ứng dụng diễn đạt yêu cầu, nối câu', 'Chương 3: Đột phá Ngữ pháp hành động', 40, 'VIDEO', FALSE, 6, 'Biến đổi động từ thể Te'),
(1, 'Bài 07: Kỹ năng nghe hiểu đoạn hội thoại ngắn đời thường', 'Chương 4: Luyện kỹ năng Đọc - Nghe', 35, 'VIDEO', FALSE, 7, 'Luyện phản xạ nghe'),
(1, 'Bài 08: Đề thi thử JLPT N5 Chuẩn format kỳ thi thật', 'Chương 5: Thi thử & Đánh giá', 60, 'QUIZ', FALSE, 8, 'Đề thi thử N5 tổng hợp');

INSERT INTO lessons (course_id, title, section_name, duration_minutes, lesson_type, is_free_preview, order_index, description) VALUES
(3, 'Bài 01: Ôn tập N5 cấp tốc & Khởi động lộ trình N4', 'Chương 1: Củng cố & Nâng cao sơ cấp', 25, 'VIDEO', TRUE, 1, 'Khởi động lộ trình N4'),
(3, 'Bài 02: Thể khả năng (Kanoukei) & Các mẫu câu biểu thị năng lực', 'Chương 1: Củng cố & Nâng cao sơ cấp', 30, 'VIDEO', TRUE, 2, 'Thể khả năng'),
(3, 'Bài 03: 50 Chữ Hán chủ đề Đời sống công sở & Giao thông', 'Chương 2: Chữ Hán & Từ vựng N4', 35, 'PRACTICE', FALSE, 3, '50 Kanji N4 thường gặp'),
(3, 'Bài 04: Thể Bị động (Ukemi) và Bị động phiền toái', 'Chương 3: Ngữ pháp then chốt N4', 45, 'VIDEO', FALSE, 4, 'Thể bị động'),
(3, 'Bài 05: Thể Sai khiến (Shieki) & Kết hợp Bị động sai khiến', 'Chương 3: Ngữ pháp then chốt N4', 40, 'VIDEO', FALSE, 5, 'Thể sai khiến'),
(3, 'Bài 06: Chiến lược xử lý bài Đọc hiểu dạng tìm thông tin và email', 'Chương 4: Kỹ năng Đọc hiểu Dokkai', 35, 'READING', FALSE, 6, 'Kỹ năng đọc hiểu N4'),
(3, 'Bài 07: Kỹ thuật bắt từ khóa trong bài Nghe hiểu Choukai', 'Chương 5: Kỹ năng Nghe hiểu Choukai', 35, 'VIDEO', FALSE, 7, 'Nghe hiểu Choukai'),
(3, 'Bài 08: Đề thi thử JLPT N4 Mô phỏng thời gian thực', 'Chương 6: Thi thử & Chữa đề', 75, 'QUIZ', FALSE, 8, 'Thi thử N4');

INSERT INTO lessons (course_id, title, section_name, duration_minutes, lesson_type, is_free_preview, order_index, description) VALUES
(5, 'Bài 01: Tổng quan cấu trúc đề thi N3 & Chiến lược phân bổ thời gian', 'Chương 1: Tổng quan & Khởi động', 30, 'VIDEO', TRUE, 1, 'Tổng quan N3'),
(5, 'Bài 02: 80 Kanji N3 chủ đề Xã hội, Kinh tế & Môi trường', 'Chương 2: Chữ Hán trung cấp', 40, 'PRACTICE', FALSE, 2, '80 Kanji N3'),
(5, 'Bài 03: Ngữ pháp nhóm nguyên nhân - kết quả (wake da, koto da, mono da)', 'Chương 3: Ngữ pháp chuyên sâu N3', 45, 'VIDEO', FALSE, 3, 'Ngữ pháp N3'),
(5, 'Bài 04: Đọc hiểu trung văn: Cách nắm bắt luận điểm của tác giả', 'Chương 4: Đột phá Đọc hiểu', 40, 'READING', FALSE, 4, 'Đọc hiểu trung văn'),
(5, 'Bài 05: Nghe hiểu phản xạ nhanh trong hội thoại công việc', 'Chương 5: Nghe hiểu Choukai', 35, 'VIDEO', FALSE, 5, 'Nghe hiểu Choukai'),
(5, 'Bài 06: Đề thi thử JLPT N3 Tiêu chuẩn Bộ Giáo dục Nhật', 'Chương 6: Thi thử toàn diện', 90, 'QUIZ', FALSE, 6, 'Thi thử N3');

-- 5. Insert Enrolled Courses for Learner
INSERT INTO course_registrations (user_id, course_id, registered_at, status, progress_percent, paid_amount, payment_method, note)
VALUES 
((SELECT id FROM users WHERE email = 'learner@jeps.edu.vn'), 1, CURRENT_TIMESTAMP - INTERVAL '20 days', 'ACTIVE', 75, 199000, 'VNPAY', 'Đang học chương 4'),
((SELECT id FROM users WHERE email = 'learner@jeps.edu.vn'), 3, CURRENT_TIMESTAMP - INTERVAL '5 days', 'ACTIVE', 25, 299000, 'BANK_TRANSFER', 'Mục tiêu đỗ JLPT N4');
