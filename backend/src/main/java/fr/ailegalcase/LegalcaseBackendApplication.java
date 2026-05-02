package fr.ailegalcase;

import fr.ailegalcase.backlog.BacklogProperties;
import fr.ailegalcase.document.vision.VisionProperties;
import fr.ailegalcase.ocr.OcrProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({ OcrProperties.class, VisionProperties.class, BacklogProperties.class })
public class LegalcaseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegalcaseBackendApplication.class, args);
	}

}
