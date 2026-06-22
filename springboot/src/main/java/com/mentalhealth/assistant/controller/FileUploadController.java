package com.mentalhealth.assistant.controller;

import com.mentalhealth.assistant.common.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        // 使用项目目录作为相对路径的基准，避免解析到 Tomcat 临时目录
        String baseDir = System.getProperty("user.dir");
        Path resolvedPath = Paths.get(uploadPath);
        if (!resolvedPath.isAbsolute()) {
            resolvedPath = Paths.get(baseDir, uploadPath);
        }
        this.uploadDir = resolvedPath;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDir, e);
        }
    }

    @PostMapping("/upload")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") String businessId,
            @RequestParam("businessField") String businessField) {

        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = businessId + ext;

        // 按业务类型和字段组织目录: uploads/{businessType}/{businessField}/
        Path targetDir = uploadDir.resolve(businessType).resolve(businessField);
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            java.io.File destFile = targetFile.toFile();
            if (destFile == null) {
                throw new IOException("无法创建目标文件");
            }
            file.transferTo(destFile);

            // 返回可访问的URL路径
            String fileUrl = "/uploads/" + businessType + "/" + businessField + "/" + filename;
            return Result.success(fileUrl);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}
