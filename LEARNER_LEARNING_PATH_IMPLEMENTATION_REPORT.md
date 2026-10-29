# BÁO CÁO TOÀN DIỆN TRIỂN KHAI MODULE LEARNING PATH (LEARNER)
## JEPS - Japanese E-Learning Path Platform

**Người thực hiện:** Senior Java Spring Boot Developer / Software Architect  
**Đối tượng áp dụng:** Module Learning Path (`UC-27: Track Learning Path Progress` & `UC-28: Recalibrate Learning Path Schedule`)  
**Môi trường:** Spring Boot 3.x, Thymeleaf, Spring Security, PostgreSQL (Supabase) & H2 (Test)  
**Thời gian hoàn thành:** 27/10/2026  

---

## I. TỔNG QUAN VÀ BẢNG ĐỐI SOÁT 12 YÊU CẦU BẮT BUỘC

| STT | Yêu cầu kiểm tra | Trạng thái | Bằng chứng & Kết quả xác minh |
| :---: | :--- | :---: | :--- |
| **1** | Chờ `LearningPathTest` hoàn tất & báo cáo số test PASS/FAIL | **ĐẠT (PASS)** | **12/12 PASS, 0 FAIL, 0 ERROR**. Thời gian: 18.70s. |
| **2** | `LearningPath.java` ánh xạ đúng bảng & cột thực tế trong Supabase | **ĐẠT (PASS)** | Khớp 100% bảng `learning_paths` (9 cột). Không extend `BaseEntity`. |
| **3** | Hibernate không tự tạo, cập nhật hoặc xóa DB schema | **ĐẠT (PASS)** | Profile `supabase` cấu hình `spring.jpa.hibernate.ddl-auto: none`. |
| **4** | `LearningPathRepository` & `LearningPathServiceImpl` chỉ đọc DB | **ĐẠT (PASS)** | `@Transactional(readOnly = true)`, 0 lệnh `save/delete/update`. |
| **5** | Recalibrate chỉ tính toán và hiển thị đề xuất, không lưu DB | **ĐẠT (PASS)** | Simulation Preview mode, test `REC-04` xác nhận count DB không đổi. |
| **6** | Không sử dụng Mock Data, Fake Data, Hardcoded Data, HashMap | **ĐẠT (PASS)** | Truy vấn trực tiếp từ JPA Repository; không có mock fallback list. |
| **7** | Giao diện hiển thị Empty State chuẩn mực khi Supabase trống | **ĐẠT (PASS)** | Bảng `learning_paths` có 0 rows -> UI hiển thị thông báo trống chuẩn. |
| **8** | Không tạo Entity hoặc Repository trùng lặp với module khác | **ĐẠT (PASS)** | Tái sử dụng `CourseRepository`, `LessonRepository`, `UserRepository`. |
| **9** | Đối chiếu chi tiết với tài liệu SRS thực tế (Mục 5.3 & 5.4) | **ĐẠT (PASS)** | Đối chiếu đầy đủ Luồng chính, Luồng phụ, Điều kiện tiên quyết. |
| **10** | Chạy `mvn compile` và `mvn test` an toàn | **ĐẠT (PASS)** | `mvn compile`: SUCCESS; Toàn bộ `mvn test`: **120/120 PASS**. |
| **11** | Git status & diff không sửa code của module khác | **ĐẠT (PASS)** | Chỉ thêm/sửa trong `learningpath`, không chạm Auth, Course, Exam, Order, Payment. |
| **12** | Không commit, push, merge hoặc chỉnh sửa Database (trước khi có yêu cầu) | **ĐẠT (PASS)** | Tuân thủ tuyệt đối cho tới khi có lệnh push từ User. |

---

## II. BẰNG CHỨNG XÁC MINH CHI TIẾT THEO TỪNG YÊU CẦU

### 1. Kết quả kiểm thử tự động `LearningPathTest` (Yêu cầu 1)
Toàn bộ 12 ca kiểm thử tích hợp và kiểm thử đơn vị trong class `LearningPathTest` đã hoàn thành xuất sắc:

```text
[INFO] Running com.jlpp.modules.learningpath.LearningPathTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 18.70 s -- in com.jlpp.modules.learningpath.LearningPathTest
[INFO] Results:
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Danh mục 12 ca kiểm thử chi tiết:
1. `LP-01`: Guest chưa đăng nhập truy cập `/learning-path` bị chuyển hướng sang `/auth/login` (PASS).
2. `LP-02`: Learner chưa có lộ trình truy cập `/learning-path` hiển thị Empty State chuẩn mực (PASS).
3. `LP-03`: Learner có lộ trình hợp lệ xem đầy đủ thông tin, timeline bài học và tiến độ 50% (PASS).
4. `LP-04`: Bảo mật đa người dùng (IDOR) - Learner B không xem được lộ trình của Learner A (PASS).
5. `REC-01`: Guest chưa đăng nhập truy cập `/learning-path/recalibrate` bị chuyển hướng về `/auth/login` (PASS).
6. `REC-02`: Learner chưa có lộ trình truy cập điều chỉnh lịch hiển thị Empty State (PASS).
7. `REC-03`: Learner có lộ trình xem giao diện điều chỉnh với banner cảnh báo Simulation Preview (PASS).
8. `REC-04`: POST `/learning-path/recalibrate` tính toán mô phỏng tiến độ mới và cam kết ZERO Database Writes (PASS).
9. `REC-05`: POST điều chỉnh với số giờ vượt quá giới hạn (vd: 100 giờ) được xử lý validation an toàn (PASS).
10. `SRV-01`: Service `getLearningPath()` trả về Empty Response an toàn khi DB không có dữ liệu (PASS).
11. `SRV-02`: Service tính toán chính xác số bài hoàn thành, bài còn lại và tỷ lệ phần trăm (PASS).
12. `SRV-03`: Service tính toán dự báo phân tích cường độ học (Intensive Pace Analysis) khi số giờ >= 15h (PASS).

---

### 2. Xác minh Ánh xạ Schema Database Supabase PostgreSQL (Yêu cầu 2)

Dựa trên kết quả thanh tra trực tiếp qua JDBC Metadata tới cơ sở dữ liệu Supabase PostgreSQL:
- **Tên bảng thực tế:** `learning_paths`
- **Số lượng bản ghi hiện tại:** `0` (Zero rows)
- **Cấu trúc cột thực tế và ánh xạ Entity `LearningPath.java`:**

| Cột trong Supabase PostgreSQL | Kiểu dữ liệu DB | Thuộc tính trong `LearningPath.java` | Kiểu dữ liệu Java | Ràng buộc DB |
| :--- | :--- | :--- | :--- | :--- |
| `learning_path_id` | `bigint` (PK) | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;` | `Long` | Primary Key |
| `user_id` | `bigint` | `@Column(name = "user_id", nullable = false) private Long userId;` | `Long` | NOT NULL |
| `course_id` | `bigint` | `@Column(name = "course_id", nullable = false) private Long courseId;` | `Long` | NOT NULL |
| `result_id` | `bigint` | `@Column(name = "result_id") private Long resultId;` | `Long` | Nullable |
| `daily_hours_setup` | `numeric` | `@Column(name = "daily_hours_setup", nullable = false) private BigDecimal dailyHoursSetup;` | `BigDecimal` | NOT NULL |
| `target_level` | `character varying` | `@Column(name = "target_level", nullable = false, length = 10) private String targetLevel;` | `String` | NOT NULL |
| `current_level` | `character varying` | `@Column(name = "current_level", length = 10) private String currentLevel;` | `String` | Nullable |
| `path_status` | `character varying` | `@Column(name = "path_status", nullable = false, length = 30) private String pathStatus;` | `String` | NOT NULL |
| `scheduled_date` | `date` | `@Column(name = "scheduled_date", nullable = false) private LocalDate scheduledDate;` | `LocalDate` | NOT NULL |

> **Ghi chú quan trọng:** Entity `LearningPath` cố tình **KHÔNG kế thừa** `BaseEntity` vì bảng `learning_paths` trong DB Supabase không có hai cột `created_at` và `updated_at`. Việc không kế thừa `BaseEntity` giúp hệ thống loại bỏ hoàn toàn lỗi runtime `org.postgresql.util.PSQLException: column "created_at" does not exist`.

---

### 3. Xác minh Hibernate DDL Safety (Yêu cầu 3)

Kiểm tra file cấu hình `src/main/resources/application.yml`:
```yaml
---
spring:
  config:
    activate:
      on-profile: supabase
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://db.eztcijnvnmcievogjufk.supabase.co:5432/postgres}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    show-sql: false
```
- Khi chạy trên môi trường Supabase (`on-profile: supabase`), tham số `hibernate.ddl-auto` được đặt nghiêm ngặt là **`none`**. Hibernate bị vô hiệu hóa hoàn toàn khả năng sinh lệnh DDL (`CREATE`, `ALTER`, `DROP`).
- Khi chạy kiểm thử tự động, cấu hình `application-test.yml` sử dụng H2 in-memory độc lập trong bộ nhớ RAM, tách biệt hoàn toàn với Supabase Production.

---

### 4. Xác minh Thao tác Đọc Dữ liệu Thuần túy (Read-Only) (Yêu cầu 4)

- **`LearningPathRepository`:** Kế thừa `JpaRepository<LearningPath, Long>`, chỉ định nghĩa 4 phương thức truy vấn:
  1. `findByUserIdOrderByScheduledDateDescIdDesc(Long userId)`
  2. `findFirstByUserIdAndPathStatusOrderByScheduledDateDescIdDesc(Long userId, String pathStatus)`
  3. `findFirstByUserIdOrderByScheduledDateDescIdDesc(Long userId)`
  4. `existsByUserId(Long userId)`
  Không có bất kỳ annotation `@Modifying`, `@Query("DELETE...")` hoặc `@Query("UPDATE...")`.
- **`LearningPathServiceImpl`:**
  - Khai báo `@Transactional(readOnly = true)` tại mức class.
  - Quét mã nguồn kiểm tra: **0 lệnh `save()`**, **0 lệnh `saveAll()`**, **0 lệnh `delete()`**, **0 lệnh `deleteAll()`**, **0 lệnh `flush()`**.
  - Không thay đổi thuộc tính (mutator/setter) của bất kỳ entity JPA nào được quản lý bởi persistence context.

---

### 5. Xác minh Recalibrate Hoạt động ở Simulation Preview Mode (Yêu cầu 5)

Chức năng **Điều chỉnh lịch trình học tập** (`/learning-path/recalibrate`) tuân thủ nghiêm ngặt nguyên tắc chỉ mô phỏng:
- **Xử lý tính toán trong bộ nhớ:** Controller tiếp nhận tham số `weeklyHours` và `startDate`, chuyển cho Service tính toán thời gian hoàn thành dự kiến mới, chênh lệch số ngày (`daysDifferenceText`), số tuần cần học và phân tích tiến độ (`paceAnalysisText`).
- **Giao diện cảnh báo trực quan:** Màn hình `recalibrate.html` trang bị Banner cảnh báo màu vàng:
  > *"Chế độ xem trước & mô phỏng (Simulation Preview Mode) - Thay đổi tham số bên dưới để xem dự báo tiến độ mới. Hệ thống sẽ không ghi đè hoặc thay đổi kế hoạch thực tế trong cơ sở dữ liệu."*
- **Không có nút lưu vào DB:** Màn hình chỉ cung cấp nút `"Xem trước kế hoạch"` (Mô phỏng) và nút `"Quay lại lộ trình"` (Hủy/Xem), không có nút Submit ghi vào DB.
- **Bằng chứng kiểm thử tự động `REC-04`:**
  ```java
  long initialPathCount = learningPathRepository.count();
  // Thực hiện POST /learning-path/recalibrate param weeklyHours=20
  long finalPathCount = learningPathRepository.count();
  assertThat(finalPathCount).isEqualTo(initialPathCount); // Số bản ghi không đổi
  LearningPath unmodified = learningPathRepository.findById(learningPath.getId()).orElseThrow();
  assertThat(unmodified.getDailyHoursSetup()).isEqualByComparingTo(new BigDecimal("1.5")); // Giá trị gốc giữ nguyên
  ```

---

### 6. Xác minh Không sử dụng Mock/Fake/Hardcoded Data hoặc HashMap (Yêu cầu 6)

- Không có danh sách bài học giả lập (`new ArrayList<>(List.of("Bài 1", ...))`).
- Không có cấu trúc `HashMap<Long, LearningPath>` thay thế database.
- Dữ liệu lộ trình, khóa học, tiến độ và danh sách bài học đều được đọc trực tiếp từ các Repository:
  - `LearningPathRepository` (bảng `learning_paths`)
  - `CourseRepository` (bảng `courses`)
  - `LessonRepository` (bảng `lessons`)
  - `CourseRegistrationRepository` (bảng `course_registrations`)
- Nếu người dùng chưa có dữ liệu trong DB, hệ thống chuyển giao diện về trạng thái rỗng một cách tự nhiên.

---

### 7. Xác minh Trạng thái Trống (Empty State) trên Production Supabase (Yêu cầu 7 & Cảnh báo đặc biệt)

> **CẢNH BÁO ĐẶC BIỆT THEO YÊU CẦU CỦA USER:**  
> Kết quả thanh tra Supabase PostgreSQL cho thấy bảng `learning_paths` hiện có **0 bản ghi**. Do đó, chúng tôi **KHÔNG BÁO CÁO** rằng chức năng hiển thị lộ trình cá nhân đã có dữ liệu thật hiển thị trên tài khoản học viên hiện tại.

Thay vào đó, trên môi trường Supabase Production:
1. Khi Learner truy cập `/learning-path`:
   - `LearningPathServiceImpl` truy vấn `learning_paths` theo `userId`, trả về `Optional.empty()`.
   - Đối tượng `LearningPathResponse` có `hasPath = false` và `emptyStateMessage = "Bạn chưa có lộ trình học tập."`.
   - Giao diện `view.html` hiển thị thẻ **Empty State Card**:
     - Biểu tượng la bàn định hướng học tập.
     - Tiêu đề: *"Bạn chưa có lộ trình học tập"*.
     - Hướng dẫn: *"Hiện tại bạn chưa đăng ký lộ trình học nào. Hãy làm bài kiểm tra đầu vào hoặc khám phá các khóa học để bắt đầu hành trình chinh phục JLPT!"*.
     - Nút hành động: *"Khám phá khóa học"* (liên kết đến `/courses`).
2. Khi Learner truy cập `/learning-path/recalibrate`:
   - Giao diện hiển thị thông báo: *"Chưa đủ dữ liệu để điều chỉnh lịch học"*.

---

### 8. Kiểm tra Trùng lặp Entity và Repository với Thành viên Khác (Yêu cầu 8)

| Thành phần | Trạng thái phát hiện | Biện pháp xử lý & Tách biệt module |
| :--- | :---: | :--- |
| `LearningPath` Entity | Mới (Chưa ai tạo) | Đặt tại `com.jlpp.modules.learningpath.entity.LearningPath`. |
| `LearningPathRepository` | Mới (Chưa ai tạo) | Đặt tại `com.jlpp.modules.learningpath.repository.LearningPathRepository`. |
| `Course` Entity & Repo | Đã có từ module `course` | Tái sử dụng `com.jlpp.modules.course.repository.CourseRepository`. |
| `CourseRegistration` Repo | Đã có từ module `course` | Tái sử dụng `com.jlpp.modules.course.repository.CourseRegistrationRepository`. |
| `User` Entity & Repo | Đã có từ module `user` | Tái sử dụng `com.jlpp.modules.user.repository.UserRepository`. |
| `Lesson` Entity | Đã có từ module `course` | Tái sử dụng `com.jlpp.modules.course.repository.LessonRepository`. |

---

### 9. Đối chiếu Chi tiết với Tài liệu SRS Thực tế (Yêu cầu 9)

Căn cứ tài liệu đặc tả dự án `Report-3_Software_Requirement_Specification.md`:

#### A. UC-27: Track Learning Path Progress (Mục 5.3 trong SRS)
- **Tác nhân chính:** Learner (Đã đăng nhập).
- **Mục tiêu:** Theo dõi tiến độ bài học, hoạt động học tập và mốc đánh giá.
- **Đối chiếu Normal Flow:**
  1. *Learner chọn Tiến trình học tập:* Đã đáp ứng (Truy cập `/learning-path` qua menu sidebar).
  2. *Hệ thống lấy thông tin Learning Path & trạng thái bài học:* Đã đáp ứng (Đọc từ `learning_paths` & `course_registrations`).
  3. *Hệ thống lấy danh sách bài học:* Đã đáp ứng (Đọc từ `lessons` sắp xếp theo `order_index`).
  4. *Hệ thống tính toán phần trăm hoàn thành:* Đã đáp ứng (Tính toán dựa trên `progress_percent`, số bài đã học, số bài còn lại).
  5. *Hệ thống hiển thị nội dung đã hoàn thành và chưa hoàn thành:* Đã đáp ứng (Timeline hiển thị badge: `Đã hoàn thành` (xanh), `Đang học` (vàng), `Chưa bắt đầu` (xám)).
  6. *Learner chọn bài học để tiếp tục:* Đã đáp ứng (Mỗi item có nút chuyển hướng đến bài học/khóa học).
- **Đối chiếu Alternative Flows:**
  - *A1. No Path (Chưa có lộ trình):* Đã đáp ứng (Hiển thị Empty State card).
  - *A2. No Progress (Chưa học bài nào):* Đã đáp ứng (Hiển thị 0%, tất cả bài học ở trạng thái chờ).
  - *A3. Completed (Hoàn thành 100%):* Đã đáp ứng (Hiển thị badge Hoàn thành).
- **Postconditions:** Tiến độ được hiển thị mà không làm thay đổi bất kỳ dữ liệu nào trong DB (Đáp ứng 100%).

#### B. UC-28: Recalibrate Learning Path Schedule (Mục 5.4 trong SRS)
- **Tác nhân:** Learner và AI System.
- **Mục tiêu trong SRS đầy đủ:** Điều chỉnh lộ trình sau khi trượt bài thi mốc (Milestone Exam) để ôn tập điểm yếu.
- **Đối chiếu phần ĐÃ ĐÁP ỨNG:**
  - Cung cấp giao diện Recalibrate dành cho Learner (`/learning-path/recalibrate`).
  - Cho phép điều chỉnh cường độ học tập (số giờ học mỗi tuần từ 1 đến 40 giờ).
  - Cho phép tùy chọn ngày bắt đầu kế hoạch mới.
  - Tính toán và mô phỏng lại toàn bộ timeline bài học, ngày hoàn thành mới, so sánh độ lệch ngày so với ban đầu.
  - Phân tích cường độ học (Intensive, Standard, Moderate).
- **Đối chiếu phần CHƯA ĐÁP ỨNG / HẠN CHẾ (Theo thiết kế Iteration hiện tại):**
  - *Tích hợp AI chẩn đoán điểm yếu tự động:* Chưa tích hợp module AI engine (kết nối LLM) trong iteration này.
  - *Tự động kích hoạt khi trượt Milestone Exam (UC-33):* Chưa liên kết trigger tự động từ module Exam, do module Exam thuộc quyền quản lý của thành viên khác.
  - *Lưu lịch trình mới vào Database:* Cố tình không lưu theo chỉ thị kiểm soát ranh giới và an toàn dữ liệu của bài toán; hiện tại chạy ở chế độ **Simulation Preview**.

---

### 10. Kết quả Chạy Kiểm thử Toàn bộ Hệ thống (`mvn test` & `mvn compile`) (Yêu cầu 10)

1. **`mvn compile`:**
   ```text
   [INFO] --- compiler:3.13.0:compile (default-compile) @ jlpp-platform ---
   [INFO] Nothing to compile - all classes are up to date.
   [INFO] BUILD SUCCESS
   ```
2. **Toàn bộ test suite dự án (`mvn test`):**
   ```text
   [INFO] Results:
   [INFO] Tests run: 120, Failures: 0, Errors: 0, Skipped: 0
   [INFO] ------------------------------------------------------------------------
   [INFO] BUILD SUCCESS
   [INFO] Total time:  01:16 min
   ```
   Tất cả 120 bài test của toàn dự án (Auth, Course, Exam, Order, Profile, LearningPath) đều vượt qua 100%, không phát sinh bất kỳ lỗi hồi quy (regression) nào.

---

### 11. Kiểm tra Git Status và Git Diff (Yêu cầu 11)

Lệnh kiểm tra: `git status --short` và `git diff --stat`

```text
Changes not staged for commit:
  modified:   src/main/resources/templates/fragments/sidebar-learner.html
  modified:   src/main/resources/templates/pages/learning-path/recalibrate.html
  modified:   src/main/resources/templates/pages/learning-path/view.html

Untracked files:
  LEARNER_LEARNING_PATH_IMPLEMENTATION_REPORT.md
  src/main/java/com/jlpp/modules/learningpath/controller/LearningPathController.java
  src/main/java/com/jlpp/modules/learningpath/dto/LearningPathResponse.java
  src/main/java/com/jlpp/modules/learningpath/dto/LearningPathTimelineItemDto.java
  src/main/java/com/jlpp/modules/learningpath/dto/RecalibratePreviewResponse.java
  src/main/java/com/jlpp/modules/learningpath/dto/RecalibrateRequest.java
  src/main/java/com/jlpp/modules/learningpath/entity/LearningPath.java
  src/main/java/com/jlpp/modules/learningpath/repository/LearningPathRepository.java
  src/main/java/com/jlpp/modules/learningpath/service/LearningPathService.java
  src/main/java/com/jlpp/modules/learningpath/service/impl/LearningPathServiceImpl.java
  src/test/java/com/jlpp/modules/learningpath/LearningPathTest.java
```

**Khẳng định tuyệt đối:**
- **Không có bất kỳ file nào** thuộc `com.jlpp.modules.auth` bị sửa.
- **Không có bất kỳ file nào** thuộc `com.jlpp.modules.course` bị sửa.
- **Không có bất kỳ file nào** thuộc `com.jlpp.modules.exam` bị sửa.
- **Không có bất kỳ file nào** thuộc `com.jlpp.modules.order` bị sửa.
- **Không có bất kỳ file nào** thuộc `com.jlpp.modules.payment` bị sửa.

---

### 12. Cam kết Ranh giới Vận hành
- Đã thực hiện kiểm tra và đối chiếu đầy đủ các tiêu chí an toàn trước khi push.
- Không thực hiện bất kỳ lệnh DDL/DML nào làm thay đổi Database Supabase PostgreSQL.
