package com.jlpp.modules.manager.lessons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.common.constants.SkillTypeEnum;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonDetail;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonRow;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LibraryItem;
import com.jlpp.modules.manager.lessons.repository.LessonDetailRepository;
import com.jlpp.modules.manager.lessons.repository.LessonLibraryRepository;
import com.jlpp.modules.manager.lessons.service.LessonDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonDetailServiceTest {

    @Mock
    private LessonDetailRepository detailRepository;

    @Mock
    private LessonLibraryRepository libraryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LessonDetailService detailService;

    @BeforeEach
    void setUp() {
        detailService = new LessonDetailService(detailRepository, libraryRepository, objectMapper);
    }

    @Test
    @DisplayName("UT_ML_DET_01: supportsLibrary returns true only for grammar, vocabulary, and kanji")
    void testSupportsLibrary() {
        assertThat(detailService.supportsLibrary(SkillTypeEnum.GRAMMAR)).isTrue();
        assertThat(detailService.supportsLibrary(SkillTypeEnum.VOCABULARY)).isTrue();
        assertThat(detailService.supportsLibrary(SkillTypeEnum.KANJI)).isTrue();
        assertThat(detailService.supportsLibrary(SkillTypeEnum.READING)).isFalse();
        assertThat(detailService.supportsLibrary(SkillTypeEnum.LISTENING)).isFalse();
    }

    @Test
    @DisplayName("UT_ML_DET_02: findLibrary routes to correct library repository methods based on skill")
    void testFindLibraryRoutesCorrectly() {
        LibraryItem item = new LibraryItem(1L, "Pattern", "Subtitle", "N5", "meta", "reading", "Meaning", "Ex", "ExVi");
        when(libraryRepository.findGrammarPoints("N5", "wa")).thenReturn(List.of(item));

        List<LibraryItem> grammarResults = detailService.findLibrary(SkillTypeEnum.GRAMMAR, "N5", "wa");
        assertThat(grammarResults).hasSize(1).contains(item);
        verify(libraryRepository).findGrammarPoints("N5", "wa");

        when(libraryRepository.findVocabularyEntries("N5", "taberu")).thenReturn(List.of(item));
        List<LibraryItem> vocabResults = detailService.findLibrary(SkillTypeEnum.VOCABULARY, "N5", "taberu");
        assertThat(vocabResults).hasSize(1);
        verify(libraryRepository).findVocabularyEntries("N5", "taberu");

        when(libraryRepository.findKanjiEntries("N5", "日")).thenReturn(List.of(item));
        List<LibraryItem> kanjiResults = detailService.findLibrary(SkillTypeEnum.KANJI, "N5", "日");
        assertThat(kanjiResults).hasSize(1);
        verify(libraryRepository).findKanjiEntries("N5", "日");
    }

    @Test
    @DisplayName("UT_ML_DET_03: findDetail returns optional detail from repository")
    void testFindDetail() {
        LessonDetail detail = new LessonDetail(50L, "GRAMMAR", 1L, "/audio/sample.mp3", "Theory content", "{\"word\":\"test\"}");
        when(detailRepository.findByLessonId(50L)).thenReturn(Optional.of(detail));

        Optional<LessonDetail> result = detailService.findDetail(50L);

        assertThat(result).isPresent();
        assertThat(result.get().audioUrl()).isEqualTo("/audio/sample.mp3");
        assertThat(result.get().body()).isEqualTo("Theory content");
    }

    @Test
    @DisplayName("UT_ML_DET_04: findListeningQuestions returns legacy audio question if questions array is empty")
    void testFindListeningQuestionsWithLegacyAudioUrl() {
        LessonDetail detail = new LessonDetail(51L, "LISTENING", null, "/audio/listening-test.mp3", "", "{}");
        when(detailRepository.findByLessonId(51L)).thenReturn(Optional.of(detail));

        List<Map<String, Object>> questions = detailService.findListeningQuestions(51L);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).get("questionAudioUrl")).isEqualTo("/audio/listening-test.mp3");
    }

    @Test
    @DisplayName("UT_ML_DET_05: findKanjiDetail parses title when detail payload is empty")
    void testFindKanjiDetailParsesTitleFallback() {
        LessonRow lesson = new LessonRow(
                60L, 1L, 1, "Hán tự: 日 - NHẬT", "Mô tả", "Nội dung",
                "Chương 1", 15, false, LessonStatusEnum.ACTIVE, "KANJI"
        );
        when(detailRepository.findByLessonId(60L)).thenReturn(Optional.empty());
        when(libraryRepository.findKanjiByCharacter("日")).thenReturn(Optional.of(Map.of("character", "日", "meaning", "NHẬT")));

        Map<String, Object> kanjiDetail = detailService.findKanjiDetail(lesson);

        assertThat(kanjiDetail).isNotNull();
        assertThat(kanjiDetail.get("character")).isEqualTo("日");
        assertThat(kanjiDetail.get("meaning")).isEqualTo("NHẬT");
    }
}
