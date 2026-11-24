package com.jeps.modules.question;

import com.jeps.modules.question.service.QuestionAudioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionAudioStorageServiceTest {

    @TempDir
    Path tempDir;

    private QuestionAudioStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new QuestionAudioStorageService(tempDir.toString());
    }

    @Test
    void store_nullFile_returnsNull() throws IOException {
        assertNull(storageService.store(null));
    }

    @Test
    void store_emptyFile_returnsNull() throws IOException {
        MockMultipartFile empty = new MockMultipartFile("file", "audio.mp3", "audio/mpeg", new byte[0]);

        assertNull(storageService.store(empty));
    }

    @Test
    void store_validMp3_returnsUrlPath() throws IOException {
        MockMultipartFile mp3 = new MockMultipartFile("file", "test.mp3", "audio/mpeg", new byte[]{1, 2, 3});

        String url = storageService.store(mp3);

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/questions/audio/"));
        assertTrue(url.endsWith(".mp3"));
    }

    @Test
    void store_validWav_returnsUrlPath() throws IOException {
        MockMultipartFile wav = new MockMultipartFile("file", "test.wav", "audio/wav", new byte[]{1, 2, 3});

        String url = storageService.store(wav);

        assertNotNull(url);
        assertTrue(url.endsWith(".wav"));
    }

    @Test
    void store_validOgg_returnsUrlPath() throws IOException {
        MockMultipartFile ogg = new MockMultipartFile("file", "test.ogg", "audio/ogg", new byte[]{1, 2, 3});

        String url = storageService.store(ogg);

        assertNotNull(url);
        assertTrue(url.endsWith(".ogg"));
    }

    @Test
    void store_audioMp4_returnsM4aExtension() throws IOException {
        MockMultipartFile m4a = new MockMultipartFile("file", "test.m4a", "audio/mp4", new byte[]{1, 2, 3});

        String url = storageService.store(m4a);

        assertNotNull(url);
        assertTrue(url.endsWith(".m4a"));
    }

    @Test
    void store_audioAac_returnsAacExtension() throws IOException {
        MockMultipartFile aac = new MockMultipartFile("file", "test.aac", "audio/aac", new byte[]{1, 2, 3});

        String url = storageService.store(aac);

        assertNotNull(url);
        assertTrue(url.endsWith(".aac"));
    }

    @Test
    void store_audioWebm_returnsWebmExtension() throws IOException {
        MockMultipartFile webm = new MockMultipartFile("file", "test.webm", "audio/webm", new byte[]{1, 2, 3});

        String url = storageService.store(webm);

        assertNotNull(url);
        assertTrue(url.endsWith(".webm"));
    }

    @Test
    void store_audioXWav_returnsWavExtension() throws IOException {
        MockMultipartFile xwav = new MockMultipartFile("file", "test.wav", "audio/x-wav", new byte[]{1, 2, 3});

        String url = storageService.store(xwav);

        assertTrue(url.endsWith(".wav"));
    }

    @Test
    void store_audioXM4a_returnsM4aExtension() throws IOException {
        MockMultipartFile xm4a = new MockMultipartFile("file", "test.m4a", "audio/x-m4a", new byte[]{1, 2, 3});

        String url = storageService.store(xm4a);

        assertTrue(url.endsWith(".m4a"));
    }

    @Test
    void store_audioMp3ContentType_returnsCorrectExtension() throws IOException {
        MockMultipartFile mp3 = new MockMultipartFile("file", "test.mp3", "audio/mp3", new byte[]{1, 2, 3});

        String url = storageService.store(mp3);

        assertTrue(url.endsWith(".mp3"));
    }

    @Test
    void store_urlContainsUUIDFilename() throws IOException {
        MockMultipartFile mp3 = new MockMultipartFile("file", "test.mp3", "audio/mpeg", new byte[]{1, 2, 3});

        String url = storageService.store(mp3);

        String filename = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
        assertEquals(36, filename.length());
        assertTrue(filename.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void store_createsDirectoryIfNotExist() throws IOException {
        Path nested = tempDir.resolve("nested/deep");
        QuestionAudioStorageService nestedService = new QuestionAudioStorageService(nested.toString());
        MockMultipartFile mp3 = new MockMultipartFile("file", "test.mp3", "audio/mpeg", new byte[]{1, 2, 3});

        String url = nestedService.store(mp3);

        assertNotNull(url);
        assertTrue(Files.exists(nested.resolve("questions/audio")));
    }

    @Test
    void store_fileWrittenToDisk() throws IOException {
        MockMultipartFile mp3 = new MockMultipartFile("file", "test.mp3", "audio/mpeg", new byte[]{10, 20, 30});

        storageService.store(mp3);

        Path audioDir = tempDir.resolve("questions/audio");
        assertTrue(Files.exists(audioDir));
        long fileCount = Files.list(audioDir).count();
        assertEquals(1, fileCount);
    }

    @Test
    void store_unsupportedContentType_throwsException() {
        MockMultipartFile video = new MockMultipartFile("file", "test.mp4", "video/mp4", new byte[]{1, 2, 3});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> storageService.store(video));
        assertTrue(ex.getMessage().contains("MP3, WAV, OGG, M4A, AAC"));
    }

    @Test
    void store_nullContentType_throwsException() {
        MockMultipartFile noType = new MockMultipartFile("file", "test.bin", null, new byte[]{1, 2, 3});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> storageService.store(noType));
        assertTrue(ex.getMessage().contains("MP3, WAV, OGG, M4A, AAC"));
    }

    @Test
    void store_fileExceedsMaxSize_throwsException() {
        MockMultipartFile huge = new MockMultipartFile("file", "huge.mp3", "audio/mpeg", new byte[1]) {
            @Override
            public long getSize() { return 51L * 1024 * 1024; }
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> storageService.store(huge));
        assertTrue(ex.getMessage().contains("50 MB"));
    }
}
