package com.erp.manufacturing.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    private final Tika tika = new Tika();

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket:erp-uploads}")
    private String supabaseBucket;

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the 50MB maximum limit");
        }
        
        try {
            // 1. Sanitize original filename and extract extension
            String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
            String extension = getFileExtension(originalFileName).toLowerCase();
            
            // 2. Validate Extension
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("File extension '" + extension + "' is not allowed.");
            }

            // 3. Validate Magic Number (MIME Type) using Tika
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                log.warn("MIME type mismatch detected for file {}. Detected: {}", originalFileName, mimeType);
                throw new IllegalArgumentException("File content type '" + mimeType + "' is not allowed.");
            }

            // 4. Generate Strict UUID Filename
            String fileName = UUID.randomUUID().toString() + "." + extension;

            // 5. Upload to Supabase Storage via REST
            String endpoint = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + fileName;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.parseMediaType(mimeType));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, 
                    HttpMethod.POST, 
                    requestEntity, 
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase upload failed: {}", response.getBody());
                throw new RuntimeException("Cloud storage provider rejected the upload.");
            }

            log.info("Successfully uploaded file to Supabase: {} as {}", originalFileName, fileName);
            
            // Return Public URL
            return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not read file for upload.", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload to remote storage.", ex);
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
