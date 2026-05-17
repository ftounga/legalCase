package fr.ailegalcase.consent.dto;

import java.util.List;

/**
 * F-240 SF-240-01 : corps de requête de {@code POST /api/v1/consent/accept}.
 *
 * <p>La validation (types autorisés, version non vide ≤ 64 chars) est faite
 * explicitement par {@code ConsentAcceptanceService} — pas de Bean Validation
 * sur ce record, afin de garder un message d'erreur métier explicite.</p>
 *
 * @param consentTypes types de documents acceptés (1 à 10 entrées)
 * @param version      version du document acceptée (chaîne libre ≤ 64 chars)
 */
public record ConsentAcceptanceRequest(
        List<String> consentTypes,
        String version
) {
}
