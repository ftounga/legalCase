package fr.ailegalcase.casefile.conclusion;

import java.util.UUID;

/**
 * F-98 / SF-98-01 — message RabbitMQ déclenchant la génération asynchrone d'un
 * projet de conclusions. Déclenché uniquement sur action manuelle de l'avocat
 * (clic « Générer le projet de conclusions » / « Régénérer »).
 *
 * @param caseConclusionId identifiant de la ligne {@code case_conclusions} à traiter
 */
public record CaseConclusionMessage(UUID caseConclusionId) {
}
