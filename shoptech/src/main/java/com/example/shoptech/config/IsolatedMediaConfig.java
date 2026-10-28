package com.example.shoptech.config;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;
@Configuration @Profile("isolated")
public class IsolatedMediaConfig implements WebMvcConfigurer {
    public static Path directory(){return Path.of(".local","media").toAbsolutePath().normalize();}
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry){registry.addResourceHandler("/sandbox-media/**").addResourceLocations(directory().toUri().toString()+"/");}
}
