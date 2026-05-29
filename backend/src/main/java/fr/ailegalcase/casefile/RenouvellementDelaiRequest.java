package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-13 : requête POST pour l'analyse du délai de dépôt du renouvellement du
 * titre de séjour (2 mois avant expiration, art. R. 433-1 CESEDA). Outil
 * single-country FR.
 *
 * @param dateExpirationTitre date d'expiration du titre de séjour (requise).
 * @param dateDepotDossier date de dépôt effectif de la demande (optionnelle).
 * @param typeTitre type de titre à titre informatif (optionnel).
 */
public record RenouvellementDelaiRequest(
        LocalDate dateExpirationTitre,
        LocalDate dateDepotDossier,
        String typeTitre
) {}
