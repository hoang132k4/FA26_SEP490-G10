package com.jlpp.modules.question.service;

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
public class QuestionAudioStorageService {

    private static final long MAX_AUDIO_SIZE = 50L * 1024 * 1024;
    private static final Map<String, String> AUDIO_EXTENSIONS = Map.of(
            "audio/mpeg", ".mp3", "audio/mp3", ".mp3", "audio/wav", ".wav",
            "audio/x-wav", ".wav", "audio/ogg", ".ogg", "audio/mp4", ".m4a",
            "audio/x-m4a", ".m4a", "audio/aac", ".aac", "audio/webm", ".webm");

    private final Path uploadDirectory;

    public QuestionAudioStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("questions/audio").normalize();
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new IllegalArgumentException("File nghe không được vượt quá 50 MB.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        String extension = AUDIO_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Chỉ chấp nhận file âm thanh MP3, WAV, OGG, M4A, AAC hoặc WEBM.");
        }
        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }
        file.transferTo(destination);
        return "/uploads/questions/audio/" + filename;
    }
}
