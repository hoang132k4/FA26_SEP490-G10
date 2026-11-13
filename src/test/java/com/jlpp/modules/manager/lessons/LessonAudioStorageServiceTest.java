package com.jlpp.modules.manager.lessons;

import com.jlpp.modules.manager.lessons.service.LessonAudioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonAudioStorageServiceTest {

    private LessonAudioStorageService audioStorageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        audioStorageService = new LessonAudioStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("UT_ML_AUDIO_01: applyQuestionAudioFiles throws exception when questionAudioUrl list is missing")
    void testMissingAudioUrlListThrowsException() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        assertThatThrownBy(() -> audioStorageService.applyQuestionAudioFiles(params, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mỗi câu hỏi nghe cần chọn file nghe hoặc nhập URL");
    }

    @Test
    @DisplayName("UT_ML_AUDIO_02: applyQuestionAudioFiles validates external URL format")
    void testValidExternalUrlAccepted() throws IOException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("questionAudioUrl", new ArrayList<>(List.of("https://example.com/audio/sample.mp3")));
        params.put("questionAudioMode", List.of("url"));

        audioStorageService.applyQuestionAudioFiles(params, List.of());

        assertThat(params.getFirst("questionAudioUrl")).isEqualTo("https://example.com/audio/sample.mp3");
    }

    @Test
    @DisplayName("UT_ML_AUDIO_03: applyQuestionAudioFiles rejects invalid URL scheme")
    void testInvalidUrlSchemeRejected() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("questionAudioUrl", new ArrayList<>(List.of("ftp://example.com/audio.mp3")));
        params.put("questionAudioMode", List.of("url"));

        assertThatThrownBy(() -> audioStorageService.applyQuestionAudioFiles(params, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL bài nghe phải bắt đầu bằng http://, https:// hoặc đường dẫn nội bộ /uploads/");
    }

    @Test
    @DisplayName("UT_ML_AUDIO_04: applyQuestionAudioFiles throws exception when mode is file but file is empty")
    void testFileModeWithEmptyFileThrowsException() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("questionAudioUrl", new ArrayList<>(List.of("placeholder")));
        params.put("questionAudioMode", List.of("file"));

        MockMultipartFile emptyFile = new MockMultipartFile("audio", "sample.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> audioStorageService.applyQuestionAudioFiles(params, List.of(emptyFile)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cần chọn file nghe");
    }

    @Test
    @DisplayName("UT_ML_AUDIO_05: applyQuestionAudioFiles successfully stores valid MP3 file")
    void testStoreValidMp3File() throws IOException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("questionAudioUrl", new ArrayList<>(List.of("placeholder")));
        params.put("questionAudioMode", List.of("file"));

        MockMultipartFile mp3File = new MockMultipartFile(
                "audio", "listening_1.mp3", "audio/mpeg", "ID3-audio-data-mock".getBytes()
        );

        audioStorageService.applyQuestionAudioFiles(params, List.of(mp3File));

        String uploadedUrl = params.getFirst("questionAudioUrl");
        assertThat(uploadedUrl).isNotNull().startsWith("/uploads/lessons/audio/").endsWith(".mp3");
    }
}

