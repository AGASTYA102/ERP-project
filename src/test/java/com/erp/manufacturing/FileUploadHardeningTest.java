package com.erp.manufacturing;
 
import com.erp.manufacturing.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
 
import java.nio.file.Path;
 
import static org.junit.jupiter.api.Assertions.*;
 
@SpringBootTest
@ActiveProfiles("test")
public class FileUploadHardeningTest {
 
    @Autowired
    private FileService fileService;
 
    @TempDir
    Path tempDir;
 
    @Test
    public void mimeMismatchShouldFail() {
        // Mock a file that has a .jpg extension but actually contains an executable script
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "malicious.jpg",
                "image/jpeg",
                "#!/bin/bash\necho 'Hacked'".getBytes()
        );
 
        // Reflection to set a temporary upload dir
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
 
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.storeFile(maliciousFile);
        });
 
        assertTrue(exception.getMessage().contains("is not allowed"), "Message should indicate type is forbidden: " + exception.getMessage());
    }
 
    @Test
    public void doubleExtensionShouldBeHandledSafely() {
        // Tika should detect the real content type even with double extensions
        MockMultipartFile doubleExtFile = new MockMultipartFile(
                "file",
                "test.php.png",
                "image/png",
                "pseudo-png-content-header-and-data".getBytes()
        );
        
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
 
        // If the content is detected as text/plain or similar, it should fail
        assertThrows(IllegalArgumentException.class, () -> {
            fileService.storeFile(doubleExtFile);
        });
    }
 
    @Test
    public void validPdfShouldPass() {
        byte[] pdfContent = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj".getBytes();
        MockMultipartFile validPdf = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfContent
        );
 
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
 
        String storedPath = fileService.storeFile(validPdf);
        assertNotNull(storedPath);
        assertTrue(storedPath.endsWith(".pdf"));
    }
 
    @Test
    public void largeFileShouldBeRejected() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );
 
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
 
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.storeFile(largeFile);
        });
        assertTrue(exception.getMessage().contains("exceeds the 5MB maximum"));
    }
}
