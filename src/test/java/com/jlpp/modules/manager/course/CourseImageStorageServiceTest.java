package com.jlpp.modules.manager.course;

import com.jlpp.modules.manager.courses.service.CourseImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseImageStorageServiceTest {

    private CourseImageStorageService storageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        storageService = new CourseImageStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("UT_MC_IMG_01: store should return null when file is null or empty")
    void testStoreNullOrEmptyFile() throws IOException {
        assertThat(storageService.store(null)).isNull();

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThat(storageService.store(emptyFile)).isNull();
    }

    @Test
    @DisplayName("UT_MC_IMG_02: store should throw exception when file exceeds 5MB")
    void testStoreFileExceedsLimit() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        assertThatThrownBy(() -> storageService.store(largeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vượt quá 5 MB");
    }

    @Test
    @DisplayName("UT_MC_IMG_03: store should throw exception for unsupported mime type")
    void testStoreUnsupportedMimeType() {
        byte[] bytes = "Some text content".getBytes();
        MockMultipartFile textFile = new MockMultipartFile("file", "doc.txt", "text/plain", bytes);

        assertThatThrownBy(() -> storageService.store(textFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ chấp nhận ảnh PNG, JPG, WEBP hoặc GIF");
    }

    @Test
    @DisplayName("UT_MC_IMG_04: store should throw exception when file signature does not match mime type")
    void testStoreSignatureMismatch() {
        byte[] fakePngBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
        MockMultipartFile badFile = new MockMultipartFile("file", "fake.png", "image/png", fakePngBytes);

        assertThatThrownBy(() -> storageService.store(badFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nội dung file không khớp với định dạng ảnh đã chọn");
    }

    @Test
    @DisplayName("UT_MC_IMG_05: store should successfully save valid PNG image")
    void testStoreValidPngImage() throws IOException {
        byte[] pngHeader = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00 };
        MockMultipartFile validPng = new MockMultipartFile("file", "test.png", "image/png", pngHeader);

        String savedPath = storageService.store(validPng);

        assertThat(savedPath).isNotNull().startsWith("/uploads/courses/").endsWith(".png");
    }

    @Test
    @DisplayName("UT_MC_IMG_06: store should successfully save valid JPEG image")
    void testStoreValidJpegImage() throws IOException {
        byte[] jpegHeader = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x01 };
        MockMultipartFile validJpeg = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegHeader);

        String savedPath = storageService.store(validJpeg);

        assertThat(savedPath).isNotNull().startsWith("/uploads/courses/").endsWith(".jpg");
    }
}

