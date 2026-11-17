const {
  rectShape, textShape, badge, button, progressBar,
  realNavbar, realBreadcrumb
} = require('./common');

// 9. FLASHCARD PRACTICE (MULTIPLE CHOICE TEST) (practice.html mode: test)
function buildMultipleChoiceTest() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'flashcards' }));

  // Header Nav
  shapes.push(textShape({ x: 40, y: 78, w: 400, h: 20, text: '‹ Trở về danh mục gói thẻ', size: 12, color: '#64748B' }));
  shapes.push(textShape({ x: 40, y: 100, w: 600, h: 28, text: 'Bộ Luyện Tập Từ Vựng Quizlet', size: 22, weight: 'bold', color: '#173B57' }));

  // Mode Switcher Bar with TAB 3 ACTIVE (lines 42-60 of practice.html)
  shapes.push(rectShape({ x: 40, y: 140, w: 1360, h: 56, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(rectShape({ x: 50, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 50, y: 158, w: 440, h: 22, text: '🎴 Thẻ ghi nhớ (Flashcards)', size: 14, weight: 'normal', color: '#64748B', align: '0.5,0.5' }));

  shapes.push(rectShape({ x: 500, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 500, y: 158, w: 440, h: 22, text: '🧩 Ghép thẻ (Match Game)', size: 14, weight: 'normal', color: '#64748B', align: '0.5,0.5' }));

  // Active Tab 3: Test
  shapes.push(rectShape({ x: 950, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#10B981', stroke: '#10B981' }));
  shapes.push(textShape({ x: 950, y: 158, w: 440, h: 22, text: '📝 Kiểm tra trắc nghiệm (Test)', size: 14, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  // Progress Bar Top (lines 70-80)
  shapes.push(rectShape({ x: 40, y: 210, w: 1360, h: 50, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 60, y: 224, w: 200, h: 20, text: 'Câu hỏi 7 / 20', size: 13, weight: 'bold', color: '#173B57' }));
  shapes.push(progressBar({ x: 260, y: 232, w: 800, percent: 35, fill: '#10B981', h: 8 }));
  shapes.push(badge({ x: 1100, y: 220, text: '✓ Đúng: 6', bg: '#D1FAE5', color: '#059669', w: 100, h: 26, rx: 13 }));
  shapes.push(badge({ x: 1220, y: 220, text: '✕ Sai: 0', bg: '#FEE2E2', color: '#DC2626', w: 90, h: 26, rx: 13 }));
  shapes.push(button({ x: 1325, y: 218, w: 65, h: 32, text: 'Nộp', variant: 'secondary' }));

  // Question Card (Test Mode)
  shapes.push(rectShape({ x: 250, y: 280, w: 940, h: 200, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#CBD5E1', strokeW: 2 }));
  shapes.push(badge({ x: 280, y: 304, text: 'TỪ VỰNG N4', bg: '#F1F5F9', color: '#475569', w: 110, h: 24, rx: 6 }));
  shapes.push(textShape({ x: 280, y: 340, w: 880, h: 24, text: 'Từ nào sau đây có nghĩa là:', size: 15, color: '#64748B' }));
  shapes.push(textShape({ x: 280, y: 372, w: 880, h: 42, text: '「 Mua sắm, việc đi mua hàng hóa 」', size: 28, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 280, y: 426, w: 880, h: 20, text: 'Gợi ý: Động từ 買う (mua) kết hợp với danh từ 物 (đồ vật)', size: 12, color: '#94A3B8' }));

  // 4 Options Grid (2 Columns x 2 Rows)
  const testOpts = [
    { key: 'A', text: '買い物 (かいもの)', sub: 'Đáp án đúng được bạn chọn', selected: true, correct: true, x: 250, y: 500 },
    { key: 'B', text: '食べ物 (たべもの)', sub: 'Thức ăn, đồ ăn', selected: false, correct: false, x: 740, y: 500 },
    { key: 'C', text: '飲み物 (のみもの)', sub: 'Đồ uống, nước giải khát', selected: false, correct: false, x: 250, y: 610 },
    { key: 'D', text: '着物 (きもの)', sub: 'Kimono, trang phục truyền thống', selected: false, correct: false, x: 740, y: 610 }
  ];

  testOpts.forEach(opt => {
    let fill = '#FFFFFF';
    let stroke = '#CBD5E1';
    let keyBg = '#F1F5F9';
    let keyCol = '#475569';

    if (opt.selected && opt.correct) {
      fill = '#ECFDF5';
      stroke = '#10B981';
      keyBg = '#10B981';
      keyCol = '#FFFFFF';
    }

    shapes.push(rectShape({ x: opt.x, y: opt.y, w: 450, h: 90, rx: 14, ry: 14, fill, stroke, strokeW: opt.selected ? 2 : 1 }));
    // Key Circle
    shapes.push(rectShape({ x: opt.x + 18, y: opt.y + 25, w: 40, h: 40, rx: 20, ry: 20, fill: keyBg, stroke: keyBg }));
    shapes.push(textShape({ x: opt.x + 18, y: opt.y + 35, w: 40, h: 20, text: opt.key, size: 16, weight: 'bold', color: keyCol, align: '0.5,0.5' }));

    shapes.push(textShape({ x: opt.x + 72, y: opt.y + 22, w: 360, h: 26, text: opt.text, size: 16, weight: 'bold', color: '#0F172A' }));
    shapes.push(textShape({ x: opt.x + 72, y: opt.y + 50, w: 360, h: 20, text: opt.sub, size: 12, color: opt.selected ? '#059669' : '#64748B' }));
  });

  // Bottom Feedback
  shapes.push(rectShape({ x: 250, y: 720, w: 940, h: 70, rx: 14, ry: 14, fill: '#D1FAE5', stroke: '#A7F3D0' }));
  shapes.push(textShape({ x: 276, y: 736, w: 600, h: 22, text: '✓ Chính xác! 買い物 nghĩa là việc mua sắm hàng hóa.', size: 14, weight: 'bold', color: '#065F46' }));
  shapes.push(textShape({ x: 276, y: 760, w: 600, h: 18, text: 'Ví dụ: 週末はスーパーへ買い物に行きます。(Cuối tuần tôi đi siêu thị mua sắm).', size: 12, color: '#047857' }));
  shapes.push(button({ x: 1010, y: 734, w: 160, h: 42, text: 'Câu tiếp theo ›', variant: 'success' }));

  return shapes.join('');
}

// 10. VOCABULARY WORD MATCH (practice.html mode: match)
function buildVocabularyWordMatch() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'flashcards' }));

  // Header Nav
  shapes.push(textShape({ x: 40, y: 78, w: 400, h: 20, text: '‹ Trở về danh mục gói thẻ', size: 12, color: '#64748B' }));
  shapes.push(textShape({ x: 40, y: 100, w: 600, h: 28, text: 'Bộ Luyện Tập Từ Vựng Quizlet', size: 22, weight: 'bold', color: '#173B57' }));

  // Mode Switcher Bar with TAB 2 ACTIVE (lines 42-60 of practice.html)
  shapes.push(rectShape({ x: 40, y: 140, w: 1360, h: 56, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(rectShape({ x: 50, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 50, y: 158, w: 440, h: 22, text: '🎴 Thẻ ghi nhớ (Flashcards)', size: 14, weight: 'normal', color: '#64748B', align: '0.5,0.5' }));

  // Active Tab 2: Match Game
  shapes.push(rectShape({ x: 500, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#F59E0B', stroke: '#F59E0B' }));
  shapes.push(textShape({ x: 500, y: 158, w: 440, h: 22, text: '🧩 Ghép thẻ (Match Game)', size: 14, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  shapes.push(rectShape({ x: 950, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 950, y: 158, w: 440, h: 22, text: '📝 Kiểm tra trắc nghiệm (Test)', size: 14, weight: 'normal', color: '#64748B', align: '0.5,0.5' }));

  // Match Game Timer & Instructions (lines 430-480)
  shapes.push(rectShape({ x: 40, y: 210, w: 1360, h: 60, rx: 14, ry: 14, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 68, y: 230, w: 600, h: 22, text: 'Nhiệm vụ: Nhấp ghép từng cặp thẻ Tiếng Nhật và Tiếng Việt tương ứng để làm biến mất thẻ', size: 14, weight: 'bold', color: '#173B57' }));
  shapes.push(badge({ x: 900, y: 226, text: '⏱ Thời gian: 00:28.4', bg: '#FEF3C7', color: '#B45309', w: 180, h: 28, rx: 14, fontSize: 12 }));
  shapes.push(badge({ x: 1100, y: 226, text: 'Đã ghép: 4 / 6 cặp', bg: '#D1FAE5', color: '#059669', w: 160, h: 28, rx: 14, fontSize: 12 }));
  shapes.push(button({ x: 1280, y: 222, w: 100, h: 36, text: 'Chơi lại', variant: 'secondary' }));

  // 12 Cards Grid for Match Game (4 cols x 3 rows) - Quizlet Grid Style
  const matchCards = [
    { text: '買い物', type: 'jp', status: 'matched', x: 120, y: 294 },
    { text: 'Nhân viên bán hàng', type: 'vn', status: 'normal', x: 420, y: 294 },
    { text: '店員 (てんいん)', type: 'jp', status: 'normal', x: 720, y: 294 },
    { text: 'Mua sắm', type: 'vn', status: 'matched', x: 1020, y: 294 },

    { text: '高い', type: 'jp', status: 'selected', x: 120, y: 440 },
    { text: 'Rẻ, giá thấp', type: 'vn', status: 'normal', x: 420, y: 440 },
    { text: '安い', type: 'jp', status: 'normal', x: 720, y: 440 },
    { text: 'Đắt tiền / Cao', type: 'vn', status: 'normal', x: 1020, y: 440 },

    { text: '領収書', type: 'jp', status: 'matched', x: 120, y: 586 },
    { text: 'Hóa đơn thanh toán', type: 'vn', status: 'matched', x: 420, y: 586 },
    { text: 'お勘定', type: 'jp', status: 'normal', x: 720, y: 586 },
    { text: 'Tính tiền, thanh toán', type: 'vn', status: 'normal', x: 1020, y: 586 }
  ];

  matchCards.forEach(mc => {
    let fill = '#FFFFFF';
    let stroke = '#CBD5E1';
    let textCol = '#0F172A';
    let opacity = 1;

    if (mc.status === 'matched') {
      fill = '#ECFDF5';
      stroke = '#10B981';
      textCol = '#059669';
    } else if (mc.status === 'selected') {
      fill = '#EFF6FF';
      stroke = '#3B82F6';
      textCol = '#1D4ED8';
    }

    shapes.push(rectShape({ x: mc.x, y: mc.y, w: 280, h: 126, rx: 16, ry: 16, fill, stroke, strokeW: mc.status === 'selected' ? 3 : (mc.status === 'matched' ? 2 : 1) }));
    shapes.push(textShape({ x: mc.x + 10, y: mc.y + 45, w: 260, h: 36, text: mc.text, size: mc.type === 'jp' ? 24 : 16, weight: 'bold', color: textCol, align: '0.5,0.5' }));
  });

  // Bottom Tip
  shapes.push(textShape({ x: 40, y: 740, w: 1360, h: 20, text: '💡 Cố gắng ghép nhanh nhất có thể để ghi tên vào Bảng xếp hạng kỷ lục tốc độ của bài học!', size: 12, color: '#64748B', align: '0.5,0.5' }));

  return shapes.join('');
}

// 11. QUIZ TAKING (quiz/do-quiz.html)
function buildQuizTaking() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'my-courses' }));
  shapes.push(realBreadcrumb(['Học tập', 'Tiếng Nhật N4', 'Bài 5: Mua sắm', 'Làm bài trắc nghiệm củng cố']));

  // Top Header Card (lines 23-60 of do-quiz.html)
  shapes.push(rectShape({ x: 40, y: 120, w: 1360, h: 110, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(rectShape({ x: 60, y: 136, w: 46, h: 46, rx: 23, ry: 23, fill: '#EFF6FF', stroke: '#EFF6FF' }));
  shapes.push(textShape({ x: 60, y: 148, w: 46, h: 24, text: '📝', size: 20, color: '#2563EB', align: '0.5,0.5' }));

  shapes.push(badge({ x: 120, y: 136, text: 'Bài tập trắc nghiệm', bg: '#2563EB', color: '#FFFFFF', w: 140, h: 24, rx: 12 }));
  shapes.push(badge({ x: 270, y: 136, text: 'N4', bg: '#173B57', color: '#FFFFFF', w: 46, h: 24, rx: 12 }));
  shapes.push(textShape({ x: 330, y: 140, w: 200, h: 20, text: '5 câu hỏi trắc nghiệm', size: 12, color: '#64748B' }));

  shapes.push(textShape({ x: 120, y: 166, w: 800, h: 26, text: 'Bài tập trắc nghiệm củng cố kiến thức: Bài 5 – Mua sắm và Giá cả', size: 17, weight: 'bold', color: '#0F172A' }));
  shapes.push(button({ x: 1220, y: 140, w: 160, h: 36, text: '← Quay lại bài giảng', variant: 'secondary' }));

  // Progress Bar inside Header (lines 50-59)
  shapes.push(textShape({ x: 60, y: 198, w: 400, h: 18, text: 'Tiến độ hoàn thành: 2/5 câu (40%)', size: 11, color: '#64748B' }));
  shapes.push(progressBar({ x: 300, y: 202, w: 1080, percent: 40, fill: '#2563EB', h: 8 }));

  // 2-Column: Left Questions (8 cols = 920px) | Right Palette (4 cols = 420px)
  // Question 1 Card (lines 68-140)
  shapes.push(rectShape({ x: 40, y: 250, w: 920, h: 260, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  // Question Header
  shapes.push(rectShape({ x: 60, y: 270, w: 32, h: 32, rx: 16, ry: 16, fill: '#2563EB', stroke: '#2563EB' }));
  shapes.push(textShape({ x: 60, y: 276, w: 32, h: 20, text: '1', size: 14, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));
  shapes.push(badge({ x: 104, y: 272, text: 'Từ vựng (Goi)', bg: '#F1F5F9', color: '#475569', w: 120, h: 26, rx: 13 }));

  shapes.push(textShape({ x: 60, y: 316, w: 860, h: 28, text: '「買い物」cách đọc Hiragana đúng của từ này là gì?', size: 16, weight: 'bold', color: '#0F172A' }));

  // 4 Radio choices for Q1
  shapes.push(rectShape({ x: 60, y: 356, w: 420, h: 44, rx: 8, ry: 8, fill: '#EFF6FF', stroke: '#3B82F6', strokeW: 2 }));
  shapes.push(textShape({ x: 80, y: 368, w: 380, h: 22, text: '◉ A. かいもの (kaimono)', size: 14, weight: 'bold', color: '#1D4ED8' }));

  shapes.push(rectShape({ x: 500, y: 356, w: 440, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 520, y: 368, w: 400, h: 22, text: '○ B. たべもの (tabemono)', size: 14, color: '#334155' }));

  shapes.push(rectShape({ x: 60, y: 412, w: 420, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 80, y: 424, w: 380, h: 22, text: '○ C. のみもの (nomimono)', size: 14, color: '#334155' }));

  shapes.push(rectShape({ x: 500, y: 412, w: 440, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 520, y: 424, w: 400, h: 22, text: '○ D. きもの (kimono)', size: 14, color: '#334155' }));

  // Question 2 Card
  shapes.push(rectShape({ x: 40, y: 530, w: 920, h: 260, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(rectShape({ x: 60, y: 550, w: 32, h: 32, rx: 16, ry: 16, fill: '#2563EB', stroke: '#2563EB' }));
  shapes.push(textShape({ x: 60, y: 556, w: 32, h: 20, text: '2', size: 14, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));
  shapes.push(badge({ x: 104, y: 552, text: 'Ngữ pháp (Bunpou)', bg: '#F1F5F9', color: '#475569', w: 140, h: 26, rx: 13 }));

  shapes.push(textShape({ x: 60, y: 596, w: 860, h: 28, text: 'Điền trợ từ thích hợp: 「このシャツ _____ いくらですか。」', size: 16, weight: 'bold', color: '#0F172A' }));

  shapes.push(rectShape({ x: 60, y: 636, w: 420, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 80, y: 648, w: 380, h: 22, text: '○ A. を', size: 14, color: '#334155' }));

  shapes.push(rectShape({ x: 500, y: 636, w: 440, h: 44, rx: 8, ry: 8, fill: '#EFF6FF', stroke: '#3B82F6', strokeW: 2 }));
  shapes.push(textShape({ x: 520, y: 648, w: 400, h: 22, text: '◉ B. は', size: 14, weight: 'bold', color: '#1D4ED8' }));

  shapes.push(rectShape({ x: 60, y: 692, w: 420, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 80, y: 704, w: 380, h: 22, text: '○ C. に', size: 14, color: '#334155' }));

  shapes.push(rectShape({ x: 500, y: 692, w: 440, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 520, y: 704, w: 400, h: 22, text: '○ D. で', size: 14, color: '#334155' }));

  // Right Column: Palette Card & Submit (lines 150-250)
  shapes.push(rectShape({ x: 980, y: 250, w: 420, h: 400, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 1004, y: 274, w: 370, h: 24, text: 'Mục lục câu hỏi', size: 16, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 1004, y: 300, w: 370, h: 18, text: 'Nhấn vào số câu để di chuyển nhanh', size: 12, color: '#64748B' }));

  // Palette 5 circles
  const qNums = [
    { num: 1, done: true }, { num: 2, done: true }, { num: 3, done: false },
    { num: 4, done: false }, { num: 5, done: false }
  ];

  qNums.forEach((qn, idx) => {
    const qx = 1004 + idx * 72;
    const qBg = qn.done ? '#2563EB' : '#F1F5F9';
    const qCol = qn.done ? '#FFFFFF' : '#475569';
    shapes.push(rectShape({ x: qx, y: 334, w: 52, h: 52, rx: 26, ry: 26, fill: qBg, stroke: qBg }));
    shapes.push(textShape({ x: qx, y: 348, w: 52, h: 24, text: '' + qn.num, size: 16, weight: 'bold', color: qCol, align: '0.5,0.5' }));
  });

  shapes.push(rectShape({ x: 1004, y: 414, w: 370, h: 1, rx: 0, ry: 0, fill: '#E2E8F0', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 1004, y: 430, w: 370, h: 20, text: 'Chú thích:', size: 12, weight: 'bold', color: '#475569' }));
  shapes.push(badge({ x: 1004, y: 456, text: 'Đã trả lời (2)', bg: '#2563EB', color: '#FFFFFF', w: 120, h: 24, rx: 12 }));
  shapes.push(badge({ x: 1140, y: 456, text: 'Chưa làm (3)', bg: '#F1F5F9', color: '#475569', w: 120, h: 24, rx: 12 }));

  // Big Submit Button
  shapes.push(button({ x: 1004, y: 530, w: 370, h: 54, text: 'NỘP BÀI TRẮC NGHIỆM', variant: 'sakura', fontSize: 16, rx: 27 }));
  shapes.push(textShape({ x: 1004, y: 596, w: 370, h: 20, text: '✓ Chấm điểm và xem kết quả ngay sau khi nộp', size: 11, color: '#64748B', align: '0.5,0.5' }));

  return shapes.join('');
}

// 12. QUIZ RESULTS (quiz/result.html)
function buildQuizResults() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'my-courses' }));
  shapes.push(realBreadcrumb(['Học tập', 'Tiếng Nhật N4', 'Bài 5: Mua sắm', 'Kết quả bài tập']));

  // Score Summary Banner Card (lines 23-66 of result.html)
  shapes.push(rectShape({ x: 40, y: 120, w: 1360, h: 240, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#E2E8F0' }));

  // Circle Score Display (lines 27-34: 170x170 green circle)
  shapes.push(rectShape({ x: 80, y: 155, w: 170, h: 170, rx: 85, ry: 85, fill: '#ECFDF5', stroke: '#10B981', strokeW: 3 }));
  shapes.push(textShape({ x: 80, y: 195, w: 170, h: 48, text: '80%', size: 40, weight: 'bold', color: '#059669', align: '0.5,0.5' }));
  shapes.push(textShape({ x: 80, y: 248, w: 170, h: 20, text: '4/5 CÂU ĐÚNG', size: 12, weight: 'bold', color: '#64748B', align: '0.5,0.5' }));

  // Message & Specs (lines 36-64)
  shapes.push(badge({ x: 290, y: 155, text: '✓ HOÀN THÀNH XUẤT SẮC', bg: '#10B981', color: '#FFFFFF', w: 200, h: 28, rx: 14, fontSize: 12 }));
  shapes.push(badge({ x: 500, y: 155, text: '17:30 30/10/2026', bg: '#F1F5F9', color: '#64748B', w: 140, h: 28, rx: 14, fontSize: 11 }));

  shapes.push(textShape({ x: 290, y: 194, w: 800, h: 32, text: 'Bài tập trắc nghiệm củng cố: Bài 5 – Mua sắm và Giá cả', size: 22, weight: 'bold', color: '#0F172A' }));
  shapes.push(textShape({ x: 290, y: 230, w: 800, h: 24, text: 'Chúc mừng bạn đã hoàn thành bài tập củng cố với kết quả xuất sắc! Bạn đã nắm chắc kiến thức trọng tâm.', size: 13, color: '#475569' }));

  // 3 Action Buttons (lines 53-62)
  shapes.push(button({ x: 290, y: 270, w: 180, h: 42, text: '⟳ Làm lại bài này', variant: 'outline-primary' }));
  shapes.push(button({ x: 485, y: 270, w: 180, h: 42, text: '← Quay lại bài học', variant: 'steel' }));
  shapes.push(button({ x: 680, y: 270, w: 180, h: 42, text: 'Trang học tập', variant: 'secondary' }));

  // Section: Detailed Question Review Header (lines 69-80)
  shapes.push(textShape({ x: 40, y: 390, w: 600, h: 26, text: '📋 Xem lại đáp án & Giải thích chi tiết', size: 17, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 40, y: 418, w: 600, h: 20, text: 'Xem lại từng câu hỏi, đáp án bạn đã chọn và ghi chú ngữ pháp chuyên sâu.', size: 13, color: '#64748B' }));
  shapes.push(badge({ x: 1240, y: 394, text: 'Đúng: 4 / Sai: 1', bg: '#F1F5F9', color: '#0F172A', w: 160, h: 28, rx: 14, fontSize: 12 }));

  // Review Item 1: Correct
  shapes.push(rectShape({ x: 40, y: 450, w: 1360, h: 104, rx: 14, ry: 14, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(badge({ x: 60, y: 466, text: 'CÂU 1', bg: '#F1F5F9', color: '#475569', w: 70, h: 24, rx: 12 }));
  shapes.push(badge({ x: 140, y: 466, text: '✓ ĐÚNG (+2.0 điểm)', bg: '#D1FAE5', color: '#059669', w: 150, h: 24, rx: 12 }));
  shapes.push(textShape({ x: 60, y: 500, w: 1100, h: 22, text: '「買い物」cách đọc Hiragana đúng là gì?  →  Bạn chọn: [A] かいもの (Chính xác)', size: 14, weight: 'bold', color: '#0F172A' }));
  shapes.push(textShape({ x: 60, y: 524, w: 1100, h: 18, text: 'Giải thích: 買い物 (kaimono) ghép từ 買い (mua) + 物 (đồ vật) tạo thành danh từ chỉ việc mua sắm.', size: 12, color: '#059669' }));

  // Review Item 2: Correct
  shapes.push(rectShape({ x: 40, y: 566, w: 1360, h: 104, rx: 14, ry: 14, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(badge({ x: 60, y: 582, text: 'CÂU 2', bg: '#F1F5F9', color: '#475569', w: 70, h: 24, rx: 12 }));
  shapes.push(badge({ x: 140, y: 582, text: '✓ ĐÚNG (+2.0 điểm)', bg: '#D1FAE5', color: '#059669', w: 150, h: 24, rx: 12 }));
  shapes.push(textShape({ x: 60, y: 616, w: 1100, h: 22, text: 'このシャツ _____ いくらですか。  →  Bạn chọn: [B] は (Chính xác)', size: 14, weight: 'bold', color: '#0F172A' }));
  shapes.push(textShape({ x: 60, y: 640, w: 1100, h: 18, text: 'Giải thích: Trợ từ 「は」được dùng để đánh dấu chủ ngữ/chủ đề được đề cập trong câu hỏi giá.', size: 12, color: '#059669' }));

  // Review Item 3: Wrong
  shapes.push(rectShape({ x: 40, y: 682, w: 1360, h: 116, rx: 14, ry: 14, fill: '#FEF2F2', stroke: '#FECACA' }));
  shapes.push(badge({ x: 60, y: 698, text: 'CÂU 3', bg: '#F1F5F9', color: '#475569', w: 70, h: 24, rx: 12 }));
  shapes.push(badge({ x: 140, y: 698, text: '✕ SAI (0 điểm)', bg: '#FEE2E2', color: '#DC2626', w: 110, h: 24, rx: 12 }));
  shapes.push(textShape({ x: 60, y: 732, w: 1100, h: 22, text: 'デパートは何時に開きますか。  →  Bạn chọn: [A] 午前9時 (Sai)', size: 14, weight: 'bold', color: '#991B1B' }));
  shapes.push(textShape({ x: 60, y: 756, w: 1100, h: 20, text: 'Đáp án đúng: [B] 午前10時  •  Giải thích: Trong bài nghe hội thoại, cửa hàng bách hóa mở cửa vào lúc 10 giờ sáng.', size: 13, weight: 'bold', color: '#059669' }));

  return shapes.join('');
}

module.exports = {
  buildMultipleChoiceTest,
  buildVocabularyWordMatch,
  buildQuizTaking,
  buildQuizResults
};
