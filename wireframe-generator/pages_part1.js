const {
  rectShape, textShape, badge, button, progressBar,
  realNavbar, realBreadcrumb
} = require('./common');

// 1. RECOMMENDED COURSES (recommended-courses.html)
function buildRecommendedCourses() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'courses' }));

  // AI Gradient Hero Banner (from lines 8-27 of recommended-courses.html)
  shapes.push(rectShape({ x: 40, y: 88, w: 1360, h: 140, rx: 20, ry: 20, fill: '#173B57', stroke: '#173B57' }));
  // AI Pill
  shapes.push(rectShape({ x: 68, y: 108, w: 220, h: 26, rx: 13, ry: 13, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 80, y: 112, w: 200, h: 18, text: '★ AI Personalization Engine', size: 12, weight: 'bold', color: '#173B57' }));
  // Title & Lead
  shapes.push(textShape({ x: 68, y: 142, w: 850, h: 36, text: 'Khóa học Đề xuất theo Năng lực của bạn', size: 24, weight: 'bold', color: '#FFFFFF' }));
  shapes.push(textShape({ x: 68, y: 182, w: 850, h: 24, text: 'Dựa trên kết quả đánh giá năng lực hiện tại (N5) và mục tiêu (N4), hệ thống đã lựa chọn lộ trình tối ưu nhất.', size: 13, color: '#CBD5E1' }));
  // Button View All
  shapes.push(button({ x: 1140, y: 135, w: 220, h: 44, text: 'Xem toàn bộ khóa học', variant: 'outline-sakura' }));

  // Section Header
  shapes.push(textShape({ x: 40, y: 248, w: 600, h: 26, text: '🏆 Khóa học phù hợp nhất với bạn', size: 18, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 40, y: 274, w: 600, h: 20, text: 'Các học phần được hệ thống AI đánh giá đạt tỷ lệ tương thích cao nhất', size: 13, color: '#64748B' }));

  // Recommended Cards Grid (2 Columns: lines 42-120 of recommended-courses.html)
  const items = [
    {
      x: 40, tag: 'Mục tiêu trọng tâm', match: '98%', level: 'N4', code: 'JLPT-N4',
      title: 'Khóa học Luyện thi JLPT N4 Toàn diện (Chương trình chuẩn)',
      reason: 'Bổ sung ngữ pháp sơ trung cấp và 300 chữ Hán trọng tâm còn thiếu qua bài test đầu vào.',
      lessons: '35 bài học', hours: '50 giờ đào tạo', skills: 'Từ vựng, Ngữ pháp, Đọc hiểu, Nghe hiểu',
      price: 'Miễn phí'
    },
    {
      x: 730, tag: 'Gợi ý nâng cao', match: '92%', level: 'N4-SPEED', code: 'N4-CUB',
      title: 'Cấp tốc Chinh phục Đọc hiểu & Nghe hiểu N4 qua 10 Đề thi thử',
      reason: 'Phù hợp với học viên chuẩn bị thi đợt tháng 12, tối ưu kỹ năng làm bài bấm giờ.',
      lessons: '25 bài học', hours: '40 giờ đào tạo', skills: 'Kỹ năng đọc dài, Nghe hội thoại thực tế',
      price: '299,000 đ'
    }
  ];

  items.forEach(it => {
    shapes.push(rectShape({ x: it.x, y: 304, w: 670, h: 360, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#CBD5E1', strokeW: 2 }));
    
    // Top badges
    shapes.push(badge({ x: it.x + 24, y: 324, text: it.tag, bg: '#FEE2E2', color: '#DC2626', w: 140, h: 26, rx: 13, fontSize: 11 }));
    shapes.push(badge({ x: it.x + 500, y: 324, text: '✓ Phù hợp ' + it.match, bg: '#D1FAE5', color: '#059669', w: 140, h: 26, rx: 13, fontSize: 11 }));

    // Course Title & Thumbnail
    shapes.push(rectShape({ x: it.x + 24, y: 364, w: 100, h: 75, rx: 8, ry: 8, fill: '#1E293B', stroke: '#1E293B' }));
    shapes.push(textShape({ x: it.x + 24, y: 390, w: 100, h: 24, text: 'JLPT', size: 16, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

    shapes.push(badge({ x: it.x + 140, y: 364, text: it.level, bg: '#173B57', color: '#FFFFFF', w: 70, h: 22, rx: 11, fontSize: 10 }));
    shapes.push(textShape({ x: it.x + 140, y: 390, w: 500, h: 26, text: it.title, size: 16, weight: 'bold', color: '#0F172A' }));
    shapes.push(textShape({ x: it.x + 140, y: 418, w: 300, h: 18, text: 'Mã khóa: ' + it.code, size: 12, color: '#64748B' }));

    // Match Reason Box
    shapes.push(rectShape({ x: it.x + 24, y: 450, w: 622, h: 60, rx: 8, ry: 8, fill: '#F8FAFC', stroke: '#E2E8F0' }));
    shapes.push(textShape({ x: it.x + 36, y: 458, w: 590, h: 20, text: '💡 Lý do đề xuất:', size: 12, weight: 'bold', color: '#173B57' }));
    shapes.push(textShape({ x: it.x + 36, y: 480, w: 590, h: 22, text: it.reason, size: 12, color: '#475569' }));

    // Specs
    shapes.push(textShape({ x: it.x + 24, y: 524, w: 622, h: 20, text: '📖 ' + it.lessons + '   •   ⏱ ' + it.hours + '   •   ✓ ' + it.skills, size: 12, color: '#64748B' }));

    // Price & CTA Row
    shapes.push(rectShape({ x: it.x + 24, y: 556, w: 622, h: 1, rx: 0, ry: 0, fill: '#E2E8F0', stroke: '#E2E8F0' }));
    shapes.push(textShape({ x: it.x + 24, y: 574, w: 100, h: 18, text: 'Học phí:', size: 11, color: '#64748B' }));
    shapes.push(textShape({ x: it.x + 24, y: 592, w: 200, h: 32, text: it.price, size: 22, weight: 'bold', color: '#173B57' }));

    shapes.push(button({ x: it.x + 380, y: 578, w: 120, h: 42, text: 'Chi tiết', variant: 'secondary' }));
    shapes.push(button({ x: it.x + 510, y: 578, w: 136, h: 42, text: 'ĐĂNG KÝ HỌC', variant: 'sakura' }));
  });

  return shapes.join('');
}

// 2. MY COURSES (my-courses.html)
function buildMyCourses() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'my-courses' }));

  // Header Title & Action (lines 8-19 of my-courses.html)
  shapes.push(badge({ x: 40, y: 88, text: 'Góc học tập cá nhân', bg: '#F1F5F9', color: '#64748B', w: 140, h: 24, rx: 12, fontSize: 11 }));
  shapes.push(textShape({ x: 40, y: 118, w: 600, h: 32, text: 'Khóa học của tôi', size: 26, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 40, y: 152, w: 600, h: 20, text: 'Theo dõi tiến độ và tiếp tục học tập các khóa học bạn đã đăng ký', size: 13, color: '#64748B' }));
  shapes.push(button({ x: 1200, y: 114, w: 200, h: 40, text: '+ Đăng ký thêm khóa học', variant: 'steel' }));

  // 3 Enrolled Course Cards in Row (lines 22-75 of my-courses.html)
  const courses = [
    {
      x: 40, level: 'N4', status: 'Đang học', title: 'Khóa học Tiếng Nhật N4 – Luyện thi JLPT Toàn diện',
      regDate: '23/10/2026', progress: 65, imgText: 'JLPT N4', hasExam: true
    },
    {
      x: 500, level: 'N3', status: 'Đang học', title: 'Tiếng Nhật Trung cấp N3 – Tăng tốc Đọc hiểu & Giao tiếp',
      regDate: '01/11/2026', progress: 30, imgText: 'JLPT N3', hasExam: false
    },
    {
      x: 960, level: 'N5', status: 'Hoàn thành', title: 'Tiếng Nhật N5 Nhập môn – Chinh phục Bảng chữ cái',
      regDate: '15/09/2026', progress: 100, imgText: 'JLPT N5', hasExam: false
    }
  ];

  courses.forEach(c => {
    shapes.push(rectShape({ x: c.x, y: 194, w: 440, h: 470, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
    
    // Thumbnail (180px height as coded in line 30 of my-courses.html)
    shapes.push(rectShape({ x: c.x, y: 194, w: 440, h: 180, rx: 16, ry: 16, fill: '#1E293B', stroke: '#1E293B' }));
    shapes.push(textShape({ x: c.x, y: 260, w: 440, h: 40, text: c.imgText, size: 28, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

    // Level Badge top-left & Status Badge top-right
    shapes.push(badge({ x: c.x + 16, y: 210, text: c.level, bg: '#173B57', color: '#FFFFFF', w: 50, h: 26, rx: 13 }));
    const isDone = (c.status === 'Hoàn thành');
    shapes.push(badge({ x: c.x + 300, y: 210, text: '✓ ' + c.status, bg: isDone ? '#10B981' : '#059669', color: '#FFFFFF', w: 120, h: 26, rx: 13 }));

    // Body
    shapes.push(textShape({ x: c.x + 20, y: 394, w: 400, h: 44, text: c.title, size: 16, weight: 'bold', color: '#0F172A' }));
    shapes.push(textShape({ x: c.x + 20, y: 444, w: 300, h: 20, text: 'Đăng ký ngày: ' + c.regDate, size: 12, color: '#64748B' }));

    // Progress Bar (lines 54-62 of my-courses.html)
    shapes.push(textShape({ x: c.x + 20, y: 478, w: 200, h: 20, text: 'Tiến độ hoàn thành:', size: 12, color: '#64748B' }));
    shapes.push(textShape({ x: c.x + 360, y: 478, w: 60, h: 20, text: c.progress + '%', size: 13, weight: 'bold', color: '#0F172A', align: '1,0' }));
    shapes.push(progressBar({ x: c.x + 20, y: 504, w: 400, percent: c.progress, fill: '#10B981', h: 8 }));

    // Footer buttons (lines 66-72 of my-courses.html)
    shapes.push(rectShape({ x: c.x + 20, y: 532, w: 400, h: 1, rx: 0, ry: 0, fill: '#F1F5F9', stroke: '#F1F5F9' }));
    shapes.push(button({ x: c.x + 20, y: 552, w: 340, h: 42, text: '▶ Xem bài học', variant: 'sakura' }));
    shapes.push(button({ x: c.x + 375, y: 552, w: 45, h: 42, text: '📁', variant: 'secondary' }));
  });

  return shapes.join('');
}

// 3. COURSE REGISTRATION (register-course.html)
function buildCourseRegistration() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'courses' }));
  shapes.push(realBreadcrumb(['Trang chủ', 'Khóa học JLPT', 'Tiếng Nhật N4', 'Xác nhận đăng ký']));

  // Back Button top right
  shapes.push(button({ x: 1170, y: 80, w: 230, h: 36, text: '← Quay lại chi tiết khóa học', variant: 'secondary' }));

  // Two Column Row: Left Form (7 cols = 820px) | Right Summary (5 cols = 500px)
  // Left Column (lines 27-140 of register-course.html)
  shapes.push(rectShape({ x: 40, y: 130, w: 820, h: 690, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#ECE7DE' }));

  // Header inside card
  shapes.push(rectShape({ x: 68, y: 154, w: 44, h: 44, rx: 22, ry: 22, fill: '#EBF4FA', stroke: '#EBF4FA' }));
  shapes.push(textShape({ x: 68, y: 164, w: 44, h: 24, text: '✓', size: 20, weight: 'bold', color: '#173B57', align: '0.5,0.5' }));
  shapes.push(textShape({ x: 124, y: 154, w: 600, h: 26, text: 'Xác Nhận Kích Hoạt Khóa Học', size: 18, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 124, y: 180, w: 600, h: 20, text: 'Khóa học sẽ được tích hợp ngay vào tài khoản học tập của bạn', size: 12, color: '#64748B' }));

  shapes.push(rectShape({ x: 68, y: 214, w: 764, h: 1, rx: 0, ry: 0, fill: '#F1EDE4', stroke: '#F1EDE4' }));

  // Step 1: Account Confirmation Box (lines 51-81)
  shapes.push(textShape({ x: 68, y: 232, w: 600, h: 20, text: 'TÀI KHOẢN HỌC VIÊN THỤ HƯỞNG', size: 12, weight: 'bold', color: '#64748B' }));
  shapes.push(rectShape({ x: 68, y: 260, w: 764, h: 76, rx: 10, ry: 10, fill: '#FAF8F5', stroke: '#EAE6DE' }));

  shapes.push(rectShape({ x: 88, y: 274, w: 48, h: 48, rx: 24, ry: 24, fill: '#173B57', stroke: '#173B57' }));
  shapes.push(textShape({ x: 88, y: 286, w: 48, h: 24, text: 'H', size: 18, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  shapes.push(textShape({ x: 148, y: 276, w: 300, h: 22, text: 'Nguyễn Văn Học Viên', size: 15, weight: 'bold', color: '#0F172A' }));
  shapes.push(badge({ x: 310, y: 276, text: 'N4', bg: '#F1F5F9', color: '#475569', w: 36, h: 20, rx: 6, fontSize: 10 }));
  shapes.push(textShape({ x: 148, y: 302, w: 300, h: 18, text: 'learner@jeps.edu.vn', size: 12, color: '#64748B' }));
  shapes.push(badge({ x: 670, y: 284, text: '✓ Đã đăng nhập', bg: '#D1FAE5', color: '#059669', w: 140, h: 28, rx: 14, fontSize: 11 }));

  // Step 2: Payment Methods (lines 84-140)
  shapes.push(textShape({ x: 68, y: 360, w: 600, h: 20, text: 'CHỌN PHƯƠNG THỨC THANH TOÁN', size: 12, weight: 'bold', color: '#64748B' }));

  // VNPay Box
  shapes.push(rectShape({ x: 68, y: 388, w: 764, h: 72, rx: 10, ry: 10, fill: '#FAFCFE', stroke: '#C8DBEE', strokeW: 2 }));
  shapes.push(rectShape({ x: 88, y: 412, w: 22, h: 22, rx: 11, ry: 11, fill: '#173B57', stroke: '#173B57' }));
  shapes.push(textShape({ x: 124, y: 402, w: 500, h: 24, text: 'Thanh toán qua VNPay (Quét mã QR / Mobile Banking / Thẻ ATM)', size: 13, weight: 'bold', color: '#0F172A' }));
  shapes.push(badge({ x: 620, y: 402, text: 'Khuyên dùng', bg: '#FEE2E2', color: '#DC2626', w: 90, h: 20, rx: 10, fontSize: 10 }));
  shapes.push(textShape({ x: 124, y: 428, w: 500, h: 18, text: 'Khóa học được mở khóa ngay lập tức trong tài khoản sau khi quét mã', size: 11, color: '#64748B' }));
  shapes.push(textShape({ x: 780, y: 410, w: 40, h: 28, text: '📱', size: 24, color: '#173B57' }));

  // Bank Transfer Box
  shapes.push(rectShape({ x: 68, y: 472, w: 764, h: 64, rx: 10, ry: 10, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(rectShape({ x: 88, y: 494, w: 22, h: 22, rx: 11, ry: 11, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 124, y: 486, w: 500, h: 22, text: 'Chuyển khoản ngân hàng trực tiếp (Kèm nội dung mã đơn)', size: 13, color: '#334155' }));
  shapes.push(textShape({ x: 124, y: 508, w: 500, h: 18, text: 'Duyệt thủ công trong vòng 15 phút sau khi chuyển tiền', size: 11, color: '#64748B' }));

  // Confirm Button
  shapes.push(button({ x: 68, y: 560, w: 764, h: 48, text: 'XÁC NHẬN ĐĂNG KÝ VÀ THANH TOÁN', variant: 'sakura', fontSize: 15 }));
  shapes.push(textShape({ x: 68, y: 620, w: 764, h: 20, text: '🔒 Bảo mật thanh toán SSL 256-bit • Hoàn tiền trong 7 ngày nếu không hài lòng', size: 11, color: '#94A3B8', align: '0.5,0.5' }));

  // Right Column: Course Summary Card (lines 142-240)
  shapes.push(rectShape({ x: 890, y: 130, w: 510, h: 690, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#ECE7DE' }));
  shapes.push(textShape({ x: 920, y: 154, w: 400, h: 24, text: 'Thông tin khóa học đăng ký', size: 16, weight: 'bold', color: '#173B57' }));
  
  shapes.push(rectShape({ x: 920, y: 194, w: 450, h: 180, rx: 12, ry: 12, fill: '#1E293B', stroke: '#1E293B' }));
  shapes.push(textShape({ x: 920, y: 260, w: 450, h: 40, text: 'JLPT N4', size: 32, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  shapes.push(badge({ x: 920, y: 390, text: 'N4', bg: '#173B57', color: '#FFFFFF', w: 46, h: 24, rx: 12 }));
  shapes.push(textShape({ x: 976, y: 390, w: 380, h: 24, text: 'Khóa học Tiếng Nhật N4 Chuẩn', size: 16, weight: 'bold', color: '#0F172A' }));

  shapes.push(textShape({ x: 920, y: 430, w: 450, h: 20, text: '📚 35 bài học lý thuyết & thực hành', size: 12, color: '#475569' }));
  shapes.push(textShape({ x: 920, y: 454, w: 450, h: 20, text: '📝 Bao gồm 1 bài thi Giữa kỳ + 1 bài thi Cuối kỳ', size: 12, color: '#475569' }));
  shapes.push(textShape({ x: 920, y: 478, w: 450, h: 20, text: '🎓 Chứng chỉ số hoàn thành khóa học JEPS', size: 12, color: '#475569' }));

  shapes.push(rectShape({ x: 920, y: 520, w: 450, h: 1, rx: 0, ry: 0, fill: '#F1EDE4', stroke: '#F1EDE4' }));
  shapes.push(textShape({ x: 920, y: 540, w: 200, h: 20, text: 'Học phí gốc:', size: 13, color: '#64748B' }));
  shapes.push(textShape({ x: 1250, y: 540, w: 120, h: 20, text: '599,000 đ', size: 13, color: '#94A3B8', align: '1,0' }));

  shapes.push(textShape({ x: 920, y: 570, w: 200, h: 20, text: 'Khuyến mãi học viên mới:', size: 13, color: '#DC2626' }));
  shapes.push(textShape({ x: 1250, y: 570, w: 120, h: 20, text: '- 300,000 đ', size: 13, weight: 'bold', color: '#DC2626', align: '1,0' }));

  shapes.push(textShape({ x: 920, y: 610, w: 200, h: 24, text: 'TỔNG THANH TOÁN:', size: 15, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 1200, y: 606, w: 170, h: 32, text: '299,000 đ', size: 24, weight: 'bold', color: '#173B57', align: '1,0' }));

  return shapes.join('');
}

// 4. LESSON STUDY WITH COURSERA GRADED ASSESSMENT HUB (lines 84-172 of lesson-study.html)
function buildLessonStudy() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'my-courses' }));
  shapes.push(realBreadcrumb(['Tổng quan', 'Khóa học Tiếng Nhật N4', 'Bài 5: Bài thi đánh giá Giữa kỳ (Midterm Exam)']));

  // View Syllabus Button top right
  shapes.push(button({ x: 1220, y: 80, w: 180, h: 36, text: '📋 Xem giáo trình', variant: 'secondary' }));

  // Main 2-Column: Left (8 cols = 920px) | Right (4 cols = 420px)
  // Left Column
  // Lesson Header Card (lines 31-82)
  shapes.push(rectShape({ x: 40, y: 124, w: 920, h: 104, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(badge({ x: 60, y: 140, text: 'BÀI THI ĐÁNH GIÁ (EXAM)', bg: '#DC2626', color: '#FFFFFF', w: 180, h: 26, rx: 13, fontSize: 11 }));
  shapes.push(badge({ x: 250, y: 140, text: 'N4', bg: '#173B57', color: '#FFFFFF', w: 46, h: 26, rx: 13, fontSize: 11 }));
  shapes.push(textShape({ x: 310, y: 144, w: 200, h: 20, text: '⏱ Thời lượng: 45 phút', size: 12, color: '#64748B' }));
  shapes.push(textShape({ x: 800, y: 144, w: 140, h: 20, text: 'Bài 5 / 12', size: 12, weight: 'bold', color: '#64748B', align: '1,0' }));

  shapes.push(textShape({ x: 60, y: 174, w: 860, h: 28, text: 'Bài 5: Bài Thi Đánh Giá Giữa Kỳ N4 (Midterm Graded Assessment)', size: 18, weight: 'bold', color: '#0F172A' }));
  shapes.push(textShape({ x: 60, y: 202, w: 860, h: 20, text: 'Chương 2: Đánh giá mốc năng lực và kiến thức từ vựng, ngữ pháp N4', size: 12, color: '#64748B' }));

  // COURSERA GRADED ASSESSMENT HUB (lines 85-172 of lesson-study.html)
  shapes.push(rectShape({ x: 40, y: 244, w: 920, h: 576, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#E2E8F0' }));

  // Hero Deep Navy Banner (lines 89-142)
  shapes.push(rectShape({ x: 40, y: 244, w: 920, h: 260, rx: 20, ry: 20, fill: '#0F2744', stroke: '#0F2744' }));
  shapes.push(badge({ x: 68, y: 268, text: 'GRADED ASSESSMENT • ĐÁNH GIÁ CÓ TÍNH ĐIỂM', bg: '#FEE2E2', color: '#991B1B', w: 320, h: 26, rx: 13, fontSize: 11 }));
  shapes.push(badge({ x: 400, y: 268, text: 'Khóa học: Tiếng Nhật N4', bg: '#1E3A8A', color: '#FFFFFF', w: 180, h: 26, rx: 13, fontSize: 11 }));
  shapes.push(badge({ x: 590, y: 268, text: 'Cấp độ: N4', bg: '#1E3A8A', color: '#FFFFFF', w: 100, h: 26, rx: 13, fontSize: 11 }));

  shapes.push(textShape({ x: 68, y: 304, w: 860, h: 36, text: 'Bài Thi Giữa Kỳ JLPT N4 – Đánh Giá Tiến Trình Học', size: 24, weight: 'bold', color: '#FFFFFF' }));
  shapes.push(textShape({ x: 68, y: 342, w: 860, h: 22, text: 'Bài thi bắt buộc trên phòng thi ảo. Kết quả được tính vào điều kiện hoàn thành khóa học.', size: 13, color: '#CBD5E1' }));

  // 4 Coursera Stat Tiles inside Navy Banner (lines 107-140)
  const tiles = [
    { x: 68, label: 'Tiêu chuẩn đạt', val: '60%', sub: 'Điểm tối thiểu' },
    { x: 280, label: 'Thời gian thi', val: '45 phút', sub: 'Tự động đếm ngược' },
    { x: 492, label: 'Định dạng đề', val: '25 câu', sub: 'Trắc nghiệm chuẩn' },
    { x: 704, label: 'Chế độ thi', val: 'Phòng thi ảo', sub: 'Focus Mode' }
  ];

  tiles.forEach(t => {
    shapes.push(rectShape({ x: t.x, y: 380, w: 200, h: 100, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
    shapes.push(textShape({ x: t.x + 10, y: 394, w: 180, h: 18, text: t.label, size: 11, color: '#64748B', align: '0.5,0.5' }));
    shapes.push(textShape({ x: t.x + 10, y: 416, w: 180, h: 30, text: t.val, size: 20, weight: 'bold', color: '#173B57', align: '0.5,0.5' }));
    shapes.push(textShape({ x: t.x + 10, y: 448, w: 180, h: 16, text: t.sub, size: 10, color: '#94A3B8', align: '0.5,0.5' }));
  });

  // Body Instructions (lines 144-156)
  shapes.push(textShape({ x: 68, y: 520, w: 860, h: 24, text: 'ℹ Hướng dẫn quy chế thi & Tiêu chuẩn hoàn thành', size: 15, weight: 'bold', color: '#0F172A' }));
  shapes.push(rectShape({ x: 68, y: 550, w: 864, h: 140, rx: 12, ry: 12, fill: '#F8FAFC', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 88, y: 564, w: 820, h: 116, text: '• Chuẩn bị: Đảm bảo đường truyền internet ổn định và không gian yên tĩnh trước khi bấm bắt đầu.\n• Quy trình làm bài: Hệ thống đưa bạn vào Phòng thi ảo chuyên dụng (tương tự như thi thử JLPT).\n• Thời gian: Đồng hồ đếm ngược tự động. Khi hết giờ, hệ thống sẽ tự thu bài và chấm điểm ngay.\n• Kết quả: Điểm số lưu vào CSDL học tập để tính điều kiện nhận chứng chỉ hoàn thành khóa học.', size: 12, color: '#475569' }));

  // BIG COURSERA CTA BUTTON (lines 158-170)
  shapes.push(button({ x: 260, y: 710, w: 480, h: 56, text: '🎓 VÀO PHÒNG THI ẢO NGAY ›', variant: 'sakura', fontSize: 17, rx: 28 }));
  shapes.push(textShape({ x: 260, y: 774, w: 480, h: 18, text: 'Nhấp để bắt đầu làm bài và tính giờ phòng thi', size: 11, color: '#64748B', align: '0.5,0.5' }));

  // Right Column: Lesson List Curriculum (420px)
  shapes.push(rectShape({ x: 980, y: 124, w: 420, h: 696, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 1004, y: 146, w: 370, h: 24, text: 'Giáo trình khóa học (12 bài)', size: 16, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 1004, y: 174, w: 370, h: 18, text: 'Tiến độ: 4/12 bài hoàn thành (33%)', size: 12, color: '#64748B' }));
  shapes.push(progressBar({ x: 1004, y: 196, w: 370, percent: 33, fill: '#10B981', h: 6 }));

  const lessonsRight = [
    { title: 'Bài 1: Từ vựng & Kanji N4 Nhập môn', done: true },
    { title: 'Bài 2: Ngữ pháp mẫu câu biểu thị nguyên nhân', done: true },
    { title: 'Bài 3: Đọc hiểu văn bản ngắn thông báo', done: true },
    { title: 'Bài 4: Luyện nghe hội thoại giao tiếp', done: true },
    { title: 'Bài 5: BÀI THI GIỮA KỲ (ĐANG HỌC)', current: true, isExam: true },
    { title: 'Bài 6: Ngữ pháp thể sai khiến & bị động', done: false },
    { title: 'Bài 7: Từ vựng chủ đề công sở & phỏng vấn', done: false },
    { title: 'Bài 8: Đọc hiểu văn bản giải thích', done: false },
    { title: 'Bài 9: Luyện nghe hiểu có hình ảnh', done: false },
    { title: 'Bài 10: BÀI THI CUỐI KỲ (FINAL EXAM)', isExam: true, done: false }
  ];

  lessonsRight.forEach((lr, idx) => {
    const ly = 216 + idx * 56;
    let fill = '#FFFFFF';
    let stroke = '#E2E8F0';
    let textCol = '#334155';

    if (lr.current) {
      fill = '#FEF2F2'; stroke = '#EF4444'; textCol = '#DC2626';
    } else if (lr.isExam) {
      fill = '#FFF7ED'; stroke = '#F97316'; textCol = '#C2410C';
    } else if (lr.done) {
      textCol = '#059669';
    }

    shapes.push(rectShape({ x: 1000, y: ly, w: 380, h: 48, rx: 8, ry: 8, fill, stroke, strokeW: lr.current ? 2 : 1 }));
    const mark = lr.done ? '✓ ' : (lr.current ? '▶ ' : (lr.isExam ? '📝 ' : '○ '));
    shapes.push(textShape({ x: 1012, y: ly + 14, w: 356, h: 20, text: mark + lr.title, size: 12, weight: lr.current || lr.isExam ? 'bold' : 'normal', color: textCol }));
  });

  return shapes.join('');
}

module.exports = {
  buildRecommendedCourses,
  buildMyCourses,
  buildCourseRegistration,
  buildLessonStudy
};
