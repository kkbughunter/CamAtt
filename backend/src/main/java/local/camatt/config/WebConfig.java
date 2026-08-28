package local.camatt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String storagePath;
    public WebConfig(@Value("${camatt.storage-path}") String storagePath) { this.storagePath = storagePath; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) { registry.addResourceHandler("/storage/**").addResourceLocations(Paths.get(storagePath).toAbsolutePath().normalize().toUri().toString()); }
    @Override public void addCorsMappings(CorsRegistry registry) { registry.addMapping("/api/**").allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS"); }
}

