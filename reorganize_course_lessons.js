const { Client } = require('pg');

const clientConfig = {
  host: 'aws-0-ap-south-1.pooler.supabase.com',
  port: 6543,
  database: 'postgres',
  user: 'postgres.eztcijnvnmcievogjufk',
  password: 'Cme85Dpt1b2HwrBQ',
  ssl: { rejectUnauthorized: false }
};

// Course curriculum definition generator
function getCurriculumForLevel(level, courseId, midtermExamId, finalExamId) {
  let units = [];

  if (level === 'N5') {
    units = [
      {
        unitNum: 1,
        title: 'Chào hỏi, Giới thiệu bản thân & Đại từ nhân xưng',
        vocabTopic: 'Quốc gia, nghề nghiệp, chào hỏi & Chữ số cơ bản',
        vocabKanji: 'Hán tự cơ bản: 一 (Nhất), 二 (Nhị), 三 (Tam), 人 (Nhân), 日 (Nhật), 本 (Bản)',
        grammarRules: 'Cấu trúc câu khẳng định, phủ định và nghi vấn: N1 は N2 です / ではありません / ですか',
        practiceTopic: 'Hội thoại chào hỏi lần đầu gặp gỡ, tự giới thiệu tên, tuổi, quốc tịch và nghề nghiệp'
      },
      {
        unitNum: 2,
        title: 'Đồ vật, Vật dụng học tập & Đại từ chỉ định',
        vocabTopic: 'Đồ dùng học tập, sách báo, thiết bị cá nhân & Danh từ chỉ vật',
        vocabKanji: 'Hán tự: 本 (Sách), 語 (Ngôn), 先 (Tiên), 生 (Sinh), 学 (Học), 校 (Hiệu)',
        grammarRules: 'Đại từ chỉ định đồ vật これ・それ・あれ và この・その・あの + Danh từ, Trợ từ sở hữu の',
        practiceTopic: 'Luyện tập hỏi đáp xác định chủ sở hữu đồ vật trong lớp học và văn phòng'
      },
      {
        unitNum: 3,
        title: 'Địa điểm, Cơ quan, Phương hướng & Mua sắm',
        vocabTopic: 'Các phòng ban, trường học, công ty, trung tâm thương mại & Số đếm giá tiền (Yên/Đồng)',
        vocabKanji: 'Hán tự: 国 (Quốc), 会 (Hội), 社 (Xã), 門 (Môn), 室 (Thất), 円 (Viên)',
        grammarRules: 'Đại từ chỉ vị trí ここ・そこ・あそこ, Cấu trúc hỏi vị trí どこ・どちら, Hỏi giá cả いくら, Trợ từ も',
        practiceTopic: 'Giao tiếp thực tế khi đi mua sắm tại cửa hàng tiện lợi và trung tâm thương mại Nhật Bản'
      },
      {
        unitNum: 4,
        title: 'Thời gian, Giờ giấc & Lịch trình sinh hoạt hàng ngày',
        vocabTopic: 'Giờ, phút, thứ trong tuần, buổi sáng/trưa/tối & Các động từ chỉ hoạt động thường nhật',
        vocabKanji: 'Hán tự: 今 (Kim), 時 (Thời), 分 (Phân), 半 (Bán), 月 (Nguyệt), 火 (Hỏa), 水 (Thủy)',
        grammarRules: 'Động từ thì hiện tại & quá khứ (ます / ました / ません), Trợ từ chỉ thời điểm に, Khoảng thời gian から...まで',
        practiceTopic: 'Luyện tập nói về thói quen thức dậy, làm việc, đọc sách và lập thời gian biểu cá nhân'
      },
      {
        unitNum: 5,
        title: 'Di chuyển, Phương tiện giao thông & Điểm đến',
        vocabTopic: 'Phương tiện giao thông (tàu điện, xe buýt, máy bay), ga tàu, sân bay & Ngày tháng năm',
        vocabKanji: 'Hán tự: 行 (Hành), 来 (Lai), 帰 (Quy), 車 (Xa), 駅 (Dịch), 年 (Niên)',
        grammarRules: 'Chỉ phương hướng di chuyển へ 行きます/来ます/帰ります, Phương tiện di chuyển で, Đi cùng ai と',
        practiceTopic: 'Hội thoại hỏi đường đi đến ga tàu, sân bay và chia sẻ kế hoạch đi du lịch Tokyo'
      },
      {
        unitNum: 6,
        title: 'Ẩm thực & Hành động tác động đối tượng trực tiếp',
        vocabTopic: 'Món ăn, đồ uống, trái cây, bữa sáng/trưa/tối & Nhà hàng, quán ăn',
        vocabKanji: 'Hán tự: 食 (Thực), 飲 (Ẩm), 買 (Mãi), 見 (Kiến), 聞 (Văn), 書 (Thư)',
        grammarRules: 'Tân ngữ trực tiếp N を Vます, Nơi diễn ra hành động N で Vます, Cấu trúc rủ rê cùng làm Vましょう / Vませんか',
        practiceTopic: 'Đọc hiểu thực đơn món ăn Nhật, hội thoại gọi món tại quán Ramen và rủ bạn đi uống cà phê'
      },
      {
        unitNum: 7,
        title: 'Công cụ, Ngôn ngữ & Trao nhận quà tặng',
        vocabTopic: 'Công cụ, đũa, dao kéo, máy vi tính, thư từ & Mối quan hệ gia đình, bạn bè',
        vocabKanji: 'Hán tự: 手 (Thủ), 紙 (Chỉ), 花 (Hoa), 友 (Hữu), 父 (Phụ), 母 (Mẫu)',
        grammarRules: 'Hành động bằng công cụ N で, Cho tặng N に あげます, Nhận từ ai N に/から もらいます, Đã hoàn thành もう Vました',
        practiceTopic: 'Giao tiếp tặng quà sinh nhật, cảm ơn và chia sẻ văn hóa tặng quà Omiyage của người Nhật'
      },
      {
        unitNum: 8,
        title: 'Tính từ, Miêu tả đặc điểm người, đồ vật & Thời tiết',
        vocabTopic: 'Tính từ đuôi い và đuôi な miêu tả tính cách, hình dáng, hương vị, cảnh quan và khí hậu',
        vocabKanji: 'Hán tự: 白 (Bạch), 黒 (Hắc), 赤 (Xích), 青 (Thanh), 大 (Đại), 小 (Tiểu), 新 (Tân), 古 (Cổ)',
        grammarRules: 'Cách chia tính từ khẳng định, phủ định, quá khứ; Bổ nghĩa cho danh từ; Câu hỏi cảm tưởng どうですか / どんな',
        practiceTopic: 'Miêu tả địa danh du lịch Kyoto, người bạn thân và cảm nhận về thời tiết 4 mùa tại Nhật Bản'
      },
      {
        unitNum: 9,
        title: 'Sở thích, Năng lực & Cấu trúc biểu thị lý do',
        vocabTopic: 'Môn thể thao, thể loại âm nhạc, hội họa, ngoại ngữ & Mức độ năng lực',
        vocabKanji: 'Hán tự: 好 (Hảo), 上 (Thượng), 下 (Hạ), 多 (Đa), 少 (Thiểu), 近 (Cận), 遠 (Viễn)',
        grammarRules: 'Biểu thị sở thích, năng lực N が 好き/嫌い/上手/下手/わかります, Trợ từ nguyên nhân lý do から, Phó từ mức độ よく/だいたい/ぜんぜん',
        practiceTopic: 'Viết bài tự giới thiệu sở thích, năng khiếu cá nhân và giao lưu kết nối câu lạc bộ'
      },
      {
        unitNum: 10,
        title: 'Sự tồn tại & Vị trí không gian của người và vật',
        vocabTopic: 'Từ chỉ vị trí không gian (trên, dưới, trong, ngoài, cạnh, giữa, đối diện) & Động vật, cây cỏ',
        vocabKanji: 'Hán tự: 前 (Tiền), 後 (Hậu), 左 (Tả), 右 (Hữu), 中 (Trung), 外 (Ngoại), 男 (Nam), 女 (Nữ)',
        grammarRules: 'Phân biệt tồn tại sinh vật N が います và đồ vật N が あります, Cấu trúc vị trí Nơi chốn に N が あります/います',
        practiceTopic: 'Đọc hiểu sơ đồ phòng trọ kiểu Nhật, miêu tả bài trí đồ đạc và tìm kiếm đồ thất lạc'
      }
    ];
  } else if (level === 'N4') {
    units = [
      {
        unitNum: 1,
        title: 'Động từ thể Te (〜て) & Yêu cầu nhờ vả lịch sự',
        vocabTopic: 'Động từ nhóm 1, nhóm 2, nhóm 3 và các hoạt động công sở, dịch vụ',
        vocabKanji: 'Hán tự: 安 (An), 高 (Cao), 長 (Trường), 短 (Đoản), 多 (Đa), 少 (Thiểu)',
        grammarRules: 'Quy tắc chia thể Te (〜て), Mẫu câu yêu cầu lịch sự 〜てください, Đề nghị giúp đỡ 〜ましょうか',
        practiceTopic: 'Hội thoại giao tiếp nhờ vả giúp đỡ tại văn phòng làm việc và dịch vụ khách hàng'
      },
      {
        unitNum: 2,
        title: 'Cho phép, Nghiêm cấm & Trạng thái tiếp diễn',
        vocabTopic: 'Quy tắc nơi công cộng, biển báo giao thông, nội quy bảo tàng & Bệnh viện',
        vocabKanji: 'Hán tự: 言 (Ngôn), 話 (Thoại), 語 (Ngữ), 読 (Độc), 書 (Thư), 聞 (Văn)',
        grammarRules: 'Xin phép 〜てもいいですか, Nghiêm cấm tuyệt đối 〜てはいけません, Trạng thái đang diễn ra 〜ています',
        practiceTopic: 'Đọc hiểu các bảng thông báo nội quy nơi công cộng và hội thoại xin phép tại cơ quan'
      },
      {
        unitNum: 3,
        title: 'Nối tiếp hành động & Chuỗi sự kiện thời gian',
        vocabTopic: 'Các động từ chuỗi hành động buổi sáng, quy trình nấu ăn, mua sắm và chuyển tàu điện',
        vocabKanji: 'Hán tự: 出 (Xuất), 入 (Nhập), 降 (Hàng), 乗 (Thừa), 着 (Trước), 発 (Phát)',
        grammarRules: 'Nối chuỗi hành động liên tiếp V1て, V2て, V3ます; Hành động sau khi hoàn thành V1てから V2',
        practiceTopic: 'Viết bài luận ngắn kể lại chuyến du lịch Nara và hướng dẫn lộ trình đi tàu điện ngầm'
      },
      {
        unitNum: 4,
        title: 'Động từ thể Nai (〜ない) & Nghĩa vụ, Miễn trừ bắt buộc',
        vocabTopic: 'Từ vựng y tế, bệnh viện, đơn thuốc, triệu chứng sức khỏe & Giữ gìn an toàn',
        vocabKanji: 'Hán tự: 病 (Bệnh), 院 (Viện), 痛 (Thống), 薬 (Dược), 体 (Thể), 頭 (Đầu)',
        grammarRules: 'Chia thể Nai (〜ない), Yêu cầu đừng làm 〜ないでください, Bắt buộc phải làm 〜なければなりません, Không cần làm cũng được 〜なくてもいいです',
        practiceTopic: 'Hội thoại khám bệnh tại phòng khám Nhật Bản và hướng dẫn sử dụng đơn thuốc'
      },
      {
        unitNum: 5,
        title: 'Động từ thể Từ điển (辞書形) & Khả năng, Sở thích',
        vocabTopic: 'Năng khiếu, thể thao nghệ thuật, kỹ năng chuyên môn và các ngôn ngữ thế giới',
        vocabKanji: 'Hán tự: 始 (Thủy), 終 (Chung), 勉 (Miễn), 強 (Cường), 習 (Tập), 研 (Nghiên)',
        grammarRules: 'Quy tắc chia thể Từ điển (辞書形), Diễn đạt năng lực V(辞書形)ことができます, Danh từ hóa sở thích 趣味は Vこと です, Trước khi V(辞書形)まえに',
        practiceTopic: 'Phỏng vấn xin việc bán thời gian (Baito), trình bày kỹ năng cá nhân và kinh nghiệm'
      },
      {
        unitNum: 6,
        title: 'Động từ thể Ta (〜た) & Trải nghiệm, Liệt kê hành động',
        vocabTopic: 'Địa danh du lịch, phong tục truyền thống lễ hội Matsuri, leo núi Phú Sĩ',
        vocabKanji: 'Hán tự: 登 (Đăng), 泊 (Bạc), 旅 (Lữ), 洗 (Tẩy), 泳 (Vịnh), 走 (Tẩu)',
        grammarRules: 'Chia thể Ta (〜た), Đã từng có kinh nghiệm Vたことがあります, Liệt kê hành động tiêu biểu V1たり V2たりします, Biến đổi trạng thái 〜くなります / 〜になります',
        practiceTopic: 'Chia sẻ kỷ niệm trải nghiệm tắm Onsen tại Hakone và liệt kê các hoạt động cuối tuần'
      },
      {
        unitNum: 7,
        title: 'Thể Thông thường (普通形) & Giao tiếp thân mật đời sống',
        vocabTopic: 'Từ vựng hội thoại thường nhật thân mật giữa bạn bè, người thân gia đình, giới trẻ',
        vocabKanji: 'Hán tự: 思 (Tư), 言 (Ngôn), 考 (Khảo), 知 (Tri), 答 (Đáp), 教 (Giáo)',
        grammarRules: 'Quy tắc chuyển từ Thể lịch sự (丁寧形) sang Thể thông thường (普通形); Hội thoại thân mật trong giao tiếp',
        practiceTopic: 'Luyện tập đối thoại thân mật với bạn cùng lớp đại học và nhắn tin nhóm giao lưu'
      },
      {
        unitNum: 8,
        title: 'Bày tỏ quan điểm cá nhân, Suy đoán & Trích dẫn ý kiến',
        vocabTopic: 'Chủ đề xã hội, môi trường, kinh tế, ý kiến quan điểm về công việc tương lai',
        vocabKanji: 'Hán tự: 物 (Vật), 事 (Sự), 心 (Tâm), 配 (Phối), 意 (Ý), 味 (Vị)',
        grammarRules: 'Tôi nghĩ rằng 〜と 思います, Ai đó đã nói rằng 〜と 言いました, Xác nhận sự đồng thuận でしょう',
        practiceTopic: 'Thảo luận nhóm về lợi ích của trí tuệ nhân tạo và trình bày ý kiến trước lớp'
      },
      {
        unitNum: 9,
        title: 'Mệnh đề bổ nghĩa cho Danh từ & Định ngữ trong tiếng Nhật',
        vocabTopic: 'Mô tả diện mạo người, trang phục áo quần, căn hộ chung cư và đồ lưu niệm',
        vocabKanji: 'Hán tự: 服 (Phục), 着 (Trước), 買 (Mãi), 売 (Mại), 貸 (Thải), 借 (Tá)',
        grammarRules: 'Cấu trúc mệnh đề bổ ngữ cho danh từ: [Thể thông thường] + Danh từ; Biến mệnh đề thành thành phần câu',
        practiceTopic: 'Đọc hiểu mẩu tin tìm người lạc, miêu tả người mặc áo đỏ đeo kính và chọn căn hộ phù hợp'
      },
      {
        unitNum: 10,
        title: 'Cấu trúc câu điều kiện と / たら & Biểu đạt Dự định, Kế hoạch',
        vocabTopic: 'Thời tiết biến đổi, kế hoạch du học, chuyển nhà, tiết kiệm tài chính cá nhân',
        vocabKanji: 'Hán tự: 晴 (Tình), 雨 (Vũ), 雪 (Tuyết), 風 (Phong), 天 (Thiên), 気 (Khí)',
        grammarRules: 'Điều kiện tất yếu 〜と, Điều kiện giả định 〜たら, Biểu đạt dự định kế hoạch 〜つもりです / 〜予定です',
        practiceTopic: 'Viết bài văn ngắn về kế hoạch 5 năm tới sau khi tốt nghiệp và lập lộ trình du học Nhật Bản'
      }
    ];
  } else if (level === 'N3') {
    units = [
      {
        unitNum: 1,
        title: 'Thể Khả năng, Tự động từ & Tha động từ trong đời sống',
        vocabTopic: 'Các cặp tự động từ - tha động từ thường gặp, biến cố đời sống, hỏng hóc đồ đạc',
        vocabKanji: 'Hán tự: 開 (Khai), 閉 (Bế), 止 (Chỉ), 消 (Tiêu), 直 (Trực), 治 (Trị)',
        grammarRules: 'Chia thể khả năng 可能形, Phân biệt sắc thái Tự động từ (が) và Tha động từ (を), Mẫu câu 〜てある và 〜ている',
        practiceTopic: 'Báo cáo sự cố thiết bị hư hỏng với ban quản lý tòa nhà và hội thoại giải quyết sự cố'
      },
      {
        unitNum: 2,
        title: 'Thể Ý chí (意向形) & Cấu trúc Biểu thị Mục đích',
        vocabTopic: 'Dự án công việc, khởi nghiệp, học tập chứng chỉ chuyên ngành, rèn luyện thể chất',
        vocabKanji: 'Hán tự: 決 (Quyết), 定 (Định), 約 (Ước), 束 (Thúc), 守 (Thủ), 破 (Phá)',
        grammarRules: 'Thể ý chí 意向形, Cấu trúc 〜(よ)うと思う, Biểu đạt mục đích 〜ために (Ý chí) và 〜ように (Trạng thái)',
        practiceTopic: 'Trình bày mục tiêu học tập năm mới và lập kế hoạch rèn luyện chuẩn bị cho kỳ thi JLPT'
      },
      {
        unitNum: 3,
        title: 'Mệnh lệnh, Cấm chỉ & Lời khuyên, Cảnh báo an toàn',
        vocabTopic: 'Quy định an toàn lao động công xưởng, phòng cháy chữa cháy, chỉ dẫn sơ tán',
        vocabKanji: 'Hán tự: 危 (Nguy), 険 (Hiểm), 命 (Mệnh), 令 (Lệnh), 禁 (Cấm), 止 (Chỉ)',
        grammarRules: 'Thể mệnh lệnh 命令形, Thể cấm chỉ 禁止形, Mẫu đưa ra lời khuyên tế nhị 〜ほうがいい, Cảnh báo 〜ないように',
        practiceTopic: 'Đọc hiểu sổ tay quy tắc an toàn lao động của công ty sản xuất và chỉ huy sơ tán khẩn cấp'
      },
      {
        unitNum: 4,
        title: 'Hệ thống Câu điều kiện chuyên sâu: ば・たら・なら・と',
        vocabTopic: 'Kinh doanh bán hàng, đàm phán hợp đồng, tư vấn đầu tư tài chính, thời cơ',
        vocabKanji: 'Hán tự: 算 (Toán), 勝 (Thắng), 負 (Phụ), 競 (Cạnh), 争 (Tranh), 試 (Thí)',
        grammarRules: 'Phân biệt triệt để 4 cấu trúc điều kiện: 〜ば, 〜たら, 〜なら, 〜と trong ngữ cảnh cụ thể',
        practiceTopic: 'Thảo luận phân tích phương án kinh doanh: nếu chọn giải pháp A thì hệ quả ra sao'
      },
      {
        unitNum: 5,
        title: 'Thể Bị động (受身形) & Bị động phiền toái trong quan hệ xã hội',
        vocabTopic: 'Mối quan hệ hàng xóm, tiếng ồn đô thị, thời sự xã hội, động vật nuôi',
        vocabKanji: 'Hán tự: 頼 (Lại), 怒 (Nộ), 盗 (Đạo), 壊 (Hoại), 悲 (Bi), 泣 (Khốc)',
        grammarRules: 'Quy tắc chia thể bị động 受身形, Bị động trực tiếp, Bị động gián tiếp / phiền toái (迷惑の受身)',
        practiceTopic: 'Chia sẻ câu chuyện dở khóc dở cười khi bị người khác dẫm vào chân trên tàu điện đông đúc'
      },
      {
        unitNum: 6,
        title: 'Thể Sai khiến (使役形) & Biểu thị Cho phép, Sai bảo',
        vocabTopic: 'Quản lý nhân sự, nuôi dạy trẻ em, huấn luyện tân binh trong doanh nghiệp',
        vocabKanji: 'Hán tự: 任 (Nhiệm), 務 (Vụ), 働 (Động), 休 (Hưu), 育 (Dục), 練 (Luyện)',
        grammarRules: 'Quy tắc chia thể sai khiến 使役形 (せる/させる), Kết hợp sai khiến bị động 〜させられる, Mẫu xin phép 〜させてください',
        practiceTopic: 'Xin phép cấp trên cho nghỉ phép về nước có việc gia đình và phân công nhiệm vụ nhân viên'
      },
      {
        unitNum: 7,
        title: 'Kính ngữ Doanh nghiệp: Tôn kính ngữ & Khiêm nhường ngữ',
        vocabTopic: 'Thuật ngữ công sở thương mại, chức danh khách hàng, đối tác, văn hóa trao danh thiếp',
        vocabKanji: 'Hán tự: 拝 (Bái), 伺 (Tứ), 申 (Thân), 参 (Tham), 存 (Tồn), 召 (Triệu)',
        grammarRules: 'Tôn kính ngữ (尊敬語), Khiêm nhường ngữ (謙譲語), Thể lịch sự đặc biệt (お/ご〜になる, お/ご〜する)',
        practiceTopic: 'Thực hành gọi điện thoại đặt lịch hẹn với giám đốc đối tác và tiếp đón khách tại công ty'
      },
      {
        unitNum: 8,
        title: 'Cấu trúc Biểu thị Nguyên nhân, Lý do nâng cao',
        vocabTopic: 'Thời tiết cực đoan, biến đổi khí hậu, dịch bệnh, tăng trưởng kinh tế vĩ mô',
        vocabKanji: 'Hán tự: 因 (Nhân), 果 (Quả), 害 (Hại), 被 (Bị), 響 (Hưởng), 影 (Ảnh)',
        grammarRules: 'Mẫu câu chỉ lý do tích cực 〜おかげで, Tiêu cực 〜せいで, Nguyên nhân khách quan 〜によって / 〜ものだから',
        practiceTopic: 'Viết báo cáo phân tích nguyên nhân thành công của chiến dịch marketing sản phẩm mới'
      },
      {
        unitNum: 9,
        title: 'Cấu trúc So sánh, Đối lập & Tương phản sắc thái',
        vocabTopic: 'So sánh sản phẩm công nghệ tử tế, xu hướng tiêu dùng thế hệ trẻ, khác biệt văn hóa',
        vocabKanji: 'Hán tự: 比 (Tỷ), 較 (Giác), 対 (Đối), 照 (Chiếu), 差 (Sai), 別 (Biệt)',
        grammarRules: 'So sánh đối chiếu 〜に対して, 〜に比べて, Bất chấp/trái ngược với kỳ vọng 〜わりに(は), 〜くせに',
        practiceTopic: 'So sánh ưu nhược điểm giữa làm việc từ xa (Remote work) và làm việc trực tiếp tại văn phòng'
      },
      {
        unitNum: 10,
        title: 'Suy đoán logic, Tính tất yếu & Bứt phá Đọc hiểu',
        vocabTopic: 'Dự báo xu thế công nghệ tương lai, ô tô tự hành, năng lượng tái tạo sạch',
        vocabKanji: 'Hán tự: 確 (Xác), 実 (Thực), 証 (Chứng), 拠 (Cứ), 疑 (Nghi), 判 (Phán)',
        grammarRules: 'Chắc chắn là 〜に違いない, Lẽ ra phải là 〜はずだ, Đương nhiên là 〜わけだ, Không hẳn là 〜わけではない',
        practiceTopic: 'Đọc hiểu bài bình luận báo chí phân tích xu thế thị trường việc làm IT tại Nhật Bản'
      }
    ];
  } else if (level === 'N2') {
    units = [
      {
        unitNum: 1,
        title: 'Phạm vi, Giới hạn & Tiêu biểu đại diện trong văn phong học thuật',
        vocabTopic: 'Văn hóa doanh nghiệp, khảo sát thị trường tiêu dùng, chuyển đổi số quốc gia',
        vocabKanji: 'Hán tự: 限 (Hạn), 範 (Phạm), 囲 (Vi), 領 (Lãnh), 域 (Vực), 統 (Thống)',
        grammarRules: 'Mẫu tiêu biểu 〜をはじめ(として), Trải rộng xuyên suốt 〜にわたって, Thông qua con đường 〜を通じて / 〜を通して',
        practiceTopic: 'Đọc báo cáo khảo sát tác động của trí tuệ nhân tạo đối với chuỗi cung ứng toàn cầu'
      },
      {
        unitNum: 2,
        title: 'Quan hệ Điều kiện Logic ngặt nghèo & Giả định nâng cao',
        vocabTopic: 'Hợp đồng thương mại quốc tế, điều khoản bảo mật, thỏa thuận hợp tác đầu tư',
        vocabKanji: 'Hán tự: 条 (Điều), 件 (Kiện), 諾 (Nặc), 拒 (Cự), 否 (Phủ), 契 (Khế)',
        grammarRules: 'Chỉ cần có điều kiện 〜さえ...ば, Cho dù có thế nào 〜たとえ...ても, Nếu không làm A thì không thể B 〜ないことには',
        practiceTopic: 'Phân tích các điều khoản pháp lý trong hợp đồng dịch vụ công nghệ thông tin'
      },
      {
        unitNum: 3,
        title: 'Căn cứ, Khởi nguồn sự việc & Động cơ hành động sâu sắc',
        vocabTopic: 'Nghiên cứu khoa học hành vi, tâm lý người tiêu dùng, triết lý kinh doanh',
        vocabKanji: 'Hán tự: 根 (Căn), 拠 (Cứ), 源 (Nguyên), 基 (Cơ), 準 (Chuẩn), 規 (Quy)',
        grammarRules: 'Chính vì lý do đó 〜からこそ, Chính vì danh nghĩa 〜だけに, Chỉ vì một sơ suất nhỏ 〜ばかりに',
        practiceTopic: 'Viết bài luận phân tích động lực giúp các doanh nghiệp khởi nghiệp vượt qua khủng hoảng'
      },
      {
        unitNum: 4,
        title: 'Biểu hiện Cảm xúc, Tâm lý nội tâm & Phản ứng tự nhiên',
        vocabTopic: 'Nghệ thuật giao tiếp ứng xử đỉnh cao, điều tiết cảm xúc cá nhân, khủng hoảng truyền thông',
        vocabKanji: 'Hán tự: 堪 (Kham), 忍 (Nhẫn), 耐 (Nại), 恐 (Khủng), 憤 (Phẫn), 悦 (Duyệt)',
        grammarRules: 'Cảm xúc không thể kìm nén 〜たまらない / 〜てしょうがない, Buộc lòng phải làm 〜ざるを得ない',
        practiceTopic: 'Bình luận về một tác phẩm văn học phản ánh mâu thuẫn thế hệ trong gia đình Nhật Bản hiện đại'
      },
      {
        unitNum: 5,
        title: 'Thời điểm bùng phát & Sự diễn tiến đồng thời của sự kiện',
        vocabTopic: 'Tin tức thời sự nóng bỏng, thị trường chứng khoán, biến động thiên tai',
        vocabKanji: 'Hán tự: 緊 (Khẩn), 急 (Cấp), 激 (Kích), 変 (Biến), 兆 (Triệu), 候 (Hậu)',
        grammarRules: 'Ngay khi có kết quả 〜次第, Vừa mới làm xong thì ngay lập tức 〜たとたん(に), Đúng lúc cao điểm 〜最中に',
        practiceTopic: 'Đọc bản tin truyền hình khẩn cấp về việc xử lý sự cố gián đoạn hệ thống ngân hàng'
      },
      {
        unitNum: 6,
        title: 'Nhượng bộ, Mặt trái & Sắc thái đối lập đa chiều',
        vocabTopic: 'Chính sách phúc lợi xã hội, già hóa dân số, phát triển kinh tế bền vững ESG',
        vocabKanji: 'Hán tự: 矛 (Mâu), 盾 (Thuẫn), 妥 (Thỏa), 協 (Hiệp), 逆 (Nghịch), 転 (Chuyển)',
        grammarRules: 'Tuy có mặt A nhưng ngược lại mặt B 〜反面 / 〜一方で, Hoàn toàn trái ngược với 〜どころか',
        practiceTopic: 'Tranh luận về hai mặt của đô thị hóa nhanh và sự suy giảm cộng đồng nông thôn Nhật Bản'
      },
      {
        unitNum: 7,
        title: 'Đánh giá, Thái độ chuẩn mực & Chuẩn mực đạo đức xã hội',
        vocabTopic: 'Đạo đức nghề nghiệp IT, bảo mật dữ liệu cá nhân, trách nhiệm cộng đồng',
        vocabKanji: 'Hán tự: 評 (Bình), 価 (Giá), 倫 (Luân), 理 (Lý), 責 (Trách), 職 (Chức)',
        grammarRules: 'Chẳng qua chỉ là 〜にすぎない, Không gì khác chính là 〜にほかならない, Nên/không nên 〜べきだ / 〜べきではない',
        practiceTopic: 'Thuyết trình về trách nhiệm của lập trình viên trong việc ngăn chặn thiên kiến thuật toán AI'
      },
      {
        unitNum: 8,
        title: 'Tiếng Nhật Thương mại & Thư tín Doanh nghiệp Keigo cao cấp',
        vocabTopic: 'Soạn thảo email đàm phán thương vụ, thư khiếu nại, thư xin lỗi khách hàng đối tác',
        vocabKanji: 'Hán tự: 謹 (Cẩn), 慎 (Thận), 儀 (Nghi), 礼 (Lễ), 謝 (Tạ), 詫 (Sá)',
        grammarRules: 'Kính ngữ cao cấp thư tín: 恐れ入りますが, ご査収のほど, 平素は格別のご高配を賜り, v.v.',
        practiceTopic: 'Soạn thảo bức thư từ chối đề nghị hợp tác một cách khéo léo mà vẫn duy trì quan hệ hữu hảo'
      },
      {
        unitNum: 9,
        title: 'Chiến thuật Bẻ khóa Đọc hiểu Trung văn & Trường văn N2',
        vocabTopic: 'Tiểu luận phê bình văn hóa, khoa học nhận thức, triết học nhân sinh quan',
        vocabKanji: 'Hán tự: 読 (Độc), 解 (Giải), 脈 (Mạch), 絡 (Lạc), 論 (Luận), 旨 (Chỉ)',
        grammarRules: 'Kỹ thuật định vị câu chủ đề Topic Sentence, nhận diện liên từ đảo ngược しかし/ところが, bóc tách cấu trúc câu phức',
        practiceTopic: 'Luyện giải các bài đọc hiểu trường văn 1000 chữ về mối quan hệ giữa con người và thiên nhiên'
      },
      {
        unitNum: 10,
        title: 'Kỹ năng Bắt âm Nghe hiểu Hội thoại nhanh & Ứng biến phòng thi',
        vocabTopic: 'Hội thoại tranh luận tại cuộc họp hội đồng quản trị, phỏng vấn chuyên gia đài NHK',
        vocabKanji: 'Hán tự: 聴 (Thính), 解 (Giải), 速 (Tốc), 記 (Ký), 要 (Yếu), 点 (Điểm)',
        grammarRules: 'Nhận diện phát âm nuốt âm, từ lóng công sở, phân loại ý kiến tán thành hay phản đối trong tranh luận',
        practiceTopic: 'Luyện tập bài nghe tổng hợp thông tin đa nguồn (Sơ đồ kết hợp âm thanh) đạt điểm tối đa'
      }
    ];
  } else if (level === 'N1') {
    units = [
      {
        unitNum: 1,
        title: 'Văn phong Khảo cứu, Luận đề & Cấu trúc Học thuật Thượng thừa',
        vocabTopic: 'Học thuật triết học, bản thể luận, ngôn ngữ học so sánh, tư tưởng Đông Á',
        vocabKanji: 'Hán tự: 綜 (Tống), 稽 (Kê), 闡 (Xiển), 緻 (Trí), 蘊 (Uẩn), 眸 (Mâu)',
        grammarRules: 'Trong hoàn cảnh đặc biệt 〜にあって, Giá trị độc nhất chỉ có ở 〜ならでは(の), Bằng tất cả phương tiện 〜をもって',
        practiceTopic: 'Đọc trích đoạn khảo cứu về sự tiếp biến văn hóa chữ Hán trong không gian Á Đông thời trung đại'
      },
      {
        unitNum: 2,
        title: 'Điều kiện Ngặt nghèo Cực đoan & Bất biến trong Tư duy Triết luận',
        vocabTopic: 'Luật hiến pháp, pháp quyền quốc tế, hiệp định giải giáp vũ khí hạt nhân, quyền con người',
        vocabKanji: 'Hán tự: 苛 (Hà), 酷 (Khốc), 抵 (Để), 触 (Xúc), 遵 (Tuân), 奉 (Phụng)',
        grammarRules: 'Bất kể là danh xưng gì 〜であれ / 〜であろうと, Nếu vì mục tiêu ấy 〜とあれば, Không thể thiếu yếu tố 〜を抜きにしては',
        practiceTopic: 'Phân tích bản phán quyết của tòa án tối cao liên quan đến quyền tự do biểu đạt tư tưởng'
      },
      {
        unitNum: 3,
        title: 'Biểu hiện Cảm xúc Tột cùng & Ý chí Kiên định Tuyệt đối',
        vocabTopic: 'Tâm cảm con người trước thảm họa, lòng biết ơn sâu sắc, đức hy sinh cao cả',
        vocabKanji: 'Hán tự: 悼 (Điệu), 憐 (Lân), 憫 (Mẫn), 憧 (Sung), 憬 (Cảnh), 慨 (Khái)',
        grammarRules: 'Vô cùng cảm kích không sao kể xiết 〜に堪えない, Không thể kìm nén được 〜を禁じ得ない, Luôn cầu chúc không ngừng 〜てやまない',
        practiceTopic: 'Đọc diễn văn tưởng niệm hòa bình tại Hiroshima và phân tích cấu trúc tu từ biểu cảm cao cấp'
      },
      {
        unitNum: 4,
        title: 'Mối quan hệ Tương hỗ Hài hòa & Hệ quả Khách quan Tất yếu',
        vocabTopic: 'Kinh tế sinh thái học, địa chính trị thế giới, cộng hưởng văn hóa đại chúng',
        vocabKanji: 'Hán tự: 醸 (Nhưỡng), 逓 (Đệ), 漸 (Tiệm), 軋 (Áp), 轢 (Lịch), 融 (Dung)',
        grammarRules: 'Cộng hưởng kết hợp cùng nhau 〜相まって, Tương xứng thích đáng với 〜相応, Vừa mới dứt thì ngay tức khắc 〜が早いか',
        practiceTopic: 'Viết bài xã luận về sức mạnh cộng hưởng giữa công nghệ bán dẫn và chính sách tự chủ chiến lược'
      },
      {
        unitNum: 5,
        title: 'Nhượng bộ Sâu sắc, Phản biện Luận điểm & Đối chiếu Đa chiều',
        vocabTopic: 'Phê bình nghệ thuật đương đại, giải cấu trúc văn học, xung đột giá trị hiện đại',
        vocabKanji: 'Hán tự: 乖 (Quai), 離 (Ly), 謬 (Mậu), 誤 (Ngộ), 欺 (Khi), 瞞 (Man)',
        grammarRules: 'Dẫu biết rằng là vậy 〜とはいえ, Giá như/nếu như là 〜ものを, Nếu là A thì còn hiểu được đằng này B 〜ならいざ知らず',
        practiceTopic: 'Phê bình một bài tham luận về ảnh hưởng của mạng xã hội đối với năng lực tư duy chiều sâu'
      },
      {
        unitNum: 6,
        title: 'Phán đoán Triết luận & Thang Tiêu chuẩn Giá trị Tuyệt đối',
        vocabTopic: 'Đạo đức sinh học, công nghệ chỉnh sửa gen CRISPR, ranh giới nhân tính',
        vocabKanji: 'Hán tự: 鑑 (Giám), 衡 (Hành), 審 (Thẩm), 酌 (Chước), 糾 (Củ), 弾 (Đạn)',
        grammarRules: 'Xứng đáng là đối tượng 〜にたる, Rất đáng giá để 〜に値する, Vô cùng/cực kỳ tồi tệ 〜極まりない / 〜極まる',
        practiceTopic: 'Đọc tiểu luận triết học về định nghĩa sự sống và ranh giới trách nhiệm của khoa học tự nhiên'
      },
      {
        unitNum: 7,
        title: 'Văn phong Xã luận Báo chí, Diễn văn Chính luận & Bình luận',
        vocabTopic: 'Chính sách tiền tệ ngân hàng trung ương BOJ, an ninh năng lượng, trật tự đa cực',
        vocabKanji: 'Hán tự: 覇 (Bá), 権 (Quyền), 凋 (Điêu), 落 (Lạc), 衡 (Hành), 平 (Bình)',
        grammarRules: 'Ngữ pháp chính luận: 〜を皮切りに, 〜にかこつけて, 〜を余儀なくされる, 〜てまえ',
        practiceTopic: 'Phân tích bài bình luận trên chuyên trang tài chính Nihon Keizai Shimbun (Nikkei)'
      },
      {
        unitNum: 8,
        title: 'Thành ngữ Bốn chữ (Yojijukugo), Điển cố Hán học & Quán dụng ngữ Cổ điển',
        vocabTopic: 'Yojijukugo kinh điển: 一期一会, 臥薪嘗胆, 臨機応変, 切磋琢磨, 破邪顕正',
        vocabKanji: 'Hán tự: 琢 (Trác), 磨 (Ma), 薪 (Tân), 胆 (Đảm), 弊 (Tệ), 害 (Hại)',
        grammarRules: 'Cách vận dụng điêu luyện Yojijukugo và tục ngữ vào bài viết nghị luận xã hội và giao tiếp cấp cao',
        practiceTopic: 'Viết bài luận ứng dụng thành ngữ phản ánh tinh thần vượt khó (Gashin Shotan) của doanh nghiệp Nhật'
      },
      {
        unitNum: 9,
        title: 'Đọc hiểu Văn bản Triết học Trừu tượng, Văn học Hiện đại & Luận đề N1',
        vocabTopic: 'Tác phẩm văn học đoạt giải Akutagawa, tiểu luận hiện sinh, tâm lý học phân tâm',
        vocabKanji: 'Hán tự: 憬 (Cảnh), 朧 (Lung), 謐 (Mật), 儚 (Vũ), 隘 (Ải), 仄 (Trắc)',
        grammarRules: 'Bẻ khóa cú pháp câu văn đa tầng lồng ghép nhiều vế phủ định kép, triệt tiêu chủ ngữ ngầm',
        practiceTopic: 'Giải quyết các câu hỏi đọc hiểu phức hợp 1200 từ với các lựa chọn bẫy sắc thái tinh vi nhất'
      },
      {
        unitNum: 10,
        title: 'Nghe hiểu Đàm phán Đỉnh cao, Hội thảo Khoa học & Tranh biện Học thuật',
        vocabTopic: 'Hội nghị thượng đỉnh ngoại giao, tranh biện kinh tế giữa các nhà hoạch định chính sách toàn cầu',
        vocabKanji: 'Hán tự: 諮 (Tư), 問 (Vấn), 応 (Ứng), 酬 (Thù), 弁 (Biện), 駁 (Bác)',
        grammarRules: 'Kỹ thuật ghi chú tốc ký Shorthand, định vị ngụ ý đằng sau những phát ngôn ngoại giao vòng vo',
        practiceTopic: 'Nghe bài phát biểu tại diễn đàn kinh tế thế giới và tóm tắt 3 khuyến nghị then chốt của diễn giả'
      }
    ];
  }

  // Build the list of lesson records
  const lessons = [];
  let orderIndex = 1;

  for (let i = 0; i < units.length; i++) {
    const u = units[i];
    const sectionName = `Bài ${u.unitNum}: ${u.title}`;

    // Insert Midterm Exam right after Bài 5
    if (u.unitNum === 6 && midtermExamId) {
      lessons.push({
        title: `Bài thi giữa kì: Đánh giá năng lực giai đoạn 1 (JLPT ${level} Midterm Exam)`,
        section_name: 'Đánh giá Cột mốc & Thi giữa kỳ',
        order_index: orderIndex++,
        lesson_type: 'EXAM',
        skill_type: 'EXAM',
        duration_minutes: 35,
        is_free_preview: false,
        exam_id: midtermExamId,
        content: `### 🎯 BÀI THI CHÍNH THỨC TRỰC TUYẾN: ĐÁNH GIÁ NĂNG LỰC GIỮA KỲ (${level})\n\nChào mừng bạn đến với bài thi đánh giá năng lực giữa kỳ dành riêng cho học viên khóa học **JLPT ${level}**.\n\n- **Thời lượng:** 35 phút\n- **Định dạng đề:** Trắc nghiệm kiến thức tổng hợp từ Bài 1 đến Bài 5 (Từ vựng, Ngữ pháp, Đọc hiểu)\n- **Điểm chuẩn đạt:** 50%\n\nNhấn nút **Vào phòng thi giữa kì** để bắt đầu tính giờ làm bài.`
      });
    }

    // 1. Vocabulary & Kanji lesson
    lessons.push({
      title: `Bài ${u.unitNum}.1: Từ vựng & Hán tự - ${u.title}`,
      section_name: sectionName,
      order_index: orderIndex++,
      lesson_type: 'THEORY',
      skill_type: 'VOCABULARY',
      duration_minutes: 30,
      is_free_preview: (u.unitNum === 1),
      exam_id: null,
      content: `### 🎯 Mục tiêu bài học\n- Làm chủ toàn bộ hệ thống từ vựng trọng tâm thuộc chủ đề **${u.title}** chuẩn trình độ JLPT ${level}.\n- Ghi nhớ mặt chữ, quy tắc bút thuận và âm Hán Việt các chữ Hán cốt lõi.\n\n---\n\n### 📚 Danh mục Từ vựng trọng tâm\n- **Chủ đề bài học:** ${u.vocabTopic}\n- Người học cần ghi nhớ nghĩa gốc, cách phát âm chuẩn Tokyo và các ngữ cảnh áp dụng thực tế trong công việc và đời sống.\n\n---\n\n### 🈲 Chữ Hán Cốt lõi (Kanji)\n- **Nội dung Kanji:** ${u.vocabKanji}\n- *Gợi ý phương pháp ghi nhớ:* Học chữ Hán thông qua hình ảnh liên tưởng và bộ thủ để nhớ lâu hơn và không nhầm lẫn các nét tương đồng.\n\n---\n\n### 💡 Lời khuyên ôn tập\n1. Sử dụng tính năng Flashcard tương tác trên hệ thống để ôn tập ngắt quãng (Spaced Repetition).\n2. Đặt ít nhất 3 câu ví dụ có chứa từ mới mỗi ngày để biến từ vựng thành phản xạ tự nhiên.`
    });

    // 2. Grammar lesson
    lessons.push({
      title: `Bài ${u.unitNum}.2: Ngữ pháp & Mẫu câu - ${u.title}`,
      section_name: sectionName,
      order_index: orderIndex++,
      lesson_type: 'THEORY',
      skill_type: 'GRAMMAR',
      duration_minutes: 35,
      is_free_preview: false,
      exam_id: null,
      content: `### 🎯 Mục tiêu bài học\n- Nắm vững cấu trúc, ý nghĩa và sắc thái biểu đạt của các mẫu ngữ pháp then chốt trong **${u.title}**.\n- Nhận biết các bẫy ngữ pháp thường xuất hiện trong đề thi JLPT ${level}.\n\n---\n\n### 📖 Cấu trúc & Quy tắc Ngữ pháp Trọng tâm\n- **Quy tắc cốt lõi:** ${u.grammarRules}\n\n#### Phân tích mẫu câu ví dụ:\n1. **Khẳng định:** Sử dụng đúng trợ từ và vị trí thành phần câu trong ngữ cảnh trang trọng cũng như giao tiếp thân mật.\n2. **Phủ định & Nghi vấn:** Chú ý các dạng chia trợ từ đi kèm tương ứng.\n\n---\n\n### ⚠️ Lưu ý phân biệt sắc thái\n- Chú ý mối quan hệ thứ bậc (trên - dưới, trong - ngoài) khi sử dụng các mẫu câu biểu cảm trong tiếng Nhật.\n- Phân biệt với các cấu trúc tương đồng nhưng khác biệt về mức độ trang trọng.`
    });

    // 3. Practice & Reading lesson
    lessons.push({
      title: `Bài ${u.unitNum}.3: Luyện tập & Đọc hiểu - ${u.title}`,
      section_name: sectionName,
      order_index: orderIndex++,
      lesson_type: 'PRACTICE',
      skill_type: 'READING',
      duration_minutes: 30,
      is_free_preview: false,
      exam_id: null,
      content: `### 🎯 Mục tiêu bài học\n- Vận dụng phối hợp từ vựng và ngữ pháp của ${u.title} vào bài đọc hiểu thực tế và bài tập trắc nghiệm củng cố.\n- Rèn luyện phản xạ đọc nhanh, nắm bắt từ khóa then chốt và trả lời chính xác câu hỏi.\n\n---\n\n### 📝 Tình huống thực hành & Đọc hiểu\n- **Chủ đề thực hành:** ${u.practiceTopic}\n\n#### Đoạn văn luyện đọc:\nNgười học đọc kỹ văn bản, phân tích liên kết logic giữa các vế câu và chọn đáp án chính xác nhất trong phần trắc nghiệm bên dưới.\n\n---\n\n### ✅ Bài tập củng cố tương tác\n- Hãy hoàn thành các câu hỏi trắc nghiệm bên dưới để hệ thống ghi nhận tiến độ học tập của bạn vào lộ trình cá nhân.`
    });
  }

  // Finally append Final Exam
  if (finalExamId) {
    lessons.push({
      title: `Bài thi cuối kì: Đánh giá tổng kết năng lực khóa học (JLPT ${level} Final Exam)`,
      section_name: 'Tổng kết khóa học & Đánh giá năng lực',
      order_index: orderIndex++,
      lesson_type: 'EXAM',
      skill_type: 'EXAM',
      duration_minutes: 50,
      is_free_preview: false,
      exam_id: finalExamId,
      content: `### 🏆 BÀI THI TỔNG KẾT CUỐI KHOÁ HỌC (JLPT ${level} FINAL EXAM)\n\nChúc mừng bạn đã hoàn thành trọn vẹn lộ trình 10 bài học toàn diện của khóa học **JLPT ${level}**!\n\n- **Thời lượng:** 50 phút\n- **Định dạng đề:** Trắc nghiệm mô phỏng chính xác cấu trúc đề thi JLPT Quốc tế (Từ vựng, Ngữ pháp, Đọc hiểu, Nghe hiểu)\n- **Điểm chuẩn đạt chứng chỉ:** 60%\n\nNhấn nút **Vào phòng thi cuối kì** để bắt đầu bài thi tốt nghiệp.`
    });
  }

  return lessons;
}

async function run() {
  const client = new Client(clientConfig);
  await client.connect();
  console.log('Connected to Supabase PostgreSQL database.');

  try {
    // Make lesson_id nullable in study_materials and exercises outside transaction
    console.log('Modifying study_materials and exercises constraints...');
    await client.query(`ALTER TABLE study_materials ALTER COLUMN lesson_id DROP NOT NULL;`);
    await client.query(`ALTER TABLE exercises ALTER COLUMN lesson_id DROP NOT NULL;`);

    await client.query('BEGIN');

    // 1. Unlink FKs from lessons in courses 1, 2, 3, 4, 5
    console.log('Unlinking foreign keys for courses 1, 2, 3, 4, 5...');
    await client.query(`
      DELETE FROM lesson_details 
      WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE course_id IN (1, 2, 3, 4, 5));
    `);
    await client.query(`
      UPDATE exercises 
      SET lesson_id = NULL 
      WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE course_id IN (1, 2, 3, 4, 5));
    `);
    await client.query(`
      UPDATE flashcards 
      SET lesson_id = NULL 
      WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE course_id IN (1, 2, 3, 4, 5));
    `);
    await client.query(`
      UPDATE study_materials 
      SET lesson_id = NULL 
      WHERE lesson_id IN (SELECT lesson_id FROM lessons WHERE course_id IN (1, 2, 3, 4, 5));
    `);

    // 2. Delete existing lessons for courses 1, 2, 3, 4, 5
    console.log('Deleting old lessons for courses 1, 2, 3, 4, 5...');
    await client.query(`DELETE FROM lessons WHERE course_id IN (1, 2, 3, 4, 5)`);

    // 3. Insert structured curriculum for each course
    const courseConfigs = [
      { id: 1, level: 'N5', midtermExamId: 51, finalExamId: 52 },
      { id: 2, level: 'N4', midtermExamId: 47, finalExamId: 48 },
      { id: 3, level: 'N3', midtermExamId: 55, finalExamId: 56 },
      { id: 4, level: 'N2', midtermExamId: 53, finalExamId: 54 },
      { id: 5, level: 'N1', midtermExamId: 49, finalExamId: 50 },
    ];

    const insertedCourseLessons = {};

    for (const cfg of courseConfigs) {
      const lessons = getCurriculumForLevel(cfg.level, cfg.id, cfg.midtermExamId, cfg.finalExamId);
      console.log(`Inserting ${lessons.length} lessons for Course ${cfg.id} (${cfg.level})...`);
      insertedCourseLessons[cfg.id] = [];

      for (const l of lessons) {
        const insRes = await client.query(`
          INSERT INTO lessons (
            course_id, lesson_title, title, section_name, order_index, 
            lesson_type, skill_type, duration_minutes, is_free_preview, 
            exam_id, content, status
          ) VALUES (
            $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 'ACTIVE'
          ) RETURNING lesson_id, order_index, skill_type
        `, [
          cfg.id, l.title, l.title, l.section_name, l.order_index, 
          l.lesson_type, l.skill_type, l.duration_minutes, l.is_free_preview, 
          l.exam_id, l.content
        ]);
        insertedCourseLessons[cfg.id].push(insRes.rows[0]);
      }

      // Update course lesson_count
      await client.query(`
        UPDATE courses 
        SET lesson_count = $1, status = 'PUBLISHED' 
        WHERE course_id = $2
      `, [lessons.length, cfg.id]);
    }

    // 4. Re-link study_materials to new lessons
    console.log('Re-linking study materials to newly inserted lessons...');
    // N5 materials (materials 7, 8, 9) -> Course 1 first 3 lessons
    if (insertedCourseLessons[1] && insertedCourseLessons[1].length >= 3) {
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 7`, [insertedCourseLessons[1][0].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 8`, [insertedCourseLessons[1][1].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 9`, [insertedCourseLessons[1][2].lesson_id]);
    }
    // N4 materials (materials 10, 11, 12, 13) -> Course 2 first 4 lessons
    if (insertedCourseLessons[2] && insertedCourseLessons[2].length >= 4) {
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 10`, [insertedCourseLessons[2][0].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 11`, [insertedCourseLessons[2][1].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 12`, [insertedCourseLessons[2][2].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 13`, [insertedCourseLessons[2][3].lesson_id]);
    }
    // N3 materials (materials 14, 15, 16) -> Course 3 first 3 lessons
    if (insertedCourseLessons[3] && insertedCourseLessons[3].length >= 3) {
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 14`, [insertedCourseLessons[3][0].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 15`, [insertedCourseLessons[3][1].lesson_id]);
      await client.query(`UPDATE study_materials SET lesson_id = $1 WHERE material_id = 16`, [insertedCourseLessons[3][2].lesson_id]);
    }

    // 5. Set fragmented duplicate courses (IDs 101 to 130) to DRAFT
    console.log('Setting fragmented duplicate courses (IDs 101-130) to DRAFT...');
    await client.query(`
      UPDATE courses 
      SET status = 'DRAFT' 
      WHERE course_id >= 101 AND course_id <= 130
    `);

    await client.query('COMMIT');
    console.log('✅ REORGANIZATION COMPLETE SUCCESSFULLY!');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('❌ Error occurred, rolled back:', err);
    throw err;
  } finally {
    await client.end();
  }
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
