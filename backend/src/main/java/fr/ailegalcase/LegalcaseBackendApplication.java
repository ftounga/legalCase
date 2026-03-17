package fr.ailegalcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LegalcaseBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegalcaseBackendApplication.class, args);
	}

}
