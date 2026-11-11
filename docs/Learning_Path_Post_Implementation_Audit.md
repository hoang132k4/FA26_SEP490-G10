# BÁO CÁO KIỂM THỬ TÍCH HỢP VÀ HẬU TRIỂN KHAI: PERSONALIZED LEARNING PATH (JEPS)
**FINAL JEPS LEARNING PATH DATA INTEGRITY & POSTGRESQL CONCURRENCY VERIFICATION REPORT**

---

| Thông tin dự án | Chi tiết |
| :--- | :--- |
| **Hệ thống** | JEPS (Japanese Exam Preparation System / JLPP Platform) |
| **Mô-đun kiểm thử** | Personalized Skill-Aware Learning Path & Concurrency-Safe Lesson Progress Tracking |
| **Phiên bản kiến trúc** | Spring Boot 3.3.4 - PostgreSQL 17.11 (Supabase) / Flyway V2 / Hibernate 6.5.3 / Thymeleaf 3 |
| **Ngày thẩm định** | 12/11/2026 (Hoàn tất kiểm định Case 1, Case 2, Case 3, Case 4) |
| **Đơn vị thẩm định** | Senior Spring Boot Architect, PostgreSQL Auditor & QA Verification Team |
| **Trạng thái tổng thể** | **VERIFIED - 50/50 TEST PASS (100%) - CHÍNH THỨC SẴN SÀNG MERGE `develop`** |

---

## MỤC LỤC
1. [TỔNG HỢP KẾT QUẢ KIỂM THỬ MAVEN SUREFIRE (50/50 PASS)](#1-tổng-hợp-kết-quả-kiểm-thử-maven-surefire-5050-pass)
2. [CASE 1: LEGACY SNAPSHOT TIMING & THÍCH ỨNG ORDER_INDEX (0-BASED, 1-BASED, NON-CONTIGUOUS)](#2-case-1-legacy-snapshot-timing--thích-ứng-order_index-0-based-1-based-non-contiguous)
3. [CASE 2: CONCURRENT AUTO-PROVISIONING (KHỞI TẠO LỘ TRÌNH ĐỒNG THỜI AN TOÀN)](#3-case-2-concurrent-auto-provisioning-khởi-tạo-lộ-trình-đồng-thời-an-toàn)
4. [CASE 3: POSTGRESQL ROW LOCKING & FIX LỖI JSONB CASTING TRÊN SUPABASE](#4-case-3-postgresql-row-locking--fix-lỗi-jsonb-casting-trên-supabase)
5. [CASE 4: LEGACY PROGRESS KHI MANAGER ĐẢO TRẬT TỰ BÀI HỌC (MANAGER REORDERING)](#5-case-4-legacy-progress-khi-manager-đảo-trật-tự-bài-học-manager-reordering)
6. [BẢNG TRẠNG THÁI DỮ LIỆU TRƯỚC VÀ SAU (BEFORE/AFTER DATABASE STATE)](#6-bảng-trạng-thái-dữ-liệu-trước-và-sau-beforeafter-database-state)
7. [GIỚI HẠN KIẾN TRÚC CÒN LẠI & KẾT LUẬN MERGE SẴN SÀNG](#7-giới-hạn-kiến-trúc-còn-lại--kết-luận-merge-sẵn-sàng)

---

## 1. TỔNG HỢP KẾT QUẢ KIỂM THỬ MAVEN SUREFIRE (50/50 PASS)

Đã đối chiếu trực tiếp từ các file kết quả thực thi của Maven Surefire (`target/surefire-reports/`):

| Test Suite (Class) | Phạm vi kiểm thử | Số test | PASS | FAIL | SKIP | Môi trường DB |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| [`LearningPathLegacyAndConcurrencyRegressionTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/learningpath/LearningPathLegacyAndConcurrencyRegressionTest.java) | Case 1 (0-based, 1-based, step-10 order_index), Case 2 (Auto-provision concurrency), Case 4 (Manager reordering), Out-of-order, Advisory duration | 7 | **7** | 0 | 0 | H2 (Postgres Mode) |
| [`LearningPathPostgresConcurrencyIT`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/learningpath/LearningPathPostgresConcurrencyIT.java) | Case 3: Kiểm thử khóa bi quan thực tế `FOR NO KEY UPDATE` và JSONB trên PostgreSQL thật | 1 | **1** | 0 | 0 | **PostgreSQL 17 (Supabase)** |
| [`LearningPathPersonalizationTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/learningpath/LearningPathPersonalizationTest.java) | Cá nhân hóa thời lượng (1.5x, 0.75x, 1.0x), bảo toàn trật tự bài học, phân tích Placement | 14 | **14** | 0 | 0 | H2 |
| [`LearningPathTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/learningpath/LearningPathTest.java) | Lập lịch timeline, cuốn chiếu theo ngày, auto-provisioning, mô phỏng recalibrate | 15 | **15** | 0 | 0 | H2 |
| [`LearningPathUserJourneyTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/learningpath/LearningPathUserJourneyTest.java) | 13 bước tích hợp: Học bài -> Sync JSONB -> Recalibrate lịch học | 1 | **1** | 0 | 0 | H2 |
| [`LearnerLearningScreensTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/course/LearnerLearningScreensTest.java) | Màn hình học viên, giao diện phòng học, hoàn thành bài học | 5 | **5** | 0 | 0 | H2 |
| [`CourseRecommendationServiceTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/course/CourseRecommendationServiceTest.java) | Gợi ý khóa học phù hợp năng lực | 2 | **2** | 0 | 0 | Mockito |
| [`CourseRegistrationServiceTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/course/CourseRegistrationServiceTest.java) | Đăng ký khóa học, tính toán phần trăm | 2 | **2** | 0 | 0 | Mockito |
| [`CourseServiceTest`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/course/CourseServiceTest.java) | CRUD và truy vấn khóa học | 3 | **3** | 0 | 0 | Mockito |
| **TỔNG CỘNG HỆ THỐNG** | **Kiểm định toàn diện Lộ trình & Khóa học** | **50** | **50** | **0** | **0** | **100% PASS** |

---

## 2. CASE 1: LEGACY SNAPSHOT TIMING & THÍCH ỨNG ORDER_INDEX (0-BASED, 1-BASED, NON-CONTIGUOUS)

### 2.1. Tái hiện lỗi tiềm ẩn (Confirmed Defect)
- **Khiếm khuyết phát hiện:** Trong logic tính `completedCount` trước đây:
  `stream().filter(l -> ... || (legacyCount > 0 && l.getOrderIndex() <= legacyCount))`
- **Tác động tiêu cực:**
  1. Với khóa học có `order_index` **0-based** (0, 1, 2, 3, 4, 5, 6, 7, 8, 9): Khi `legacyCount = 5`, biểu thức `orderIndex <= 5` đã khớp tới **6 bài học** (bài 0 đến bài 5), gây thừa 1 bài.
  2. Với khóa học có `order_index` **non-contiguous** (ví dụ bước nhảy 10: 10, 20, 30, ..., 100): Biểu thức `orderIndex <= 5` không khớp bất kỳ bài nào (`10 > 5`). Khi học viên hoàn thành Bài 6, tiến độ bị tụt từ 50% xuống chỉ còn 10%!

### 2.2. Giải pháp khắc phục triệt để (Fix)
Trong [`LessonProgressServiceImpl.java`](file:///C:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/learningpath/service/impl/LessonProgressServiceImpl.java):
1. Đọc danh sách bài học đã sắp xếp chuẩn xác: `lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)`.
2. Lấy đúng `legacyCount` bài học đầu tiên theo trật tự danh sách (dựa trên Set ID bài học), bất kể Manager đặt `order_index` là 0-based, 1-based, hay cách quãng 10:
   ```java
   Set<Long> legacyInferredLessonIds = new HashSet<>();
   if (legacyCount > 0) {
       int count = Math.min(legacyCount, courseLessons.size());
       for (int i = 0; i < count; i++) {
           legacyInferredLessonIds.add(courseLessons.get(i).getId());
       }
   }
   long completedCount = courseLessons.stream()
           .filter(l -> progressDto.isLessonCompleted(l.getId()) || legacyInferredLessonIds.contains(l.getId()))
           .count();
   ```
3. Trong `isLessonCompleted()`: Nếu `lessonOrderIndex` không nằm trong khoảng 1-based thông thường (`1 <= order <= totalLessons`), hệ thống tự động tra cứu vị trí bài học trong danh sách sắp xếp của khóa học.

### 2.3. Kết quả xác minh thực nghiệm
- `legacyInferredCount` được đóng băng là `5` **TRƯỚC KHI** tiến độ chung thay đổi.
- Bài 6 được ghi nhận chuẩn xác vào JSONB.
- Tiến độ khóa học tăng từ 50% lên đúng **60%** (5 bài cũ + 1 bài mới).
- Các bài 1–5 duy trì trạng thái hoàn thành suy diễn, tuyệt đối **không sinh timestamp `completedAt` giả mạo** trong JSONB.
- Bài 6 được tính đúng 1 lần (không đếm đúp). Các bài 7–10 giữ nguyên trạng thái chưa học.

---

## 3. CASE 2: CONCURRENT AUTO-PROVISIONING (KHỞI TẠO LỘ TRÌNH ĐỒNG THỜI AN TOÀN)

### 3.1. Tái hiện lỗi tiềm ẩn (Confirmed Defect)
- **Tình huống:** Học viên đã đăng ký khóa học nhưng chưa từng bấm vào trang `/learning-path` (bảng `learning_paths` chưa có dòng dữ liệu nào). Học viên mở 2 tab học bài và hoàn thành Bài 2 và Bài 3 đồng thời.
- **Khiếm khuyết cũ:** `markLessonCompleted()` chỉ tìm kiếm `lp` có sẵn; nếu `lp == null` thì không auto-create, khiến tiến độ JSONB bị bỏ qua hoàn toàn. Ngoài ra, nếu tạo mà không đồng bộ khóa, 2 luồng sẽ chèn 2 bản ghi `LearningPath` trùng lặp cho cùng một học viên và khóa học.

### 3.2. Giải pháp khắc phục triệt để (Fix)
Trong [`LessonProgressServiceImpl.java`](file:///C:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/learningpath/service/impl/LessonProgressServiceImpl.java):
1. Luôn khóa bản ghi `course_registrations` trước bằng `PESSIMISTIC_WRITE` (`findForUpdateByUserIdAndCourseId`). Bản ghi đăng ký khóa học luôn tồn tại khi học viên vào học.
2. Khi giữ khóa đăng ký, kiểm tra nếu `lp == null` thì tiến hành auto-provision `LearningPath` và gọi `saveAndFlush(lp)` ngay trong transaction.
3. Luồng 1 tạo xong và lưu bài học của nó. Luồng 2 bị chặn bởi khóa hàng của đăng ký khóa học, khi vào sau sẽ thấy ngay `LearningPath` đã được tạo bởi Luồng 1, và tiếp tục lấy khóa bi quan trên `LearningPath` để ghi tiếp bài thứ 2.
4. Trật tự lấy khóa cố định 100%: **Khóa `CourseRegistration` trước -> Khóa `LearningPath` sau**, loại trừ hoàn toàn nguy cơ Deadlock.

### 3.3. Kết quả xác minh thực nghiệm
- Thực thi kiểm thử 2 luồng song song qua `CountDownLatch` trong `testCase2_ConcurrentAutoProvisioning_NoDuplicateLearningPath`:
  - **Số bản ghi `LearningPath` tạo ra:** Đúng **1 bản ghi** duy nhất (không trùng lặp).
  - **Nội dung JSONB:** Lưu đầy đủ cả Bài 2 (`99902`) và Bài 3 (`99903`).
  - **Tiến độ tích lũy:** Cập nhật chuẩn xác lên **20%** (2/10 bài).

---

## 4. CASE 3: POSTGRESQL ROW LOCKING & FIX LỖI JSONB CASTING TRÊN SUPABASE

### 4.1. Phát hiện lỗi nghiêm trọng trên PostgreSQL thực tế (Critical PostgreSQL Bug)
Khi chạy kiểm thử trực tiếp trên cơ sở dữ liệu Supabase PostgreSQL 17:
```log
Caused by: org.postgresql.util.PSQLException: ERROR: column "lesson_progress" is of type jsonb but expression is of type character varying
  Hint: You will need to rewrite or cast the expression.
```
- **Nguyên nhân kiến trúc:** Cột `lesson_progress` trong PostgreSQL là kiểu `jsonb`. Trước đây thực thể `LearningPath.java` chỉ khai báo `@Column(name = "lesson_progress", columnDefinition = "jsonb") private String lessonProgress;`. Khi Hibernate 6 đẩy tham số dạng chuỗi, PostgreSQL JDBC driver nhận diện là `VARCHAR` và từ chối ép kiểu ngầm định sang `JSONB`. H2 chấp nhận chuỗi này nhưng PostgreSQL thực tế sẽ ném ngoại lệ dữ liệu!

### 4.2. Khắc phục dứt điểm (Fix)
Bổ sung annotation Hibernate 6 chuẩn cho PostgreSQL trong [`LearningPath.java`](file:///C:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/learningpath/entity/LearningPath.java):
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "lesson_progress", columnDefinition = "jsonb")
private String lessonProgress;
```

### 4.3. Bằng chứng khóa bi quan thực thi trên PostgreSQL 17 (Supabase)
Kiểm thử tích hợp [`LearningPathPostgresConcurrencyIT.java`](file:///C:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/learningpath/LearningPathPostgresConcurrencyIT.java) đã chạy thành công 100% với log câu lệnh Hibernate và PostgreSQL:
```sql
Hibernate: 
    select
        lp1_0.learning_path_id, ...
    from
        learning_paths lp1_0 
    where
        lp1_0.user_id=? 
        and lp1_0.course_id=? 
    order by
        lp1_0.scheduled_date desc,
        lp1_0.learning_path_id desc for no key update
```
- Mệnh đề `FOR NO KEY UPDATE` là cú pháp khóa hàng bi quan chính thức của PostgreSQL, đảm bảo tuần tự hóa các thao tác cập nhật đồng thời trên cùng một lộ trình.
- Cả hai luồng hoàn thành thành công, JSONB trong PostgreSQL lưu đầy đủ 2 bài học, tiến độ đồng bộ chính xác.

---

## 5. CASE 4: LEGACY PROGRESS KHI MANAGER ĐẢO TRẬT TỰ BÀI HỌC (MANAGER REORDERING)

### 5.1. Bản chất dữ liệu lịch sử
- Cơ sở dữ liệu cũ của JEPS trước đợt nâng cấp chỉ lưu **duy nhất 01 con số** `progress_percent` (ví dụ 50%) trong `course_registrations`.
- Cơ sở dữ liệu **hoàn toàn không lưu ID của các bài học cũ đã hoàn thành**. Do đó, mọi cơ chế đối với dữ liệu cũ đều là **suy diễn theo vị trí (Positional Inference)**.
- Khi nâng cấp lên hệ thống mới, các bài học được học kể từ thời điểm này sẽ được lưu **mã định danh bất biến (Verified Lesson ID)** vào JSONB.

### 5.2. Kết quả kiểm chứng khi Manager đổi `order_index`
Thực nghiệm trong `testCase4_ManagerReordering_VerifiedLessonIdsInvariant`:
- Học viên có 50% tiến độ cũ và đã hoàn thành Bài 6 mới (lưu trong JSONB).
- Manager đổi thứ tự: Chuyển Bài 10 lên vị trí 1 (`order_index = 1`) và đưa Bài 1 xuống vị trí 10 (`order_index = 10`).
- **Kết quả xác minh:**
  1. **Bài 6 (Verified JSONB):** Hoàn toàn **bất biến (Invariant)**. Bài 6 vẫn giữ nguyên trạng thái `COMPLETED` cùng timestamp ban đầu, không bị ảnh hưởng bởi việc đổi thứ tự.
  2. **Tiến độ cũ (Legacy Inferred):** Vì là suy diễn theo vị trí 5 bài đầu tiên trong danh sách hiện tại:
     - Bài 10 hiện đứng ở vị trí 1 -> Được suy diễn là hoàn thành.
     - Bài 1 chuyển xuống vị trí 10 -> Không còn nằm trong top 5 bài đầu tiên nên chuyển sang chưa hoàn thành.
- **Kết luận kiến trúc:** Đây là giới hạn tự nhiên không thể tránh khỏi của dữ liệu cũ (do hệ thống cũ không lưu ID bài học). Báo cáo ghi nhận rõ ràng: **Chỉ có bản ghi JSONB mới mang tính định danh thực thể bất biến; tiến độ cũ mang tính suy diễn thứ tự.**

---

## 6. BẢNG TRẠNG THÁI DỮ LIỆU TRƯỚC VÀ SAU (BEFORE/AFTER DATABASE STATE)

### 6.1. Case 1: Học nhảy cóc trên dữ liệu cũ (Out-of-Order Lesson 6 Completion)
| Thực thể / Cột | Trạng thái ban đầu | Sau khi hoàn thành Bài 6 | Ghi chú kiến trúc |
| :--- | :--- | :--- | :--- |
| `course_registrations.progress_percent` | `50%` | **`60%`** | Tăng chính xác 1 bài (6/10) |
| `learning_paths.lesson_progress` | `null` | `{"version": 1, "legacyInferredCount": 5, "lessons": {"99906": {"status": "COMPLETED"}}}` | Đóng băng legacy = 5, lưu duy nhất Bài 6 |
| Bài 1–5 (`isLessonCompleted`) | `true` (Inferred) | **`true` (Frozen Legacy)** | Bảo toàn trạng thái, không ô nhiễm |
| Bài 6 (`isLessonCompleted`) | `false` | **`true` (Verified JSONB)** | Hoàn thành có timestamp chuẩn |
| Bài 7–10 (`isLessonCompleted`) | `false` | **`false`** | Không bị đánh dấu nhầm |

### 6.2. Case 2: Tạo lộ trình đồng thời (Concurrent Auto-Provisioning)
| Tiêu chí | Trước khi gửi Request | Sau khi Luồng 1 & Luồng 2 hoàn tất |
| :--- | :--- | :--- |
| Số bản ghi `learning_paths` | `0` | **`1`** (Được tạo nguyên tử, không duplicate) |
| Cột `lesson_progress` | Chưa tồn tại | `{"lessons": {"99902": {...}, "99903": {...}}, "version": 1, "legacyInferredCount": 0}` |
| `course_registrations.progress_percent` | `0%` | **`20%`** (Đếm đủ 2 bài, không Lost Update) |

---

## 7. GIỚI HẠN KIẾN TRÚC CÒN LẠI & KẾT LUẬN MERGE SẴN SÀNG

### 7.1. Giới hạn kiến trúc cần lưu ý (Architectural Boundaries)
1. **Dữ liệu hoàn thành cũ không có timestamp:** Hệ thống cũ không lưu thời điểm hoàn thành của từng bài học. Trường `completedAt` trong JSONB chỉ bắt đầu có giá trị thật cho các bài học hoàn thành kể từ phiên bản này trở đi.
2. **Khóa học thay đổi thứ tự đối với học viên cũ:** Với các bài học chưa có trong JSONB, trạng thái hoàn thành dựa trên số lượng bài học suy diễn từ `progress_percent` cũ theo trật tự danh sách bài học hiện hành.

### 7.2. Kết luận của Architect
Toàn bộ 4 vấn đề kiểm định cuối cùng:
- **Case 1 (Legacy Snapshot Timing & Order Index Variants):** Đã khắc phục và kiểm chứng trên cả 0-based, 1-based, và step-10 non-contiguous.
- **Case 2 (Concurrent Auto-Provisioning):** Đã tuần tự hóa và bảo đảm không tạo trùng lặp lộ trình.
- **Case 3 (PostgreSQL Row Locking):** Đã khắc phục lỗi `jsonb` casting qua `@JdbcTypeCode(SqlTypes.JSON)` và kiểm chứng thành công trên PostgreSQL 17 (Supabase).
- **Case 4 (Manager Reordering):** Đã chứng minh tính bất biến của JSONB ID và minh định tính chất vị trí của tiến độ cũ.

Hệ thống đã đạt **50/50 test PASS (100%)**, bảo vệ toàn vẹn dữ liệu, không thay đổi cấu trúc bảng ngoài cột JSONB đã phê duyệt. Mô-đun **Personalized Learning Path** chính thức **ĐỦ ĐIỀU KIỆN SẴN SÀNG MERGE VÀO NHÁNH `develop`**.
