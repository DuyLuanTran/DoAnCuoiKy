package com.example.shoptech.service;

import com.cloudinary.Cloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@org.springframework.context.annotation.Profile("!isolated")
public class CloudinaryService {

    private static final Logger log =
            LoggerFactory.getLogger(CloudinaryService.class);

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public UploadedImage uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "resource_type", "image",
                            "folder", "shoptech/uploads",
                            "public_id", UUID.randomUUID().toString(),
                            "overwrite", false,
                            "allowed_formats", List.of(
                                    "jpg",
                                    "jpeg",
                                    "png",
                                    "webp"
                            )
                    )
            );

            Object secureUrl = result.get("secure_url");
            Object publicId = result.get("public_id");

            if (!(secureUrl instanceof String imageUrl)
                    || imageUrl.isBlank()
                    || !(publicId instanceof String imagePublicId)
                    || imagePublicId.isBlank()) {
                throw new IllegalStateException(
                        "Cloudinary không trả về thông tin ảnh hợp lệ."
                );
            }

            return new UploadedImage(imageUrl, imagePublicId);
        } catch (IOException exception) {
            log.error("Không thể upload ảnh lên Cloudinary.", exception);

            throw new IllegalStateException(
                    "Upload ảnh thất bại. Vui lòng thử lại.",
                    exception
            );
        }
    }

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ảnh."
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Ảnh không được vượt quá 5 MB."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(
                        contentType.toLowerCase(Locale.ROOT)
                )) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WEBP."
            );
        }
    }

    public record UploadedImage(
            String secureUrl,
            String publicId
    ) {
    }
}
