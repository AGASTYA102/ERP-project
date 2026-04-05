package com.erp.manufacturing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Stores an uploaded file securely.
     * Generates a UUID prefix for uniqueness and validates the file extension.
     * Prevents path traversal vulnerabilities.
     * 
     * @param file The multipart file to store
     * @return The absolute path to the stored file
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the 5MB maximum limit");
        }
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Prevent path traversal — strip directory components
            String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();

            // Validate file extension
            String extension = getFileExtension(originalFileName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("File type '" + extension + "' is not allowed. "
                        + "Accepted types: " + ALLOWED_EXTENSIONS);
            }

            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Path targetLocation = uploadPath.resolve(fileName).normalize().toAbsolutePath();
            Path normalizedUploadPath = uploadPath.normalize().toAbsolutePath();
            if (!targetLocation.startsWith(normalizedUploadPath)) {
                throw new SecurityException("Cannot store file outside target directory.");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}
