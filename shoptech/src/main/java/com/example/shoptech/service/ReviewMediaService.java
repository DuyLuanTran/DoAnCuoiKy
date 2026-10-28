package com.example.shoptech.service;

import com.cloudinary.Cloudinary;
import com.example.shoptech.config.IsolatedMediaConfig;
import com.example.shoptech.entity.ReviewAttachment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;

/** Validates bytes as well as the declared MIME; never uses a client-supplied filename or URL. */
@Service
public class ReviewMediaService {
    private final Cloudinary cloudinary;
    private final boolean isolated;
    public ReviewMediaService(Cloudinary cloudinary,Environment env){this.cloudinary=cloudinary;isolated=env.acceptsProfiles(Profiles.of("isolated"));}
    public record Checked(MultipartFile file,String extension,String kind) {}
    public List<Checked> validate(List<MultipartFile> files) {
        List<Checked> result=new ArrayList<>();int images=0,videos=0;long total=0;
        if(files==null)return result;
        for(MultipartFile file:files){
            if(file.isEmpty())continue;
            String mime=Objects.toString(file.getContentType(),"").toLowerCase(Locale.ROOT);
            boolean video=Set.of("video/mp4","video/webm").contains(mime);
            if(file.getSize()>(video?20L:5L)*1024*1024)throw bad("Ảnh tối đa 5 MB, video tối đa 20 MB.");
            total+=file.getSize();if(total>25L*1024*1024)throw bad("Tổng tệp đính kèm tối đa 25 MB.");
            try {
                byte[] b=file.getBytes();String extension;
                if(mime.equals("image/png")&&b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))extension="png";
                else if(mime.equals("image/jpeg")&&b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)extension="jpg";
                else if(mime.equals("image/webp")&&b.length>=16&&ascii(b,0,4).equals("RIFF")&&ascii(b,8,4).equals("WEBP"))extension="webp";
                else if(mime.equals("video/mp4")&&b.length>=24&&ascii(b,4,4).equals("ftyp"))extension="mp4";
                else if(mime.equals("video/webm")&&b.length>=16&&(b[0]&255)==26&&(b[1]&255)==69&&(b[2]&255)==223&&(b[3]&255)==163&&ascii(b,0,Math.min(256,b.length)).contains("webm"))extension="webm";
                else throw bad("Tệp không hợp lệ. Dùng ảnh JPG/PNG/WebP hoặc video MP4/WebM.");
                if(!video&&!extension.equals("webp")){
                    try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(b))){
                        var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw bad("Không đọc được ảnh.");
                        var reader=readers.next();try{reader.setInput(input);if((long)reader.getWidth(0)*reader.getHeight(0)>40_000_000L)throw bad("Ảnh tối đa 40 triệu điểm ảnh.");if(reader.read(0)==null)throw bad("Ảnh không hợp lệ.");}finally{reader.dispose();}
                    }
                }
                if(video)videos++;else images++;
                if(images>3||videos>1)throw bad("Mỗi đánh giá tối đa 3 ảnh và 1 video.");
                result.add(new Checked(file,extension,video?"VIDEO":"IMAGE"));
            }catch(IOException e){throw bad("Không đọc được tệp đính kèm.");}
        }
        return result;
    }
    public ReviewAttachment upload(Checked checked) {
        String id=UUID.randomUUID().toString();
        try {
            if(isolated){
                String name="review-"+id+"."+checked.extension();Path dir=IsolatedMediaConfig.directory();Files.createDirectories(dir);
                Files.write(dir.resolve(name),checked.file().getBytes(),StandardOpenOption.CREATE_NEW);
                return new ReviewAttachment("/sandbox-media/"+name,"local/"+name,checked.kind());
            }
            Map<?,?> result=cloudinary.uploader().upload(checked.file().getBytes(),Map.of("resource_type",checked.kind().equals("VIDEO")?"video":"image","folder","shoptech/reviews","public_id",id,"overwrite",false,"allowed_formats",List.of(checked.extension())));
            if(!(result.get("secure_url") instanceof String url)||!url.startsWith("https://")||!(result.get("public_id") instanceof String storage))throw new IOException("Invalid upload response");
            return new ReviewAttachment(url,storage,checked.kind());
        }catch(IOException e){throw new IllegalStateException("Không thể lưu tệp đánh giá. Vui lòng thử lại.",e);}
    }
    public void discard(ReviewAttachment attachment){
        try{
            if(isolated){String name=attachment.getStorageId().replaceFirst("^local/","");if(name.matches("review-[a-f0-9-]+\\.(png|jpg|webp|mp4|webm)"))Files.deleteIfExists(IsolatedMediaConfig.directory().resolve(name));}
            else cloudinary.uploader().destroy(attachment.getStorageId(),Map.of("resource_type",attachment.getKind().equals("VIDEO")?"video":"image"));
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Review media cleanup failed for {}",attachment.getStorageId());}
    }
    private static String ascii(byte[] b,int offset,int size){return new String(b,offset,size,StandardCharsets.US_ASCII);}
    private static IllegalArgumentException bad(String message){return new IllegalArgumentException(message);}
}
