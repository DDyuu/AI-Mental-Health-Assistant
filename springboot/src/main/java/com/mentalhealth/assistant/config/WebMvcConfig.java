package com.mentalhealth.assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 使 /uploads/** 映射到文件系统的上传目录
        String baseDir = System.getProperty("user.dir");
        Path resolvedPath = Paths.get(uploadPath);
        if (!resolvedPath.isAbsolute()) {
            resolvedPath = Paths.get(baseDir, uploadPath);
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + resolvedPath.toString().replace("\\", "/") + "/");
    }
}
