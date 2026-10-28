package com.example.shoptech.service;
import com.cloudinary.Cloudinary;
import com.example.shoptech.config.IsolatedMediaConfig;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;
@Service @Profile("isolated") @DependsOn("isolatedDatabaseGuard")
public class IsolatedImageService extends CloudinaryService {
    public IsolatedImageService(Cloudinary unused){super(unused);}
    @Override public UploadedImage uploadImage(MultipartFile file){
        validateImage(file);
        try{
            byte[] bytes=file.getBytes();String format;
            if(bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))format="png";
            else if(bytes.length>=3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)format="jpg";
            else if(bytes.length>=12&&new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP"))format="webp";
            else throw new IllegalArgumentException("Nội dung ảnh không hợp lệ. Chọn ảnh PNG, JPEG hoặc WebP.");
            String name=UUID.randomUUID()+"."+format;Path dir=IsolatedMediaConfig.directory();Files.createDirectories(dir);Files.write(dir.resolve(name),bytes,StandardOpenOption.CREATE_NEW);
            return new UploadedImage("/sandbox-media/"+name,"local/"+name);
        }catch(java.io.IOException e){throw new IllegalStateException("Không thể lưu ảnh vào môi trường thử.",e);}
    }
}
