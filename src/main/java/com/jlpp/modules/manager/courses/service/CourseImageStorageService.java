package com.jlpp.modules.manager.courses.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CourseImageStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path uploadDirectory;

    public CourseImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("courses").normalize();
    }

    public boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    public String store(MultipartFile file) throws IOException {
        if (!hasFile(file)) return null;
        validate(file);
        String extension = IMAGE_EXTENSIONS.get(file.getContentType().trim().toLowerCase(Locale.ROOT));

        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Tên file ảnh không hợp lệ.");
        }
        file.transferTo(destination);
        return "/uploads/courses/" + filename;
    }

    public void validate(MultipartFile file) throws IOException {
        if (!hasFile(file)) return;
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh khóa học không được vượt quá 5 MB.");
        }
        String contentType = file.getContentType();
        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        String extension = IMAGE_EXTENSIONS.get(normalizedType);
        if (extension == null) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh PNG, JPG, WEBP hoặc GIF.");
        }
        byte[] header = file.getBytes();
        boolean validSignature = switch (normalizedType) {
            case "image/png" -> header.length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50
                    && header[2] == 0x4e && header[3] == 0x47 && header[4] == 0x0d && header[5] == 0x0a
                    && header[6] == 0x1a && header[7] == 0x0a;
            case "image/jpeg" -> header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff;
            case "image/gif" -> header.length >= 6 && new String(header, 0, 6, java.nio.charset.StandardCharsets.US_ASCII)
                    .matches("GIF8[79]a");
            case "image/webp" -> header.length >= 12 && new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
        if (!validSignature) throw new IllegalArgumentException("Nội dung file không khớp với định dạng ảnh đã chọn.");
    }
}
