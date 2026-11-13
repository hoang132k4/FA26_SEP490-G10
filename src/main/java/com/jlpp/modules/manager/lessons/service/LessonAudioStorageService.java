package com.jlpp.modules.manager.lessons.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LessonAudioStorageService {
    private static final long MAX_AUDIO_SIZE = 50L * 1024 * 1024;
    private static final Map<String, String> AUDIO_EXTENSIONS = Map.of(
            "audio/mpeg", ".mp3", "audio/mp3", ".mp3", "audio/wav", ".wav",
            "audio/x-wav", ".wav", "audio/ogg", ".ogg", "audio/mp4", ".m4a",
            "audio/x-m4a", ".m4a", "audio/aac", ".aac", "audio/webm", ".webm");

    private final Path uploadDirectory;

    public LessonAudioStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("lessons/audio").normalize();
    }

    public void applyQuestionAudioFiles(MultiValueMap<String, String> params, List<MultipartFile> files) throws IOException {
        List<String> urls = params.get("questionAudioUrl");
        List<String> modes = params.get("questionAudioMode");
        int fileIndex = 0;
        if (urls == null || urls.isEmpty()) throw new IllegalArgumentException("Mỗi câu hỏi nghe cần chọn file nghe hoặc nhập URL.");
        for (int index = 0; index < urls.size(); index++) {
            String mode = modes == null || index >= modes.size() ? "url" : modes.get(index);
            String url = urls.get(index) == null ? "" : urls.get(index).trim();
            if ("file".equalsIgnoreCase(mode)) {
                MultipartFile file = files != null && fileIndex < files.size() ? files.get(fileIndex) : null;
                fileIndex++;
                if (!hasFile(file)) throw new IllegalArgumentException("Câu hỏi nghe số " + (index + 1) + " cần chọn file nghe.");
                url = store(file);
            } else if (!"url".equalsIgnoreCase(mode)) {
                throw new IllegalArgumentException("Vui lòng chọn file nghe hoặc URL cho câu hỏi số " + (index + 1) + ".");
            }
            if (url.isBlank()) throw new IllegalArgumentException("Câu hỏi nghe số " + (index + 1) + " cần chọn file nghe hoặc nhập URL.");
            if (!((url.startsWith("/") && !url.startsWith("//")) || url.matches("(?i)^https?://.+"))) {
                throw new IllegalArgumentException("URL bài nghe phải bắt đầu bằng http://, https:// hoặc đường dẫn nội bộ /uploads/.");
            }
            urls.set(index, url);
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        String extension = AUDIO_EXTENSIONS.get(contentType);
        if (extension == null) {
            String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            if (originalName.endsWith(".mp3")) extension = ".mp3";
            else if (originalName.endsWith(".wav")) extension = ".wav";
            else if (originalName.endsWith(".ogg")) extension = ".ogg";
            else if (originalName.endsWith(".m4a")) extension = ".m4a";
            else if (originalName.endsWith(".aac")) extension = ".aac";
            else if (originalName.endsWith(".webm")) extension = ".webm";
        }
        if (extension == null) throw new IllegalArgumentException("Chỉ chấp nhận file âm thanh MP3, WAV, OGG, M4A, AAC hoặc WEBM.");
        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadDirectory)) throw new IllegalArgumentException("Tên file nghe không hợp lệ.");
        file.transferTo(destination);
        return "/uploads/lessons/audio/" + filename;
    }
}
