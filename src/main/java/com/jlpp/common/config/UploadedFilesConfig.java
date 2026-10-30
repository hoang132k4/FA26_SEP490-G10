package com.jlpp.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadedFilesConfig implements WebMvcConfigurer {

    private final String uploadDirectory;

    public UploadedFilesConfig(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        String location = Path.of(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
        this.uploadDirectory = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDirectory);
    }
}
