package fr.ailegalcase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.security.oauth2.client.registration.google.client-id=test-google-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
class LegalcaseBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
