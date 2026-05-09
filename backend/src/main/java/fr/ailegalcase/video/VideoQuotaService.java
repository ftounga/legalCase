package fr.ailegalcase.video;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * SF-231-01 : hook quota vidéo. Implémentation no-op pour permettre le merge
 * de SF-231-01 sans dépendre de SF-231-03.
 *
 * <p>SF-231-03 remplacera l'implémentation par un check réel sur un compteur
 * mensuel par workspace (cf. mini-spec F-231 / SF-231-03 — quotas vidéo).
 *
 * <p>L'API est stable : SF-231-03 implémentera la même méthode et lèvera
 * {@link fr.ailegalcase.shared.PaymentRequiredException} avec le code
 * {@code VIDEO_QUOTA_EXCEEDED} si le quota est dépassé.
 */
@Service
public class VideoQuotaService {

    /**
     * Vérifie qu'un upload vidéo supplémentaire est autorisé pour ce workspace.
     * Sera surchargé / remplacé par SF-231-03.
     *
     * @param workspaceId workspace cible
     * @param durationSeconds durée de la vidéo (pour quota durée cumulée éventuel)
     */
    public void checkVideoQuotaForUpload(UUID workspaceId, long durationSeconds) {
        // SF-231-01 : no-op. Implémentation réelle dans SF-231-03.
    }
}
