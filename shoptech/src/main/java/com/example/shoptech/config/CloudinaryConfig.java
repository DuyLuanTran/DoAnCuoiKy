package com.example.shoptech.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        if (cloudName.isBlank()
                || apiKey.isBlank()
                || apiSecret.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình Cloudinary. "
                            + "Vui lòng kiểm tra CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY và CLOUDINARY_API_SECRET."
            );
        }

        return new Cloudinary(Map.of(
                "cloud_name", cloudName.strip(),
                "api_key", apiKey.strip(),
                "api_secret", apiSecret.strip(),
                "secure", true
        ));
    }
}