package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-163-01 : DTO de réponse de l'endpoint catalogue simulateurs.
 *
 * <p>{@code legalDomain} et {@code country} reflètent le workspace primary
 * de l'utilisateur authentifié. {@code toolIds} est la liste dédupliquée
 * et triée (priority asc, toolId lex.) des outils décisionnels disponibles
 * en mode simulateur autonome pour ce couple domaine × pays.</p>
 *
 * <p>Tous les champs peuvent être {@code null} ou vides — l'absence de
 * workspace ou de domaine renvoie une réponse 200 vide, pas une erreur.</p>
 */
public record SimulatorsCatalogResponse(String legalDomain, String country, List<String> toolIds) {
}
