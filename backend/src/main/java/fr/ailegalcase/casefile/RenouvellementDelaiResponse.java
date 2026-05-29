package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-13 : réponse de l'analyse du délai de dépôt du renouvellement du titre
 * de séjour (2 mois avant expiration, art. R. 433-1 CESEDA). Outil single-country FR.
 */
public record RenouvellementDelaiResponse(
        UUID caseFileId,
        LocalDate dateExpirationTitre,
        LocalDate dateDepotDossier,
        String typeTitre,
        LocalDate dateOptimalDepot,
        LocalDate dateDepotImperatif,
        Long joursRestantsAvantOptimal,
        Long joursRestantsAvantImperatif,
        RenouvellementDelaiStatut statut,
        boolean risqueIrruption,
        boolean alerteRetard,
        String recommandation,
        String country,
        String baseJuridique
) {}
