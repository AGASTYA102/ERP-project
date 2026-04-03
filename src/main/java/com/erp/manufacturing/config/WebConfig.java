package com.erp.manufacturing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir);
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();
        
        // This maps the request path to the actual file location.
        // It covers both typical linux and windows pathing behavior from the config.
        String resourcePath = uploadDir;
        if (resourcePath.startsWith("./")) {
            resourcePath = resourcePath.substring(2);
        }
        
        registry.addResourceHandler("/" + resourcePath + "/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/");
    }
}
