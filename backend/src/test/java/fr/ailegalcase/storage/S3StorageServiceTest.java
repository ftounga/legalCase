package fr.ailegalcase.storage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SF-INFRA-08 : couvre le branchement CloudFront du service de stockage.
 *
 * <ul>
 *   <li>Fallback S3 quand {@code legalcase.cloudfront.domain} est absent.</li>
 *   <li>URL CloudFront signée quand domain + key-pair-id + private-key présents.</li>
 *   <li>L'URL signée contient bien {@code Policy/Expires}, {@code Signature}, {@code Key-Pair-Id}.</li>
 * </ul>
 */
class S3StorageServiceTest {

    private static String testPrivateKeyPem;
    private static final String TEST_KEY_PAIR_ID = "K2JUNITTEST123";
    private static final String TEST_CLOUDFRONT_DOMAIN = "d2oaldre5efpif.cloudfront.net";

    private S3Client s3Client;
    private S3Presigner presigner;
    private StorageProperties storageProps;

    @BeforeAll
    static void generateTestKeyPair() throws Exception {
        // Génère une paire RSA 2048 éphémère pour les tests — pas de secret en repo.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        testPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(kp.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        presigner = mock(S3Presigner.class);
        storageProps = new StorageProperties();
        storageProps.setBucket("test-bucket");
        storageProps.setRegion("eu-west-3");
    }

    @Test
    void presignedDownloadUrl_withoutCloudFrontDomain_returnsS3PresignedUrl() throws Exception {
        // Aucun domain CloudFront → fallback S3 présignée
        CloudFrontProperties cfProps = new CloudFrontProperties();
        // domain volontairement null

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        URL fakeUrl = URI.create("https://s3.eu-west-3.amazonaws.com/test-bucket/some-key?X-Amz-Signature=abc").toURL();
        when(presigned.url()).thenReturn(fakeUrl);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        S3StorageService service = new S3StorageService(s3Client, presigner, storageProps,
                cfProps, Optional.empty());

        String result = service.presignedDownloadUrl("some-key", 15);

        assertThat(result).contains("s3.eu-west-3.amazonaws.com").contains("X-Amz-Signature");
    }

    @Test
    void presignedDownloadUrl_withCloudFrontDomain_returnsSignedCloudFrontUrl() {
        CloudFrontProperties cfProps = new CloudFrontProperties();
        cfProps.setDomain(TEST_CLOUDFRONT_DOMAIN);
        cfProps.setKeyPairId(TEST_KEY_PAIR_ID);
        cfProps.setPrivateKey(testPrivateKeyPem);

        CloudFrontUrlSigner signer = new CloudFrontUrlSigner(cfProps);
        signer.init();

        S3StorageService service = new S3StorageService(s3Client, presigner, storageProps,
                cfProps, Optional.of(signer));

        String result = service.presignedDownloadUrl(
                "workspace-1/case-1/doc-1/contrat.pdf", 15);

        assertThat(result).startsWith("https://" + TEST_CLOUDFRONT_DOMAIN + "/")
                .contains("workspace-1/case-1/doc-1/contrat.pdf");
    }

    @Test
    void presignedDownloadUrl_withCloudFrontDomain_signedUrlContainsExpectedParams() {
        CloudFrontProperties cfProps = new CloudFrontProperties();
        cfProps.setDomain(TEST_CLOUDFRONT_DOMAIN);
        cfProps.setKeyPairId(TEST_KEY_PAIR_ID);
        cfProps.setPrivateKey(testPrivateKeyPem);

        CloudFrontUrlSigner signer = new CloudFrontUrlSigner(cfProps);
        signer.init();

        S3StorageService service = new S3StorageService(s3Client, presigner, storageProps,
                cfProps, Optional.of(signer));

        String result = service.presignedDownloadUrl("some-key.pdf", 15);

        assertThat(result)
                .contains("Expires=")
                .contains("Signature=")
                .contains("Key-Pair-Id=" + TEST_KEY_PAIR_ID);
    }

    @Test
    void cloudFrontUrlSigner_missingKeyPairId_failsAtInit() {
        CloudFrontProperties cfProps = new CloudFrontProperties();
        cfProps.setDomain(TEST_CLOUDFRONT_DOMAIN);
        // keyPairId absent
        cfProps.setPrivateKey(testPrivateKeyPem);

        CloudFrontUrlSigner signer = new CloudFrontUrlSigner(cfProps);

        try {
            signer.init();
            org.junit.jupiter.api.Assertions.fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("CLOUDFRONT_KEY_PAIR_ID");
        }
    }

    @Test
    void cloudFrontUrlSigner_missingPrivateKey_failsAtInit() {
        CloudFrontProperties cfProps = new CloudFrontProperties();
        cfProps.setDomain(TEST_CLOUDFRONT_DOMAIN);
        cfProps.setKeyPairId(TEST_KEY_PAIR_ID);
        // privateKey absent

        CloudFrontUrlSigner signer = new CloudFrontUrlSigner(cfProps);

        try {
            signer.init();
            org.junit.jupiter.api.Assertions.fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("CLOUDFRONT_PRIVATE_KEY");
        }
    }
}
