package fr.ailegalcase.analysis;

import java.util.UUID;

/**
 * Message RabbitMQ déclenchant la synthèse globale d'un dossier.
 *
 * <p>Déclenchée uniquement sur action manuelle de l'avocat (clic
 * "Analyser le dossier"). L'auto-trigger post-DocumentAnalysis introduit par
 * SF-185-03 a été supprimé par SF-185-04 (analyse de dossier mono-doc trop
 * pauvre conceptuellement, coût Anthropic injustifié).
 */
public record CaseAnalysisMessage(UUID caseFileId) {
}
