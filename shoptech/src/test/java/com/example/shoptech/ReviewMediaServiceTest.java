package com.example.shoptech;

import com.cloudinary.Cloudinary;
import com.example.shoptech.service.ReviewMediaService;
import com.example.shoptech.config.IsolatedMediaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReviewMediaServiceTest {
    ReviewMediaService service(){MockEnvironment env=new MockEnvironment();env.setActiveProfiles("isolated");return new ReviewMediaService(new Cloudinary(Map.of("cloud_name","unused","api_key","unused","api_secret","unused")),env);}
    MockMultipartFile image() throws Exception {return new MockMultipartFile("files","../../unsafe.png","image/png",new ClassPathResource("static/user/img/product01.png").getContentAsByteArray());}
    @Test void realImageIsValidatedAndSavedWithGeneratedNameOnly() throws Exception {
        var service=service();var checked=service.validate(List.of(image()));assertEquals("IMAGE",checked.get(0).kind());var uploaded=service.upload(checked.get(0));
        try {assertTrue(uploaded.getUrl().matches("/sandbox-media/review-[a-f0-9-]+\\.png"));assertTrue(Files.exists(IsolatedMediaConfig.directory().resolve(uploaded.getStorageId().substring(6))));}
        finally {service.discard(uploaded);}
        assertFalse(Files.exists(IsolatedMediaConfig.directory().resolve(uploaded.getStorageId().substring(6))));
    }
    @Test void svgScriptAndMimeSpoofingAreRejected() throws Exception {
        var service=service();assertThrows(IllegalArgumentException.class,()->service.validate(List.of(new MockMultipartFile("files","x.png","image/png","<script>alert(1)</script>".getBytes()))));
        assertThrows(IllegalArgumentException.class,()->service.validate(List.of(new MockMultipartFile("files","x.svg","image/svg+xml","<svg/>".getBytes()))));
        byte[] bytes=image().getBytes();assertThrows(IllegalArgumentException.class,()->service.validate(List.of(new MockMultipartFile("files","x.jpg","image/jpeg",bytes))));
    }
    @Test void moreThanThreeImagesAndOversizedImageAreRejected() throws Exception {
        var service=service();var image=image();assertThrows(IllegalArgumentException.class,()->service.validate(List.of(image,image,image,image)));
        MultipartFile oversized=new MockMultipartFile("files","x.png","image/png",image.getBytes()){@Override public long getSize(){return 5L*1024*1024+1;}};
        assertThrows(IllegalArgumentException.class,()->service.validate(List.of(oversized)));
    }
    @Test void videoSignatureAndCountAndSizeAreChecked() {
        var service=service();byte[] bytes=new byte[32];System.arraycopy("ftypisom".getBytes(java.nio.charset.StandardCharsets.US_ASCII),0,bytes,4,8);
        var video=new MockMultipartFile("files","video.mp4","video/mp4",bytes);assertEquals("VIDEO",service.validate(List.of(video)).get(0).kind());
        assertThrows(IllegalArgumentException.class,()->service.validate(List.of(video,video)));
        assertThrows(IllegalArgumentException.class,()->service.validate(List.of(new MockMultipartFile("files","x.mp4","video/mp4","not a video".getBytes()))));
        MultipartFile oversized=new MockMultipartFile("files","x.mp4","video/mp4",bytes){@Override public long getSize(){return 20L*1024*1024+1;}};assertThrows(IllegalArgumentException.class,()->service.validate(List.of(oversized)));
    }
    @Test void totalAttachmentsCannotExceedTwentyFiveMegabytes() throws Exception {
        var service=service();var image=image();MultipartFile large=new MockMultipartFile("files","x.png","image/png",image.getBytes()){@Override public long getSize(){return 5L*1024*1024;}};
        byte[] bytes=new byte[32];System.arraycopy("ftypisom".getBytes(java.nio.charset.StandardCharsets.US_ASCII),0,bytes,4,8);MultipartFile video=new MockMultipartFile("files","x.mp4","video/mp4",bytes){@Override public long getSize(){return 20L*1024*1024;}};
        assertThrows(IllegalArgumentException.class,()->service.validate(List.of(video,large,large)));
    }
}
