Bạn là UI/UX designer. Hãy tạo cho tôi file wireframe định dạng **Pencil Project (Evolus Pencil) .epgz** gồm 15 trang (mỗi màn hình 1 page), kích thước mỗi page 1440x900. Dùng các stencil có sẵn của Pencil (Desktop - Sketchy GUI hoặc Common Shapes / Bootstrap nếu có): Rectangle, Text, Button, TextBox, ComboBox, CheckBox, Table/Grid, Tab, Badge (rectangle bo góc nhỏ có chữ). Toàn bộ chữ trên giao diện bằng **tiếng Việt** đúng như mô tả dưới đây.

Nếu bạn không thể xuất trực tiếp file .epgz, hãy xuất cấu trúc thư mục gồm `content.xml` + `pages/page_*.xml` theo đúng định dạng của Pencil 3.x để tôi nén zip và đổi đuôi thành .epgz.

---

## A. LAYOUT CHUNG (áp dụng cho cả 14 màn hình)

Hệ thống: **JEPS – Hệ thống luyện thi JLPT tiếng Nhật**, vai trò đang dùng: **Manager**.

**Màu sắc:**
- Navy đậm (sidebar, tiêu đề): #1E2A4A
- Steel blue (badge, nút chính): #4A6FA5
- Nền trang: #F4F6FA; card nền trắng, bo góc 12px, đổ bóng nhẹ
- Badge: xanh lá = ACTIVE/PUBLISHED, vàng = DRAFT, xám = ARCHIVED, xanh dương = loại câu hỏi, đỏ = HARD, xanh lá = EASY

**Sidebar trái (rộng 260px, nền navy #1E2A4A, chữ trắng):**
- Logo "JEPS" + dòng nhỏ "Quản lý Nội dung"
- Chip nhỏ: "MANAGER"
- Menu dọc (icon + chữ): Dashboard, Khóa học, Bài học, **Câu hỏi**, **Bài quiz**, **Đề thi**, Flashcard, Báo cáo
- Dưới cùng: "Đăng xuất" (chữ màu cam đỏ)
- Mục đang active được tô nền sáng hơn

**Header trên (nền trắng, cao 64px, viền dưới):**
- Trái: "Quản lý nội dung • JEPS" (chữ đậm navy)
- Phải: badge "Manager Portal" (nền steel blue) + nút viền đỏ bo tròn "Đăng xuất"

**Vùng nội dung:** padding 24px. Mỗi trang bắt đầu bằng hàng tiêu đề: bên trái là H2 đậm navy + dòng mô tả xám nhỏ, bên phải là nút hành động.

---

## B. MODULE ĐỀ THI (menu active: "Đề thi")

### 1. Exam List – Danh sách đề thi
- Tiêu đề: "Quản lý Đề thi" – mô tả "Quản lý đề thi và câu hỏi trong đề". Nút phải: **[+ Tạo đề thi]** (xanh).
- 3 card thống kê ngang bằng nhau: "Placement – 3", "JLPT Mock – 10", "Milestone – 5" (số to).
- Card bộ lọc 1 hàng: TextBox "Tìm theo tên đề thi...", ComboBox "Tất cả loại" (PLACEMENT_TEST / JLPT_MOCK / MILESTONE), ComboBox "Tất cả cấp độ" (N5–N1), ComboBox "Tất cả trạng thái" (DRAFT / PUBLISHED / ARCHIVED), nút [Lọc] viền xanh, nút [Xoá] viền xám.
- Bảng: ID | Tên đề thi | Loại (badge xanh nhạt) | Cấp độ | Thời lượng | Số câu | Trạng thái (badge) | Thao tác (2 nút icon nhỏ: ✏ Sửa, ☑ Quản lý câu hỏi).
- Dữ liệu mẫu 4–5 dòng, ví dụ: "1 | Đề thi thử JLPT N3 – Số 1 | JLPT_MOCK | N3 | 140 phút | 50 | PUBLISHED".
- Trạng thái rỗng: "Không tìm thấy đề thi."

### 2. Exam Creation – Tạo đề thi
- Tiêu đề: "Tạo đề thi" – mô tả "Thông tin được lưu vào database". Nút phải: [Quay lại].
- Card form, lưới 3 cột:
  - Hàng 1 (full): "Tên đề thi *" – TextBox.
  - Hàng 2: "Loại đề thi *" ComboBox | "Cấp độ JLPT *" ComboBox | "Trạng thái *" ComboBox.
  - Hàng 3: "Tổng số câu hỏi *" TextBox số | "Thời lượng" ô xám **chỉ đọc** hiển thị "140 phút" | "Điểm đạt" ô xám **chỉ đọc** hiển thị "95/180 điểm".
  - Ghi chú nhỏ dưới 2 ô xám: "Tự động theo chuẩn JLPT: N5 105p/80đ, N4 125p/90đ, N3 140p/95đ, N2 155p/90đ, N1 170p/100đ".
  - Hàng 4 (full): "Mô tả" – textarea 5 dòng.
- Cuối form, căn phải: [Huỷ] viền xám, [Lưu cấu hình] xanh.
- Thêm 1 alert đỏ mẫu ở trên form: "Không thể tạo đề thi với trạng thái PUBLISHED. Hãy tạo ở trạng thái DRAFT trước, gắn câu hỏi, rồi chuyển trạng thái."

### 3. Exam Details (View Exam) – Xem đề thi
Màn chỉ xem, đơn giản.
- Tiêu đề: "Chi tiết đề thi". Nút phải: [Quay lại], [✏ Sửa] (xanh).
- Card thông tin, lưới 2–3 cột dạng "nhãn: giá trị":
  Tên đề thi: Đề thi thử JLPT N3 – Số 1 | Loại: badge JLPT_MOCK | Cấp độ: N3 | Trạng thái: badge PUBLISHED | Thời lượng: 140 phút | Điểm đạt: 95/180 | Tổng số câu: 50 | Mô tả: (đoạn text ngắn).
- Card "Câu hỏi trong đề (50)": bảng chỉ đọc # | Nội dung (cắt ngắn) | Loại | Section (badge LANGUAGE / READING / LISTENING) | Điểm. Không có nút thao tác.

### 4. Exam Editing – Sửa đề thi
- Giống màn 2 nhưng tiêu đề "Sửa cấu hình đề thi", các ô đã điền sẵn dữ liệu.
- Phía trên form thêm card nhỏ: "Câu hỏi đã gắn: **50**   Tổng điểm: **180**" bên trái, nút viền xanh lá [☑ Quản lý câu hỏi] bên phải.
- Phía dưới form thêm card "Câu hỏi trong đề (50)" với nút [✏ Chỉnh sửa], bảng chỉ đọc: # | Nội dung | Loại | Section | Điểm.

### 5. Exam Import – Import câu hỏi vào đề
- Trang "Câu hỏi trong đề thi" (tên đề + badge N3, JLPT_MOCK; nút [Quay lại danh sách]), gồm bảng câu hỏi đã gắn (# | Nội dung | Loại | Section | Điểm | ↑ ↓ ✕), và nổi bật card "Import câu hỏi từ Excel":
  - File chooser "Chọn file .xlsx", nút xanh lá [⬆ Import vào đề], nút viền xám [⬇ Tải file mẫu].
  - Dòng ghi chú: "File mẫu đề thi có thêm cột section và points. Câu hỏi được lưu vào ngân hàng (ACTIVE) và gắn luôn vào đề theo đúng thứ tự trong file."
- Thêm alert đỏ mẫu có danh sách lỗi:
  - "Dòng 5: section 'WRITING' không hợp lệ (LANGUAGE, READING, LISTENING)."
  - "Dòng 8: points phải là số nguyên >= 1."
- Và alert xanh mẫu: "Đã import và gắn 20 câu hỏi vào đề thi."

### 6. Exam Generation – Sinh đề thi tự động bằng AI (màn mới)
- Tiêu đề: "Sinh đề thi tự động" – mô tả "AI chọn câu hỏi từ ngân hàng theo cấu trúc đề JLPT, không tạo câu hỏi mới". Nút phải: [Quay lại].
- Card cấu hình: ComboBox "Đề thi đích" (chỉ đề DRAFT), ComboBox "Cấp độ JLPT", hiển thị chỉ đọc "Thời lượng / Điểm đạt" theo cấp độ.
- Bảng cấu trúc theo section: Section | Số câu | Điểm/câu | Độ khó (EASY/MEDIUM/HARD %) – 3 dòng LANGUAGE, READING, LISTENING, ô nhập số.
- Hàng thông tin: "Câu hỏi ACTIVE khả dụng: LANGUAGE 120 • READING 45 • LISTENING 60".
- Nút xanh to [✨ Sinh đề].
- Card kết quả xem trước: bảng # | Nội dung | Section | Độ khó | Điểm, có nút ✕ đổi câu khác ở mỗi dòng; cuối card: [Sinh lại] và [Lưu vào đề] (xanh).

---

## C. MODULE CÂU HỎI (menu active: "Câu hỏi")

### 7. Question List – Ngân hàng câu hỏi
- Tiêu đề: "Question Bank" – mô tả "Quản lý ngân hàng câu hỏi". Nút phải: [📗 Import Excel] viền xanh lá, [+ Thêm câu hỏi] xanh.
- Card lọc 1 hàng: TextBox "Tìm nội dung...", ComboBox Category, Loại (SINGLE_CHOICE/MULTI_CHOICE/PASSAGE), Level, Kỹ năng (VOCABULARY/GRAMMAR/KANJI/READING/LISTENING), Độ khó, T.thái, nút [Lọc], [Xoá].
- Card bảng, header card: "Tổng số: **248** câu hỏi".
- Bảng: ID | Nội dung (cắt ngắn) | Loại (badge: PASSAGE xanh nhạt, MULTI_CHOICE vàng, SINGLE_CHOICE xanh) | Level | Kỹ năng | Độ khó (badge) | Trạng thái (badge) | Sử dụng (badge "2E / 1Q" hoặc "-") | Thao tác [✏ Sửa].

### 8. Question Creation – Thêm câu hỏi
- Tiêu đề: "Thêm câu hỏi". Nút phải: [Quay lại].
- Card form:
  - Mục "Thông tin cơ bản": 5 ô trên 1 hàng – Loại câu hỏi *, JLPT Level, Kỹ năng, Độ khó, Trạng thái *.
  - "Nội dung câu hỏi *" – textarea 4 dòng (mẫu: "来週の旅行の____はもう全部終わりましたか。").
  - Hàng: "Category" ComboBox (1/3) | "Audio URL" TextBox (2/3).
  - Mục "Đáp án": lưới 2x2, mỗi ô là input group: [A] [ô nhập nội dung] [☐ Đúng]; tương tự B, C, D. Mẫu đánh dấu A đúng.
  - "Giải thích đáp án" – textarea 3 dòng.
  - Căn phải: [Huỷ], [Lưu câu hỏi].
- Ghi chú bên cạnh: khi chọn loại PASSAGE thì ẩn mục "Đáp án".

### 9. Question Editing – Sửa câu hỏi
- Như màn 8, tiêu đề "Sửa câu hỏi", dữ liệu điền sẵn. Nút phải thêm [🗑 Xóa] viền đỏ.
- Card trên form: "Đang sử dụng trong: 📄 **2** đề thi [1 đang hoạt động] ⚡ **1** quiz [1 đang hoạt động]" + badge vàng "⚠ Không thể archive".
- Biến thể PASSAGE (vẽ thêm phần dưới form): card "Câu hỏi con" với nút [+ Thêm câu con], bảng ID | Nội dung | Loại | Trạng thái | ✏.

### 10. Question Import – Import câu hỏi
- Màn 7 với panel import đang mở ngay dưới hàng tiêu đề:
  - File chooser .xlsx, nút xanh lá [⬆ Import], nút viền [⬇ Tải file mẫu].
  - Ghi chú: "Hỗ trợ câu đơn lẻ và bài đọc/nghe nhiều câu (cột group). Có lỗi ở bất kỳ dòng nào thì cả file không được lưu. Xem sheet "Huong dan" trong file mẫu."
- Alert đỏ mẫu kèm danh sách lỗi dòng ("Dòng 3: thiếu nội dung câu hỏi.", "Dòng 7: answer_key không hợp lệ.").

---

## D. MODULE QUIZ (menu active: "Bài quiz")

### 10b. Quiz List – Danh sách quiz
- Tiêu đề: "Quản lý Quiz" – mô tả "Quản lý bài quiz theo từng bài học". Nút phải: **[+ Tạo quiz]** (xanh).
- Card bộ lọc 1 hàng: TextBox "Tìm theo tên quiz..." (rộng ~40%), ComboBox "Tất cả bài học" (danh sách bài học, vd "Bài 1 – Chào hỏi", "Bài 2 – Gia đình"), ComboBox "Tất cả trạng thái" (DRAFT / PUBLISHED / ARCHIVED), nút [Lọc] viền xanh, nút [Xoá] viền xám.
- Card bảng, header card: "Tổng số: **12** quiz".
- Bảng: ID | Tên quiz (chữ đậm) | Bài học | Số câu | Đã gắn (badge xanh lá nếu > 0, vàng nếu = 0) | Điểm đạt (vd "60%") | Trạng thái (badge: PUBLISHED xanh lá, DRAFT vàng, ARCHIVED xám) | Thao tác (2 nút icon nhỏ: ✏ Sửa, ☑ Quản lý câu hỏi).
- Dữ liệu mẫu 4–5 dòng, ví dụ:
  - "1 | Trắc nghiệm: Bài 1 – Chào hỏi | Bài 1 – Chào hỏi | 5 | 5 | 60% | PUBLISHED"
  - "2 | Trắc nghiệm: Bài 2 – Gia đình | Bài 2 – Gia đình | 10 | 0 | 70% | DRAFT"
- Trạng thái rỗng: "Không tìm thấy quiz."

### 11. Quiz Details (View Quiz) – Xem quiz
Màn chỉ xem, đơn giản.
- Tiêu đề: "Chi tiết quiz". Nút phải: [Quay lại], [✏ Sửa] (xanh).
- Card thông tin dạng "nhãn: giá trị": Tên quiz: Trắc nghiệm Bài 1 – Chào hỏi | Bài học: Bài 1 – Chào hỏi | Trạng thái: badge PUBLISHED | Tổng số câu: 5 | Đã gắn: 5 | Điểm đạt: 60%.
- Card "Câu hỏi trong quiz (5)": bảng chỉ đọc ID | Nội dung | Loại | Level | Kỹ năng | Độ khó. Không có nút thao tác.

### 12. Quiz Creation – Tạo quiz
- Tiêu đề "Tạo quiz" – mô tả "Thông tin được lưu vào bảng exercises". Nút [Quay lại].
- Card form 2 cột:
  - Full: "Tên quiz *".
  - "Bài học *" ComboBox | "Trạng thái *" ComboBox.
  - "Tổng số câu hỏi *" | "Điểm đạt (%) *" (mẫu 60).
- Căn phải: [Huỷ], [Lưu quiz].

### 13. Quiz Editing – Sửa quiz
- Như màn 12, tiêu đề "Sửa cấu hình quiz", dữ liệu điền sẵn.
- Card trên form: "Câu hỏi đã gắn: **5**" + nút [☑ Quản lý câu hỏi].

### 14. Quiz Generation – Sinh quiz tự động bằng AI (màn mới)
- Tiêu đề "Sinh quiz tự động" – mô tả "AI chọn câu hỏi phù hợp nội dung bài học từ ngân hàng, không tạo câu hỏi mới".
- Card cấu hình: ComboBox "Quiz đích", ComboBox "Bài học" (hiển thị cấp độ khóa học), ô "Số câu" (mặc định 10), CheckBox nhóm kỹ năng (Từ vựng, Ngữ pháp, Kanji, Đọc, Nghe), thanh chọn độ khó (Dễ / Trung bình / Khó).
- Nút [✨ Sinh quiz].
- Card xem trước: bảng # | Nội dung | Kỹ năng | Độ khó | ✕ (đổi câu); cuối card [Sinh lại], [Lưu vào quiz].

---

## E. YÊU CẦU ĐẦU RA
1. 15 page, đặt tên page đúng theo tên tiếng Anh: Exam Creation, Exam List, Exam Details, Exam Editing, Exam Import, Exam Generation, Question List, Question Creation, Question Editing, Question Import, Quiz List, Quiz Details, Quiz Creation, Quiz Editing, Quiz Generation.
2. Sidebar + header dùng chung, có thể làm 1 page "Master Layout" rồi tham chiếu (background page) cho các page còn lại.
3. Dữ liệu mẫu trong bảng phải hợp lý (nội dung tiếng Nhật JLPT, cấp độ N5–N1).
4. Căn lề gọn, khoảng cách đều 16–24px, font sans-serif.
