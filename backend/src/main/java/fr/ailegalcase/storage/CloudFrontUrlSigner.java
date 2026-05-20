package fr.ailegalcase.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * SF-INFRA-08 : signe les URLs CloudFront via la canned policy AWS
 * (SHA1withRSA + base64 URL-safe spécifique CloudFront).
 *
 * <p>Bean instancié uniquement si {@code legalcase.cloudfront.domain} est
 * défini ({@code @ConditionalOnProperty}). Si la propriété domain est
 * présente mais que key-pair-id / private-key manquent, le bean échoue au
 * démarrage (fail-fast).
 *
 * <p>Format de la canned policy AWS CloudFront :
 * <pre>
 * https://{domain}/{key}?Expires={epoch}&Signature={sig}&Key-Pair-Id={kid}
 * </pre>
 *
 * <p>Algorithme : la canned policy est un JSON figé
 * {@code {"Statement":[{"Resource":"<URL>","Condition":{"DateLessThan":{"AWS:EpochTime":<epoch>}}}]}}
 * signé en SHA1withRSA, encodé avec l'alphabet base64 spécifique CloudFront
 * ({@code + / =} → {@code - _ ~}).
 */
@Component
@ConditionalOnProperty(prefix = "legalcase.cloudfront", name = "domain")
public class CloudFrontUrlSigner {

    private final CloudFrontProperties props;
    private PrivateKey privateKey;

    public CloudFrontUrlSigner(CloudFrontProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (props.getKeyPairId() == null || props.getKeyPairId().isBlank()) {
            throw new IllegalStateException(
                    "CLOUDFRONT_DOMAIN est défini mais CLOUDFRONT_KEY_PAIR_ID est absent. "
                            + "La signature CloudFront est obligatoire en mode CDN — cf. SF-INFRA-08.");
        }
        if (props.getPrivateKey() == null || props.getPrivateKey().isBlank()) {
            throw new IllegalStateException(
                    "CLOUDFRONT_DOMAIN est défini mais CLOUDFRONT_PRIVATE_KEY est absent. "
                            + "La signature CloudFront est obligatoire en mode CDN — cf. SF-INFRA-08.");
        }
        try {
            this.privateKey = parsePrivateKey(props.getPrivateKey());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "CLOUDFRONT_PRIVATE_KEY invalide (PKCS#8 PEM ou base64 attendu).", e);
        }
    }

    /**
     * Génère une URL CloudFront signée avec canned policy.
     *
     * @param storageKey       clé S3 (= chemin sous le domaine CloudFront)
     * @param expirationSeconds durée de validité (depuis maintenant)
     * @return URL HTTPS signée
     */
    public String signedUrl(String storageKey, long expirationSeconds) {
        String normalizedKey = storageKey.startsWith("/") ? storageKey.substring(1) : storageKey;
        String resource = "https://" + props.getDomain() + "/" + normalizedKey;
        long epoch = Instant.now().getEpochSecond() + expirationSeconds;

        String policy = "{\"Statement\":[{\"Resource\":\"" + resource
                + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + epoch + "}}}]}";

        byte[] signatureBytes;
        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(privateKey);
            signer.update(policy.getBytes(StandardCharsets.UTF_8));
            signatureBytes = signer.sign();
        } catch (Exception e) {
            throw new IllegalStateException("Échec de signature CloudFront", e);
        }

        String signatureEncoded = cloudFrontBase64(signatureBytes);

        return resource
                + "?Expires=" + epoch
                + "&Signature=" + signatureEncoded
                + "&Key-Pair-Id=" + props.getKeyPairId();
    }

    /**
     * AWS CloudFront utilise un alphabet base64 spécifique :
     * {@code +} → {@code -}, {@code /} → {@code _}, {@code =} → {@code ~}.
     */
    private static String cloudFrontBase64(byte[] bytes) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return b64.replace('+', '-').replace('/', '_').replace('=', '~');
    }

    /**
     * Accepte une clé privée RSA au format PEM PKCS#8 (avec
     * {@code -----BEGIN PRIVATE KEY-----}) ou en base64 brut.
     */
    private static PrivateKey parsePrivateKey(String pemOrBase64) throws Exception {
        String stripped = pemOrBase64
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(stripped);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
