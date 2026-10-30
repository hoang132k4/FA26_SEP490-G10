package com.jlpp.common.config;

import com.jlpp.modules.flashcard.entity.Flashcard;
import com.jlpp.modules.flashcard.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FlashcardRepository flashcardRepository;
    private final com.jlpp.modules.quiz.service.QuizService quizService;

    @Override
    public void run(String... args) {
        log.info("Checking database and seed data...");
        seedFlashcardsIfEmpty();
        seedQuizQuestionsIfEmpty();
    }

    private void seedQuizQuestionsIfEmpty() {
        try {
            // Seed quiz questions for key lessons (e.g. 109 - N2, 84 - N4, 85 - N4)
            for (Long lid : List.of(109L, 84L, 85L)) {
                quizService.seedQuizQuestionsForLesson(lid);
            }
        } catch (Exception e) {
            log.warn("Failed to seed quiz questions: {}", e.getMessage());
        }
    }

    private void seedFlashcardsIfEmpty() {
        if (flashcardRepository.count() > 0) {
            log.info("Flashcards already present (count={}). Skipping seed.", flashcardRepository.count());
            return;
        }

        log.info("Seeding initial authentic Japanese flashcards into Supabase...");
        List<Flashcard> seeds = List.of(
                // Lesson 72 - N5
                Flashcard.builder()
                        .lessonId(72L)
                        .vocabulary("桜")
                        .reading("さくら (sakura)")
                        .meaning("Hoa anh đào")
                        .exampleSentence("日本の春は桜の花がとても美しいです。")
                        .jlptLevel("N5")
                        .reviewStatus("MASTERED")
                        .frontContent("Biểu tượng mùa xuân của Nhật Bản")
                        .backContent("Danh từ chỉ hoa anh đào, nở vào khoảng tháng 3 - tháng 4.")
                        .build(),
                Flashcard.builder()
                        .lessonId(72L)
                        .vocabulary("先生")
                        .reading("せんせい (sensei)")
                        .meaning("Thầy giáo, cô giáo, bác sĩ")
                        .exampleSentence("山田先生はとても親切で熱心です。")
                        .jlptLevel("N5")
                        .reviewStatus("REVIEWING")
                        .frontContent("Từ xưng hô kính trọng với người dạy học")
                        .backContent("Hán tự: TIÊN SINH, người sinh ra trước, người dẫn dắt.")
                        .build(),
                Flashcard.builder()
                        .lessonId(72L)
                        .vocabulary("日本語")
                        .reading("にほんご (nihongo)")
                        .meaning("Tiếng Nhật")
                        .exampleSentence("毎日JLPPで日本語を勉強しています。")
                        .jlptLevel("N5")
                        .reviewStatus("NEW")
                        .frontContent("Ngôn ngữ của xứ sở mặt trời mọc")
                        .backContent("Hán tự: NHẬT BẢN NGỮ.")
                        .build(),
                Flashcard.builder()
                        .lessonId(72L)
                        .vocabulary("友達")
                        .reading("ともだち (tomodachi)")
                        .meaning("Bạn bè, bạn thân")
                        .exampleSentence("週末に友達と一緒に映画を見に行きます。")
                        .jlptLevel("N5")
                        .reviewStatus("NEW")
                        .frontContent("Người đồng hành thân thiết")
                        .backContent("Hán tự: HỮU ĐẠT.")
                        .build(),

                // Lesson 84 - N4
                Flashcard.builder()
                        .lessonId(84L)
                        .vocabulary("準備")
                        .reading("じゅんび (junbi)")
                        .meaning("Chuẩn bị, sắp đặt trước")
                        .exampleSentence("旅行の準備はもう全部終わりましたか。")
                        .jlptLevel("N4")
                        .reviewStatus("REVIEWING")
                        .frontContent("Hành động sắp xếp trước khi làm việc gì")
                        .backContent("Hán tự: CHUẨN BỊ. Đi cùng động từ 準備する.")
                        .build(),
                Flashcard.builder()
                        .lessonId(84L)
                        .vocabulary("経験")
                        .reading("けいけん (keiken)")
                        .meaning("Kinh nghiệm, trải nghiệm thực tế")
                        .exampleSentence("日本で働いた経験はとても貴重です。")
                        .jlptLevel("N4")
                        .reviewStatus("MASTERED")
                        .frontContent("Những điều đã trải qua và học hỏi được")
                        .backContent("Hán tự: KINH NGHIỆM. Mẫu câu: V-た ことがある (đã từng có kinh nghiệm).")
                        .build(),
                Flashcard.builder()
                        .lessonId(84L)
                        .vocabulary("約束")
                        .reading("やくそく (yakusoku)")
                        .meaning("Lời hứa, cuộc hẹn")
                        .exampleSentence("友達と会う約束を覚えています。")
                        .jlptLevel("N4")
                        .reviewStatus("NEW")
                        .frontContent("Giao hẹn, cam kết với người khác")
                        .backContent("Hán tự: ƯỚC THÚC. 約束を守る (giữ lời hứa), 約束を破る (thất hứa).")
                        .build(),
                Flashcard.builder()
                        .lessonId(84L)
                        .vocabulary("習慣")
                        .reading("しゅうかん (shuukan)")
                        .meaning("Thói quen, phong tục tập quán")
                        .exampleSentence("毎朝運動する良い習慣をつけましょう。")
                        .jlptLevel("N4")
                        .reviewStatus("NEW")
                        .frontContent("Hành vi lặp đi lặp lại thành nề nếp")
                        .backContent("Hán tự: TẬP QUÁN.")
                        .build(),
                Flashcard.builder()
                        .lessonId(84L)
                        .vocabulary("案内")
                        .reading("あんない (annai)")
                        .meaning("Hướng dẫn, dẫn đường")
                        .exampleSentence("東京の街を親切に案内してくれました。")
                        .jlptLevel("N4")
                        .reviewStatus("REVIEWING")
                        .frontContent("Chỉ dẫn cho người khác tham quan hoặc hiểu rõ")
                        .backContent("Hán tự: ÁN NỘI. 案内する (hướng dẫn).")
                        .build(),

                // Lesson 85 - N4
                Flashcard.builder()
                        .lessonId(85L)
                        .vocabulary("複雑")
                        .reading("ふくざつ (fukuzatsu)")
                        .meaning("Phức tạp, rắc rối")
                        .exampleSentence("この文法ルールは少し複雑ですが、慣れれば簡単です。")
                        .jlptLevel("N4")
                        .reviewStatus("NEW")
                        .frontContent("Trái nghĩa với 簡単 (đơn giản)")
                        .backContent("Hán tự: PHỨC TẠP. Tính từ đuôi な.")
                        .build(),
                Flashcard.builder()
                        .lessonId(85L)
                        .vocabulary("簡単")
                        .reading("かんたん (kantan)")
                        .meaning("Đơn giản, dễ hiểu")
                        .exampleSentence("このアプリを使えば、単語の暗記がとても簡単になります。")
                        .jlptLevel("N4")
                        .reviewStatus("MASTERED")
                        .frontContent("Trái nghĩa với 複雑 (phức tạp)")
                        .backContent("Hán tự: GIẢN ĐƠN. Tính từ đuôi な.")
                        .build(),
                Flashcard.builder()
                        .lessonId(85L)
                        .vocabulary("説明")
                        .reading("せつめい (setsumei)")
                        .meaning("Giải thích, thuyết minh")
                        .exampleSentence("先生が分かりやすく丁寧に説明してくれました。")
                        .jlptLevel("N4")
                        .reviewStatus("REVIEWING")
                        .frontContent("Làm sáng tỏ ý nghĩa hoặc quy tắc")
                        .backContent("Hán tự: THUYẾT MINH. 説明する.")
                        .build()
        );

        flashcardRepository.saveAll(seeds);
        log.info("Successfully seeded {} flashcards into Supabase!", seeds.size());
    }
}
