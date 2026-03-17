package fr.ailegalcase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.security.oauth2.client.registration.google.client-id=test-google-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
		"spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
		"spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
class LegalcaseBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
