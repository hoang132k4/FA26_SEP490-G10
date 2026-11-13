const {
  rectShape, textShape, badge, button, progressBar,
  realNavbar, realBreadcrumb
} = require('./common');

// 13. TAKE EXAM / VIRTUAL EXAM ROOM (take-exam.html)
function buildTakeExam() {
  const shapes = [];

  // 1. Kotoba Navigation Bar (Header)
  shapes.push(realNavbar({ activeMenu: 'mock-exam' }));

  // 2. Breadcrumb Navigation & Top Return Action (lines 9-35 of take-exam.html)
  shapes.push(realBreadcrumb(['Học tập', 'Khóa học JLPT N4', 'Bài thi đánh giá giữa kì (Midterm Exam)']));
  shapes.push(button({
    x: 1220, y: 76, w: 180, h: 32,
    text: '← Quay lại khóa học',
    variant: 'secondary',
    rx: 16, fontSize: 12
  }));

  // 3. Top Exam Focus Hero Banner (lines 38-89 of take-exam.html)
  // Banner background with deep Kotoba Navy
  shapes.push(rectShape({
    x: 40, y: 114, w: 1360, h: 124, rx: 18, ry: 18,
    fill: '#173B57', stroke: '#173B57'
  }));

  // Badges row inside Hero Banner
  // Exam type badge (Midterm)
  shapes.push(rectShape({
    x: 64, y: 130, w: 260, h: 26, rx: 13, ry: 13,
    fill: '#FEF3C7', stroke: '#FEF3C7'
  }));
  shapes.push(textShape({
    x: 74, y: 136, w: 240, h: 18,
    text: '★ Bài thi đánh giá giữa kì (Midterm)',
    size: 11, weight: 'bold', color: '#92400E'
  }));

  // JLPT Level badge
  shapes.push(rectShape({
    x: 334, y: 130, w: 110, h: 26, rx: 13, ry: 13,
    fill: '#244B6E', stroke: '#335E85'
  }));
  shapes.push(textShape({
    x: 344, y: 136, w: 90, h: 18,
    text: 'Trình độ: N4',
    size: 11, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5'
  }));

  // Focus Mode badge
  shapes.push(rectShape({
    x: 454, y: 130, w: 230, h: 26, rx: 13, ry: 13,
    fill: '#1E4768', stroke: '#2A5880'
  }));
  shapes.push(textShape({
    x: 464, y: 136, w: 210, h: 18,
    text: '🛡 Chế độ thi tập trung (Focus Mode)',
    size: 11, color: '#A7F3D0'
  }));

  // Banner Title & Subtitle
  shapes.push(textShape({
    x: 64, y: 164, w: 780, h: 30,
    text: 'Bài thi đánh giá năng lực giữa kì JLPT N4',
    size: 20, weight: 'bold', color: '#FFFFFF'
  }));
  shapes.push(textShape({
    x: 64, y: 198, w: 820, h: 22,
    text: 'Bài kiểm tra trắc nghiệm tiếng Nhật trực tuyến có tính giờ, kiểm tra toàn diện Từ vựng, Ngữ pháp, Đọc hiểu.',
    size: 12, color: '#CBD5E1'
  }));

  // Banner Countdown Timer Display Card (lines 75-87 of take-exam.html)
  shapes.push(rectShape({
    x: 1140, y: 128, w: 236, h: 96, rx: 16, ry: 16,
    fill: '#0F2744', stroke: '#335275'
  }));
  // Timer icon circle
  shapes.push(rectShape({
    x: 1156, y: 146, w: 42, h: 42, rx: 21, ry: 21,
    fill: '#1B3B5F', stroke: '#335275'
  }));
  shapes.push(textShape({
    x: 1156, y: 154, w: 42, h: 26,
    text: '⏱', size: 20, color: '#F59E0B', align: '0.5,0.5'
  }));
  // Timer text
  shapes.push(textShape({
    x: 1208, y: 140, w: 150, h: 18,
    text: 'Thời gian còn lại',
    size: 11, color: '#94A3B8'
  }));
  shapes.push(textShape({
    x: 1208, y: 158, w: 150, h: 38,
    text: '34:52',
    size: 30, weight: 'bold', color: '#FFFFFF'
  }));
  shapes.push(textShape({
    x: 1208, y: 198, w: 150, h: 16,
    text: '● Đang tính giờ thi',
    size: 10, color: '#34D399'
  }));

  // 4. Skill Group Filter Tabs Bar (lines 115-140 of take-exam.html)
  shapes.push(rectShape({
    x: 40, y: 248, w: 1360, h: 48, rx: 14, ry: 14,
    fill: '#FFFFFF', stroke: '#E2E8F0'
  }));
  // Tab 1: All questions (Active)
  shapes.push(rectShape({
    x: 48, y: 254, w: 220, h: 36, rx: 18, ry: 18,
    fill: '#173B57', stroke: '#173B57'
  }));
  shapes.push(textShape({
    x: 48, y: 263, w: 220, h: 20,
    text: '☵ Tất cả các câu (10)',
    size: 13, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5'
  }));
  // Tab 2: Vocab & Grammar
  shapes.push(rectShape({
    x: 278, y: 254, w: 230, h: 36, rx: 18, ry: 18,
    fill: '#F1F5F9', stroke: '#E2E8F0'
  }));
  shapes.push(textShape({
    x: 278, y: 263, w: 230, h: 20,
    text: '📖 Từ vựng • Ngữ pháp (6)',
    size: 13, color: '#475569', align: '0.5,0.5'
  }));
  // Tab 3: Reading
  shapes.push(rectShape({
    x: 518, y: 254, w: 170, h: 36, rx: 18, ry: 18,
    fill: '#F1F5F9', stroke: '#E2E8F0'
  }));
  shapes.push(textShape({
    x: 518, y: 263, w: 170, h: 20,
    text: '📑 Đọc hiểu (4)',
    size: 13, color: '#475569', align: '0.5,0.5'
  }));
  // Tab 4: Listening (0)
  shapes.push(rectShape({
    x: 698, y: 254, w: 170, h: 36, rx: 18, ry: 18,
    fill: '#F8FAFC', stroke: '#E2E8F0'
  }));
  shapes.push(textShape({
    x: 698, y: 263, w: 170, h: 20,
    text: '🎧 Nghe hiểu (0)',
    size: 13, color: '#94A3B8', align: '0.5,0.5'
  }));

  // =========================================================================
  // 5. Left Column: Questions List (lines 143-233 of take-exam.html)
  // =========================================================================

  // --- Question 1 Card ---
  shapes.push(rectShape({
    x: 40, y: 308, w: 890, h: 258, rx: 16, ry: 16,
    fill: '#FFFFFF', stroke: '#E2E8F0'
  }));
  // Question Header Row
  shapes.push(badge({
    x: 60, y: 326, text: 'Câu 1', bg: '#EBF4FA', color: '#173B57',
    w: 70, h: 26, rx: 13, fontSize: 12
  }));
  shapes.push(badge({
    x: 138, y: 326, text: '2.0 điểm', bg: '#F8FAFC', color: '#64748B',
    w: 80, h: 26, rx: 13, fontSize: 11
  }));
  shapes.push(badge({
    x: 226, y: 326, text: 'Từ vựng (Goi)', bg: '#FFF4E5', color: '#D97706',
    w: 120, h: 26, rx: 13, fontSize: 11
  }));

  // Flag Question Button (Not Flagged)
  shapes.push(rectShape({
    x: 810, y: 324, w: 100, h: 28, rx: 14, ry: 14,
    fill: '#FFFFFF', stroke: '#F59E0B'
  }));
  shapes.push(textShape({
    x: 810, y: 330, w: 100, h: 18,
    text: '⚑ Đặt cờ',
    size: 12, weight: 'bold', color: '#D97706', align: '0.5,0.5'
  }));

  // Divider inside Question 1
  shapes.push(rectShape({
    x: 60, y: 362, w: 850, h: 1, rx: 0, ry: 0,
    fill: '#F1F5F9', stroke: '#F1F5F9'
  }));

  // Question Content (Japanese text)
  shapes.push(textShape({
    x: 60, y: 374, w: 850, h: 28,
    text: 'きのう デパートで 新しい （　　） を 買いました。',
    size: 17, weight: 'bold', color: '#0F172A'
  }));

  // 4 Option Choices Grid (2 Columns x 2 Rows)
  // Option A (Selected)
  shapes.push(rectShape({
    x: 60, y: 414, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#EBF4FA', stroke: '#173B57', strokeW: 2
  }));
  shapes.push(textShape({
    x: 76, y: 426, w: 380, h: 22,
    text: '◉ A. くつ (giày dép)',
    size: 14, weight: 'bold', color: '#173B57'
  }));

  // Option B
  shapes.push(rectShape({
    x: 495, y: 414, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#CBD5E1'
  }));
  shapes.push(textShape({
    x: 511, y: 426, w: 380, h: 22,
    text: '○ B. くつを',
    size: 14, color: '#334155'
  }));

  // Option C
  shapes.push(rectShape({
    x: 60, y: 468, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#CBD5E1'
  }));
  shapes.push(textShape({
    x: 76, y: 480, w: 380, h: 22,
    text: '○ C. くつに',
    size: 14, color: '#334155'
  }));

  // Option D
  shapes.push(rectShape({
    x: 495, y: 468, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#CBD5E1'
  }));
  shapes.push(textShape({
    x: 511, y: 480, w: 380, h: 22,
    text: '○ D. くつで',
    size: 14, color: '#334155'
  }));

  // Question 1 auto-saved hint
  shapes.push(textShape({
    x: 60, y: 526, w: 850, h: 18,
    text: '✓ Đã chọn đáp án A • Tự động lưu trên hệ thống',
    size: 11, color: '#10B981'
  }));

  // --- Question 2 Card ---
  shapes.push(rectShape({
    x: 40, y: 578, w: 890, h: 258, rx: 16, ry: 16,
    fill: '#FFFFFF', stroke: '#E2E8F0'
  }));
  // Question Header Row
  shapes.push(badge({
    x: 60, y: 596, text: 'Câu 2', bg: '#EBF4FA', color: '#173B57',
    w: 70, h: 26, rx: 13, fontSize: 12
  }));
  shapes.push(badge({
    x: 138, y: 596, text: '2.0 điểm', bg: '#F8FAFC', color: '#64748B',
    w: 80, h: 26, rx: 13, fontSize: 11
  }));
  shapes.push(badge({
    x: 226, y: 596, text: 'Ngữ pháp (Bunpou)', bg: '#FFF4E5', color: '#D97706',
    w: 140, h: 26, rx: 13, fontSize: 11
  }));

  // Flag Question Button (ACTIVE FLAGGED)
  shapes.push(rectShape({
    x: 790, y: 594, w: 120, h: 28, rx: 14, ry: 14,
    fill: '#FEF3C7', stroke: '#F59E0B'
  }));
  shapes.push(textShape({
    x: 790, y: 600, w: 120, h: 18,
    text: '⚑ ĐÃ ĐẶT CỜ',
    size: 11, weight: 'bold', color: '#B45309', align: '0.5,0.5'
  }));

  // Divider inside Question 2
  shapes.push(rectShape({
    x: 60, y: 632, w: 850, h: 1, rx: 0, ry: 0,
    fill: '#F1F5F9', stroke: '#F1F5F9'
  }));

  // Question Content (Japanese text)
  shapes.push(textShape({
    x: 60, y: 644, w: 850, h: 28,
    text: '田中さんは いま 自分の部屋で 本を （　　） います。',
    size: 17, weight: 'bold', color: '#0F172A'
  }));

  // 4 Option Choices Grid (2 Columns x 2 Rows)
  // Option A
  shapes.push(rectShape({
    x: 60, y: 684, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#CBD5E1'
  }));
  shapes.push(textShape({
    x: 76, y: 696, w: 380, h: 22,
    text: '○ A. 読む (よむ)',
    size: 14, color: '#334155'
  }));

  // Option B
  shapes.push(rectShape({
    x: 495, y: 684, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#CBD5E1'
  }));
  shapes.push(textShape({
    x: 511, y: 696, w: 380, h: 22,
    text: '○ B. 読んだ (よんだ)',
    size: 14, color: '#334155'
  }));

  // Option C (Selected)
  shapes.push(rectShape({
    x: 60, y: 738, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#EBF4FA', stroke: '#173B57', strokeW: 2
  }));
  shapes.push(textShape({
    x: 76, y: 750, w: 380, h: 22,
    text: '◉ C. 読んで (よんで)',
    size: 14, weight: 'bold', color: '#173B57'
  }));

  // Option D
  shapes.push(rectShape({
    x: 495, y: 738, w: 415, h: 44, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#CBD5E1'
  }));
  shapes.push(textShape({
    x: 511, y: 750, w: 380, h: 22,
    text: '○ D. 読み (よみ)',
    size: 14, color: '#334155'
  }));

  // Question 2 flagged hint
  shapes.push(textShape({
    x: 60, y: 796, w: 850, h: 18,
    text: '⚑ Bạn đã đánh dấu câu hỏi này để xem lại trước khi nộp bài thi.',
    size: 11, color: '#D97706'
  }));

  // Bottom Notice Bar under Questions (lines 221-231 of take-exam.html)
  shapes.push(rectShape({
    x: 40, y: 848, w: 890, h: 38, rx: 10, ry: 10,
    fill: '#FFFFFF', stroke: '#E2E8F0'
  }));
  shapes.push(textShape({
    x: 56, y: 858, w: 850, h: 20,
    text: '☁ Lựa chọn của bạn được lưu tạm tự động trên trình duyệt • Không lo mất kết quả khi mạng chập chờn.',
    size: 11, color: '#64748B'
  }));

  // =========================================================================
  // 6. Right Column: Sticky Exam Palette & Controls (lines 236-302 of take-exam.html)
  // =========================================================================
  shapes.push(rectShape({
    x: 950, y: 308, w: 450, h: 578, rx: 18, ry: 18,
    fill: '#FFFFFF', stroke: '#E2E8F0'
  }));

  // Featured Sticky Countdown Timer Box (lines 241-253 of take-exam.html)
  shapes.push(rectShape({
    x: 970, y: 326, w: 410, h: 104, rx: 16, ry: 16,
    fill: '#0F2744', stroke: '#1B3B5F'
  }));
  // Timer header row
  shapes.push(textShape({
    x: 988, y: 338, w: 230, h: 18,
    text: '⏱ THỜI GIAN LÀM BÀI',
    size: 11, weight: 'bold', color: '#94A3B8'
  }));
  shapes.push(rectShape({
    x: 1300, y: 334, w: 64, h: 20, rx: 10, ry: 10,
    fill: '#244B6E', stroke: '#244B6E'
  }));
  shapes.push(textShape({
    x: 1300, y: 338, w: 64, h: 16,
    text: 'Đang đếm',
    size: 10, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5'
  }));

  // Big Monospace Countdown Display (34:52)
  shapes.push(textShape({
    x: 970, y: 356, w: 410, h: 46,
    text: '34:52',
    size: 36, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5'
  }));

  // Timer Progress Bar (Amber/Warning indicator)
  shapes.push(rectShape({
    x: 988, y: 412, w: 374, h: 5, rx: 2, ry: 2,
    fill: '#334E68', stroke: '#334E68'
  }));
  shapes.push(rectShape({
    x: 988, y: 412, w: 310, h: 5, rx: 2, ry: 2,
    fill: '#F59E0B', stroke: '#F59E0B'
  }));

  // Palette Header & Progress (lines 255-267 of take-exam.html)
  shapes.push(textShape({
    x: 970, y: 446, w: 200, h: 22,
    text: '☵ Bảng câu hỏi',
    size: 15, weight: 'bold', color: '#173B57'
  }));
  shapes.push(badge({
    x: 1290, y: 442, text: 'Đã làm: 2 / 10', bg: '#F1F5F9', color: '#334155',
    w: 90, h: 24, rx: 12, fontSize: 11
  }));

  // Overall Exam Progress Bar (20% done)
  shapes.push(progressBar({
    x: 970, y: 476, w: 410, percent: 20, fill: '#10B981', h: 6
  }));

  // Question Jump Grid (10 questions, lines 270-277 of take-exam.html)
  // Background card for grid
  shapes.push(rectShape({
    x: 970, y: 494, w: 410, h: 126, rx: 12, ry: 12,
    fill: '#F8FAFC', stroke: '#E2E8F0'
  }));

  // Row 1: Questions 1 - 5
  const qPaletteRow1 = [
    { num: '1', state: 'done' },      // Green answered
    { num: '2 ⚑', state: 'flagged' }, // Yellow flagged
    { num: '3', state: 'current' },   // Active blue outline
    { num: '4', state: 'empty' },     // Neutral
    { num: '5', state: 'empty' }      // Neutral
  ];

  qPaletteRow1.forEach((q, idx) => {
    const qx = 986 + idx * 78;
    const qy = 506;
    let bg = '#FFFFFF';
    let stroke = '#CBD5E1';
    let col = '#475569';
    let strokeW = 1;

    if (q.state === 'done') {
      bg = '#10B981'; stroke = '#10B981'; col = '#FFFFFF';
    } else if (q.state === 'flagged') {
      bg = '#F59E0B'; stroke = '#F59E0B'; col = '#FFFFFF';
    } else if (q.state === 'current') {
      bg = '#FFFFFF'; stroke = '#173B57'; strokeW = 2; col = '#173B57';
    }

    shapes.push(rectShape({
      x: qx, y: qy, w: 48, h: 48, rx: 24, ry: 24,
      fill: bg, stroke, strokeW
    }));
    shapes.push(textShape({
      x: qx, y: qy + 14, w: 48, h: 20,
      text: q.num, size: (q.num.length > 1 ? 12 : 14), weight: 'bold', color: col, align: '0.5,0.5'
    }));
  });

  // Row 2: Questions 6 - 10
  const qPaletteRow2 = [
    { num: '6', state: 'empty' },
    { num: '7', state: 'empty' },
    { num: '8', state: 'empty' },
    { num: '9', state: 'empty' },
    { num: '10', state: 'empty' }
  ];

  qPaletteRow2.forEach((q, idx) => {
    const qx = 986 + idx * 78;
    const qy = 562;
    shapes.push(rectShape({
      x: qx, y: qy, w: 48, h: 48, rx: 24, ry: 24,
      fill: '#FFFFFF', stroke: '#CBD5E1', strokeW: 1
    }));
    shapes.push(textShape({
      x: qx, y: qy + 14, w: 48, h: 20,
      text: q.num, size: 13, weight: 'bold', color: '#64748B', align: '0.5,0.5'
    }));
  });

  // Palette Legend (lines 280-293 of take-exam.html)
  shapes.push(rectShape({
    x: 970, y: 632, w: 410, h: 1, rx: 0, ry: 0,
    fill: '#E2E8F0', stroke: '#E2E8F0'
  }));

  // Legend items
  // 1. Done (Green)
  shapes.push(rectShape({
    x: 976, y: 644, w: 14, h: 14, rx: 7, ry: 7,
    fill: '#10B981', stroke: '#10B981'
  }));
  shapes.push(textShape({
    x: 996, y: 642, w: 90, h: 18,
    text: 'Đã làm (2)', size: 11, color: '#334155'
  }));

  // 2. Flagged (Amber)
  shapes.push(rectShape({
    x: 1096, y: 644, w: 14, h: 14, rx: 7, ry: 7,
    fill: '#F59E0B', stroke: '#F59E0B'
  }));
  shapes.push(textShape({
    x: 1116, y: 642, w: 110, h: 18,
    text: 'Đặt cờ xem lại (1)', size: 11, color: '#334155'
  }));

  // 3. Unanswered (Neutral)
  shapes.push(rectShape({
    x: 1242, y: 644, w: 14, h: 14, rx: 7, ry: 7,
    fill: '#FFFFFF', stroke: '#94A3B8'
  }));
  shapes.push(textShape({
    x: 1262, y: 642, w: 110, h: 18,
    text: 'Chưa làm (8)', size: 11, color: '#334155'
  }));

  // Exam Meta Information Box
  shapes.push(rectShape({
    x: 970, y: 672, w: 410, h: 56, rx: 12, ry: 12,
    fill: '#F1F5F9', stroke: '#E2E8F0'
  }));
  shapes.push(textShape({
    x: 986, y: 680, w: 380, h: 18,
    text: '🎯 Tổng số câu: 10 câu • Thời lượng: 35 phút',
    size: 11, weight: 'bold', color: '#173B57'
  }));
  shapes.push(textShape({
    x: 986, y: 700, w: 380, h: 18,
    text: '📊 Điểm đạt yêu cầu: 12.0 / 20.0 điểm (≥ 60%)',
    size: 11, color: '#475569'
  }));

  // Main Submit Action Button (lines 296-300 of take-exam.html)
  shapes.push(button({
    x: 970, y: 742, w: 410, h: 50,
    text: '✓ NỘP BÀI THI NGAY',
    variant: 'sakura',
    rx: 25, fontSize: 15
  }));
  shapes.push(textShape({
    x: 970, y: 800, w: 410, h: 18,
    text: '⏱ Hệ thống tự động thu bài khi hết 35 phút làm bài.',
    size: 11, color: '#64748B', align: '0.5,0.5'
  }));
  shapes.push(textShape({
    x: 970, y: 820, w: 410, h: 18,
    text: 'Có thể xem giải thích chi tiết đáp án ngay sau khi nộp.',
    size: 10, color: '#94A3B8', align: '0.5,0.5'
  }));

  return shapes.join('');
}

module.exports = {
  buildTakeExam
};

