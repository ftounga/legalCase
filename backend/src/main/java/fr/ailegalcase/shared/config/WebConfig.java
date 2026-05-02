package fr.ailegalcase.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String LOCAL_DEV_ORIGIN = "http://localhost:4200";

    @Value("${app.allowed-frontend-urls:${app.frontend-url:http://localhost:4200}}")
    private String allowedFrontendUrlsCsv;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(parseAllowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private String[] parseAllowedOrigins() {
        Set<String> origins = new LinkedHashSet<>();
        Arrays.stream(allowedFrontendUrlsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(origins::add);
        origins.add(LOCAL_DEV_ORIGIN);
        return origins.toArray(new String[0]);
    }
}
