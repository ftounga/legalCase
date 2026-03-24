package fr.ailegalcase.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.frontend-url-staging:#{null}}")
    private String frontendUrlStaging;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        java.util.List<String> origins = new java.util.ArrayList<>();
        origins.add(frontendUrl);
        origins.add("http://localhost:4200");
        if (frontendUrlStaging != null) origins.add(frontendUrlStaging);
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
