package fr.ailegalcase.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SF-INFRA-08 : configuration CloudFront pour la génération d'URLs signées
 * de téléchargement de documents via le CDN au lieu de S3 direct.
 *
 * <p>Mappées depuis les variables d'environnement :
 * <ul>
 *   <li>{@code CLOUDFRONT_DOMAIN} (env) → {@code legalcase.cloudfront.domain}</li>
 *   <li>{@code CLOUDFRONT_KEY_PAIR_ID} (env) → {@code legalcase.cloudfront.key-pair-id}</li>
 *   <li>{@code CLOUDFRONT_PRIVATE_KEY} (env) → {@code legalcase.cloudfront.private-key}
 *       (PEM RSA private key, multilignes ou base64)</li>
 * </ul>
 *
 * <p>Si {@code domain} est absent : fallback URLs S3 présignées natives.
 * Si {@code domain} est présent mais {@code key-pair-id} ou {@code private-key}
 * manquent : {@link CloudFrontUrlSigner} lève {@link IllegalStateException}
 * au démarrage (fail-fast). Cf. mini-spec SF-INFRA-08 §C4.
 */
@Component
@ConfigurationProperties(prefix = "legalcase.cloudfront")
public class CloudFrontProperties {

    private String domain;
    private String keyPairId;
    private String privateKey;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getKeyPairId() {
        return keyPairId;
    }

    public void setKeyPairId(String keyPairId) {
        this.keyPairId = keyPairId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * @return true si la configuration est complète et que la génération
     * d'URLs signées CloudFront est activée.
     */
    public boolean isEnabled() {
        return domain != null && !domain.isBlank();
    }
}
