const {
  rectShape, textShape, badge, button, progressBar,
  realNavbar, realBreadcrumb
} = require('./common');

// 5. FLASHCARD LIST (flashcard/list.html)
function buildFlashcardList() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'flashcards' }));

  // Header row (lines 9-28 of list.html)
  shapes.push(realBreadcrumb(['Học tập', 'Gói Flashcard từ vựng']));
  shapes.push(textShape({ x: 40, y: 114, w: 600, h: 32, text: 'Gói Flashcard Từ Vựng', size: 24, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 40, y: 148, w: 700, h: 20, text: 'Học từ vựng theo giáo trình chuẩn JLPT và tự quản lý sổ tay từ vựng cá nhân.', size: 13, color: '#64748B' }));
  shapes.push(button({ x: 1210, y: 114, w: 190, h: 42, text: '+ Tạo gói thẻ mới', variant: 'sakura' }));

  // Mini Stats Summary Bar (4 Cards: lines 31-76 of list.html)
  const stats = [
    { x: 40, label: 'Gói hệ thống', val: '6 Gói', iconBg: '#EBF4FA', iconCol: '#173B57' },
    { x: 390, label: 'Gói cá nhân', val: '3 Gói', iconBg: '#FEE2E2', iconCol: '#DC2626' },
    { x: 740, label: 'Tổng từ vựng', val: '320 Từ', iconBg: '#F1F5F9', iconCol: '#475569' },
    { x: 1090, label: 'Đã thuộc lòng', val: '185 Từ', iconBg: '#D1FAE5', iconCol: '#059669' }
  ];

  stats.forEach(s => {
    shapes.push(rectShape({ x: s.x, y: 184, w: 310, h: 86, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
    shapes.push(textShape({ x: s.x + 20, y: 200, w: 180, h: 20, text: s.label, size: 12, color: '#64748B' }));
    shapes.push(textShape({ x: s.x + 20, y: 226, w: 180, h: 32, text: s.val, size: 22, weight: 'bold', color: '#0F172A' }));
    shapes.push(rectShape({ x: s.x + 240, y: 206, w: 46, h: 46, rx: 23, ry: 23, fill: s.iconBg, stroke: s.iconBg }));
    shapes.push(textShape({ x: s.x + 240, y: 216, w: 46, h: 26, text: '★', size: 18, color: s.iconCol, align: '0.5,0.5' }));
  });

  // Toolbar Card: Tabs (Hệ thống / Cá nhân) + Search (lines 79-115)
  shapes.push(rectShape({ x: 40, y: 288, w: 1360, h: 64, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  // Tabs
  shapes.push(button({ x: 56, y: 300, w: 180, h: 40, text: '🏛 Gói hệ thống (6)', variant: 'steel' }));
  shapes.push(button({ x: 246, y: 300, w: 180, h: 40, text: '👤 Gói cá nhân (3)', variant: 'secondary' }));

  // Search input
  shapes.push(rectShape({ x: 800, y: 300, w: 380, h: 40, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 816, y: 310, w: 350, h: 20, text: 'Tìm kiếm gói thẻ, từ vựng...', size: 13, color: '#94A3B8' }));
  shapes.push(button({ x: 1195, y: 300, w: 185, h: 40, text: 'Luyện tập Quizlet', variant: 'sakura' }));

  // Grid of 6 Flashcard Decks (lines 120-220)
  const decks = [
    { x: 40, y: 372, level: 'N4', title: 'N4 – Từ vựng Mua sắm & Tiền tệ', total: '45 từ', master: '28 từ', pct: 62 },
    { x: 500, y: 372, level: 'N4', title: 'N4 – Động từ Thể sai khiến & Bị động', total: '38 từ', master: '15 từ', pct: 39 },
    { x: 960, y: 372, level: 'N5', title: 'N5 – 100 Từ vựng Gia đình & Nghề nghiệp', total: '50 từ', master: '48 từ', pct: 96 },
    { x: 40, y: 588, level: 'N3', title: 'N3 – Phó từ quan trọng trong kỳ thi JLPT', total: '60 từ', master: '12 từ', pct: 20 },
    { x: 500, y: 588, level: 'N4', title: 'N4 – Kanji Chủ đề Nhà hàng & Ăn uống', total: '30 từ', master: '18 từ', pct: 60 },
    { x: 960, y: 588, level: 'N4', title: 'N4 – Tính từ đuôi い và đuôi な thông dụng', total: '40 từ', master: '25 từ', pct: 63 }
  ];

  decks.forEach(d => {
    shapes.push(rectShape({ x: d.x, y: d.y, w: 440, h: 196, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
    
    // Header
    shapes.push(badge({ x: d.x + 20, y: d.y + 18, text: d.level, bg: '#173B57', color: '#FFFFFF', w: 46, h: 24, rx: 12 }));
    shapes.push(textShape({ x: d.x + 76, y: d.y + 20, w: 200, h: 20, text: d.total + ' vựng', size: 12, color: '#64748B' }));
    shapes.push(badge({ x: d.x + 320, y: d.y + 18, text: 'Đã thuộc ' + d.pct + '%', bg: '#D1FAE5', color: '#059669', w: 100, h: 24, rx: 12 }));

    // Title
    shapes.push(textShape({ x: d.x + 20, y: d.y + 54, w: 400, h: 24, text: d.title, size: 15, weight: 'bold', color: '#0F172A' }));
    
    // Progress bar
    shapes.push(progressBar({ x: d.x + 20, y: d.y + 88, w: 400, percent: d.pct, fill: '#10B981', h: 6 }));
    shapes.push(textShape({ x: d.x + 20, y: d.y + 100, w: 400, h: 18, text: 'Đã thuộc ' + d.master + ' / ' + d.total, size: 11, color: '#64748B' }));

    // Footer actions
    shapes.push(rectShape({ x: d.x + 20, y: d.y + 128, w: 400, h: 1, rx: 0, ry: 0, fill: '#F1F5F9', stroke: '#F1F5F9' }));
    shapes.push(button({ x: d.x + 20, y: d.y + 144, w: 190, h: 36, text: 'Chi tiết gói', variant: 'secondary' }));
    shapes.push(button({ x: d.x + 230, y: d.y + 144, w: 190, h: 36, text: 'Luyện tập ngay', variant: 'sakura' }));
  });

  return shapes.join('');
}

// 6. FLASHCARD DETAIL (flashcard/detail.html)
function buildFlashcardDetail() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'flashcards' }));
  shapes.push(realBreadcrumb(['Học tập', 'Flashcard', '桜 (Hoa anh đào)']));

  // Top action bar (lines 22-33 of detail.html)
  shapes.push(textShape({ x: 40, y: 114, w: 600, h: 30, text: 'Chi Tiết Thẻ Flashcard', size: 22, weight: 'bold', color: '#173B57' }));
  shapes.push(button({ x: 990, y: 110, w: 120, h: 36, text: '← Quay lại', variant: 'secondary' }));
  shapes.push(button({ x: 1120, y: 110, w: 130, h: 36, text: '✏ Chỉnh sửa', variant: 'outline-primary' }));
  shapes.push(button({ x: 1260, y: 110, w: 140, h: 36, text: '🗑 Xóa thẻ', variant: 'outline-danger' }));

  // Main 2-Column (lines 35-150): Left 3D Flip Card (col-6 = 670px) | Right Info (col-6 = 670px)
  // Left Column: Interactive 3D Card
  shapes.push(rectShape({ x: 40, y: 164, w: 660, h: 560, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(badge({ x: 68, y: 188, text: 'N4', bg: '#173B57', color: '#FFFFFF', w: 46, h: 26, rx: 13 }));
  shapes.push(badge({ x: 570, y: 188, text: 'ĐÃ THUỘC', bg: '#D1FAE5', color: '#059669', w: 100, h: 26, rx: 13 }));

  // 3D Card Inner (lines 45-68)
  shapes.push(rectShape({ x: 80, y: 236, w: 580, h: 380, rx: 20, ry: 20, fill: '#F8FAFC', stroke: '#CBD5E1', strokeW: 2 }));
  shapes.push(badge({ x: 104, y: 256, text: 'Mặt trước (Tiếng Nhật)', bg: '#E2E8F0', color: '#475569', w: 160, h: 24, rx: 6, fontSize: 11 }));

  // Big Kanji Center
  shapes.push(textShape({ x: 80, y: 340, w: 580, h: 80, text: '桜', size: 72, weight: 'bold', color: '#173B57', align: '0.5,0.5' }));
  shapes.push(textShape({ x: 80, y: 436, w: 580, h: 30, text: 'さくら (sakura)', size: 24, color: '#64748B', align: '0.5,0.5' }));

  shapes.push(textShape({ x: 80, y: 550, w: 580, h: 20, text: '🔄 Nhấp để lật xem nghĩa tiếng Việt & ví dụ', size: 12, color: '#E06D53', align: '0.5,0.5' }));

  // TTS Pronounce button (lines 71-76)
  shapes.push(button({ x: 220, y: 640, w: 300, h: 44, text: '🔊 Nghe phát âm tiếng Nhật', variant: 'outline-sakura' }));

  // Right Column: Metadata & Detailed Info (lines 80-180)
  shapes.push(rectShape({ x: 740, y: 164, w: 660, h: 560, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 768, y: 188, w: 500, h: 24, text: 'Thông tin học tập chi tiết', size: 16, weight: 'bold', color: '#173B57' }));
  shapes.push(rectShape({ x: 768, y: 220, w: 604, h: 1, rx: 0, ry: 0, fill: '#F1F5F9', stroke: '#F1F5F9' }));

  const metaRows = [
    { label: 'Từ vựng / Kanji:', val: '桜' },
    { label: 'Cách đọc (Furigana):', val: 'さくら (sakura)' },
    { label: 'Hán Việt:', val: 'ANH (Hoa anh đào)' },
    { label: 'Ý nghĩa tiếng Việt:', val: 'Hoa anh đào (biểu tượng mùa xuân Nhật Bản)' },
    { label: 'Từ loại:', val: 'Danh từ (Noun)' },
    { label: 'Cấp độ JLPT:', val: 'N4 (Sơ trung cấp)' },
    { label: 'Gói từ vựng:', val: 'N4 – Từ vựng Mùa xuân & Thiên nhiên' }
  ];

  metaRows.forEach((mr, idx) => {
    const ry = 236 + idx * 40;
    shapes.push(textShape({ x: 768, y: ry, w: 200, h: 22, text: mr.label, size: 13, color: '#64748B' }));
    shapes.push(textShape({ x: 970, y: ry, w: 400, h: 22, text: mr.val, size: 13, weight: 'bold', color: '#0F172A' }));
  });

  // Example sentence box
  shapes.push(rectShape({ x: 768, y: 530, w: 604, h: 90, rx: 12, ry: 12, fill: '#F8FAFC', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 788, y: 544, w: 560, h: 20, text: '📝 Câu ví dụ mẫu (例文):', size: 12, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 788, y: 568, w: 560, h: 24, text: '春になると、公園の桜がとても綺麗に咲きます。', size: 14, color: '#0F172A' }));
  shapes.push(textShape({ x: 788, y: 592, w: 560, h: 20, text: '(Khi mùa xuân đến, hoa anh đào trong công viên nở rất đẹp).', size: 12, color: '#64748B' }));

  shapes.push(button({ x: 768, y: 640, w: 604, h: 44, text: 'Luyện tập thẻ này với Quizlet Engine ›', variant: 'steel' }));

  return shapes.join('');
}

// 7. CREATE FLASHCARD (flashcard/form.html)
function buildCreateFlashcard() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'flashcards' }));
  shapes.push(realBreadcrumb(['Học tập', 'Flashcard', 'Tạo gói thẻ mới']));

  // Header (lines 18-29 of form.html)
  shapes.push(textShape({ x: 40, y: 114, w: 600, h: 30, text: 'Tạo gói thẻ Flashcard mới', size: 22, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 40, y: 146, w: 600, h: 20, text: 'Đặt tên gói, chọn cấp độ JLPT rồi thêm từ vựng vào gói.', size: 13, color: '#64748B' }));
  shapes.push(button({ x: 1280, y: 110, w: 120, h: 36, text: '✕ Huỷ bỏ', variant: 'secondary' }));

  // Form Card (lines 35-120 of form.html)
  shapes.push(rectShape({ x: 40, y: 180, w: 1360, h: 680, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#E2E8F0' }));

  // Field: Tên gói từ vựng (8 cols = 860px) + Cấp độ (4 cols = 420px)
  shapes.push(textShape({ x: 70, y: 210, w: 400, h: 20, text: 'Tên gói từ vựng *', size: 13, weight: 'bold', color: '#0F172A' }));
  shapes.push(rectShape({ x: 70, y: 236, w: 860, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 86, y: 248, w: 800, h: 20, text: 'Ví dụ: Sổ tay từ vựng N4 Bài 6 – Ẩm thực & Nhà hàng', size: 13, color: '#94A3B8' }));

  shapes.push(textShape({ x: 960, y: 210, w: 400, h: 20, text: 'Cấp độ JLPT *', size: 13, weight: 'bold', color: '#0F172A' }));
  shapes.push(rectShape({ x: 960, y: 236, w: 410, h: 44, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 976, y: 248, w: 350, h: 20, text: 'N4 (Sơ trung cấp) ▾', size: 13, color: '#0F172A' }));

  // Word input section divider
  shapes.push(rectShape({ x: 70, y: 310, w: 1300, h: 1, rx: 0, ry: 0, fill: '#E2E8F0', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 70, y: 330, w: 500, h: 24, text: 'Danh sách thẻ từ vựng trong gói (Nhập liệu trực tiếp)', size: 15, weight: 'bold', color: '#173B57' }));

  // Dynamic Word Rows Header
  shapes.push(rectShape({ x: 70, y: 366, w: 1300, h: 40, rx: 8, ry: 8, fill: '#F8FAFC', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 90, y: 378, w: 40, h: 18, text: '#', size: 12, weight: 'bold', color: '#64748B' }));
  shapes.push(textShape({ x: 140, y: 378, w: 260, h: 18, text: 'TỪ VỰNG / KANJI *', size: 12, weight: 'bold', color: '#64748B' }));
  shapes.push(textShape({ x: 420, y: 378, w: 220, h: 18, text: 'CÁCH ĐỌC (FURIGANA)', size: 12, weight: 'bold', color: '#64748B' }));
  shapes.push(textShape({ x: 660, y: 378, w: 320, h: 18, text: 'Ý NGHĨA TIẾNG VIỆT *', size: 12, weight: 'bold', color: '#64748B' }));
  shapes.push(textShape({ x: 1000, y: 378, w: 300, h: 18, text: 'CÂU VÍ DỤ MINH HỌA', size: 12, weight: 'bold', color: '#64748B' }));

  // Row 1
  shapes.push(rectShape({ x: 70, y: 416, w: 1300, h: 54, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 90, y: 432, w: 40, h: 20, text: '1', size: 13, color: '#64748B' }));
  shapes.push(rectShape({ x: 130, y: 424, w: 260, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 142, y: 434, w: 230, h: 20, text: '料理', size: 14, weight: 'bold', color: '#0F172A' }));

  shapes.push(rectShape({ x: 410, y: 424, w: 220, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 422, y: 434, w: 190, h: 20, text: 'りょうり', size: 13, color: '#475569' }));

  shapes.push(rectShape({ x: 650, y: 424, w: 320, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 662, y: 434, w: 290, h: 20, text: 'Món ăn, nấu nướng ẩm thực', size: 13, color: '#0F172A' }));

  shapes.push(rectShape({ x: 990, y: 424, w: 320, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 1002, y: 434, w: 290, h: 20, text: '日本料理が好きです。', size: 13, color: '#64748B' }));

  shapes.push(button({ x: 1320, y: 424, w: 40, h: 38, text: '✕', variant: 'outline-danger', rx: 6 }));

  // Row 2
  shapes.push(rectShape({ x: 70, y: 480, w: 1300, h: 54, rx: 8, ry: 8, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 90, y: 496, w: 40, h: 20, text: '2', size: 13, color: '#64748B' }));
  shapes.push(rectShape({ x: 130, y: 488, w: 260, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 142, y: 498, w: 230, h: 20, text: '注文する', size: 14, weight: 'bold', color: '#0F172A' }));

  shapes.push(rectShape({ x: 410, y: 488, w: 220, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 422, y: 498, w: 190, h: 20, text: 'ちゅうもんする', size: 13, color: '#475569' }));

  shapes.push(rectShape({ x: 650, y: 488, w: 320, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 662, y: 498, w: 290, h: 20, text: 'Gọi món, đặt món tại nhà hàng', size: 13, color: '#0F172A' }));

  shapes.push(rectShape({ x: 990, y: 488, w: 320, h: 38, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 1002, y: 498, w: 290, h: 20, text: 'ラーメンを注文しました。', size: 13, color: '#64748B' }));

  shapes.push(button({ x: 1320, y: 488, w: 40, h: 38, text: '✕', variant: 'outline-danger', rx: 6 }));

  // Add more button
  shapes.push(button({ x: 70, y: 554, w: 220, h: 40, text: '+ Thêm từ vựng mới', variant: 'outline-steel' }));
  shapes.push(button({ x: 310, y: 554, w: 200, h: 40, text: '📥 Nhập từ file Excel', variant: 'secondary' }));

  // Footer Buttons
  shapes.push(rectShape({ x: 70, y: 730, w: 1300, h: 1, rx: 0, ry: 0, fill: '#E2E8F0', stroke: '#E2E8F0' }));
  shapes.push(button({ x: 1040, y: 750, w: 140, h: 44, text: 'Huỷ bỏ', variant: 'secondary' }));
  shapes.push(button({ x: 1195, y: 750, w: 175, h: 44, text: 'LƯU GÓI THẺ', variant: 'sakura' }));

  return shapes.join('');
}

// 8. FLASHCARD PRACTICE (FLIP CARD) (practice.html mode: flashcard)
function buildFlipCardPractice() {
  const shapes = [];
  shapes.push(realNavbar({ activeMenu: 'flashcards' }));

  // Header & Top Nav (lines 9-39 of practice.html)
  shapes.push(textShape({ x: 40, y: 78, w: 400, h: 20, text: '‹ Trở về danh mục gói thẻ', size: 12, color: '#64748B' }));
  shapes.push(textShape({ x: 40, y: 100, w: 600, h: 28, text: 'Bộ Luyện Tập Từ Vựng Quizlet', size: 22, weight: 'bold', color: '#173B57' }));

  // Quick Level Filter top right
  shapes.push(rectShape({ x: 1100, y: 92, w: 140, h: 36, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 1114, y: 100, w: 110, h: 20, text: 'JLPT N4 ▾', size: 12, color: '#0F172A' }));
  shapes.push(rectShape({ x: 1250, y: 92, w: 150, h: 36, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 1264, y: 100, w: 120, h: 20, text: 'Bài 5: Mua sắm ▾', size: 12, color: '#0F172A' }));

  // Quizlet Study Mode Switcher Bar (lines 42-60 of practice.html)
  shapes.push(rectShape({ x: 40, y: 140, w: 1360, h: 56, rx: 16, ry: 16, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  // Active Tab 1: Flashcards
  shapes.push(rectShape({ x: 50, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#E06D53', stroke: '#E06D53' }));
  shapes.push(textShape({ x: 50, y: 158, w: 440, h: 22, text: '🎴 Thẻ ghi nhớ (Flashcards)', size: 14, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  // Tab 2: Match Game
  shapes.push(rectShape({ x: 500, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 500, y: 158, w: 440, h: 22, text: '🧩 Ghép thẻ (Match Game)', size: 14, weight: 'normal', color: '#64748B', align: '0.5,0.5' }));

  // Tab 3: Test
  shapes.push(rectShape({ x: 950, y: 146, w: 440, h: 44, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 950, y: 158, w: 440, h: 22, text: '📝 Kiểm tra trắc nghiệm (Test)', size: 14, weight: 'normal', color: '#64748B', align: '0.5,0.5' }));

  // Top Progress & Stats (lines 70-80)
  shapes.push(rectShape({ x: 40, y: 210, w: 1360, h: 50, rx: 12, ry: 12, fill: '#FFFFFF', stroke: '#E2E8F0' }));
  shapes.push(badge({ x: 60, y: 222, text: 'Chưa nhớ: 4', bg: '#FEE2E2', color: '#DC2626', w: 120, h: 26, rx: 13 }));
  shapes.push(badge({ x: 190, y: 222, text: 'Đã nhớ: 12', bg: '#D1FAE5', color: '#059669', w: 110, h: 26, rx: 13 }));

  shapes.push(progressBar({ x: 450, y: 232, w: 700, percent: 35, fill: '#E06D53', h: 8 }));
  shapes.push(textShape({ x: 1180, y: 224, w: 200, h: 20, text: 'Thẻ 12 / 35 (35%)', size: 13, weight: 'bold', color: '#173B57', align: '1,0' }));

  // BIG 3D FLASHCARD (lines 90-150 of practice.html)
  shapes.push(rectShape({ x: 300, y: 280, w: 840, h: 400, rx: 20, ry: 20, fill: '#FFFFFF', stroke: '#CBD5E1', strokeW: 2 }));
  shapes.push(badge({ x: 330, y: 304, text: 'DANH TỪ', bg: '#F1F5F9', color: '#475569', w: 90, h: 24, rx: 6 }));
  shapes.push(textShape({ x: 1080, y: 304, w: 30, h: 24, text: '🔊', size: 20, color: '#173B57' }));

  // Big Japanese
  shapes.push(textShape({ x: 300, y: 400, w: 840, h: 72, text: '買い物', size: 68, weight: 'bold', color: '#173B57', align: '0.5,0.5' }));
  shapes.push(textShape({ x: 300, y: 486, w: 840, h: 30, text: 'かいもの (kaimono)', size: 22, color: '#64748B', align: '0.5,0.5' }));

  shapes.push(textShape({ x: 300, y: 610, w: 840, h: 24, text: '🔄 Nhấn vào thẻ hoặc phím Space để lật mặt sau', size: 13, color: '#E06D53', align: '0.5,0.5' }));

  // Quizlet Action Buttons (Chưa nhớ / Đã nhớ) (lines 160-200)
  shapes.push(button({ x: 300, y: 700, w: 240, h: 54, text: '✕ Chưa thuộc (Học lại)', variant: 'outline-danger', fontSize: 15, rx: 27 }));
  shapes.push(button({ x: 560, y: 700, w: 140, h: 54, text: '‹ Thẻ trước', variant: 'secondary', rx: 27 }));
  shapes.push(button({ x: 710, y: 700, w: 170, h: 54, text: 'Lật thẻ [Space]', variant: 'secondary', rx: 27 }));
  shapes.push(button({ x: 900, y: 700, w: 240, h: 54, text: '✓ Đã thuộc lòng', variant: 'success', fontSize: 15, rx: 27 }));

  return shapes.join('');
}

module.exports = {
  buildFlashcardList,
  buildFlashcardDetail,
  buildCreateFlashcard,
  buildFlipCardPractice
};
