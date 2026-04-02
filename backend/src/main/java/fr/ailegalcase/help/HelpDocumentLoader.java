package fr.ailegalcase.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

@Component
public class HelpDocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(HelpDocumentLoader.class);

    private final String documentation;

    public HelpDocumentLoader() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:help/*.md");
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

        StringBuilder sb = new StringBuilder();
        for (Resource resource : resources) {
            sb.append(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            sb.append("\n\n");
        }
        this.documentation = sb.toString();
        log.info("Help documentation loaded: {} file(s), {} chars", resources.length, this.documentation.length());
    }

    public String getDocumentation() {
        return documentation;
    }
}
