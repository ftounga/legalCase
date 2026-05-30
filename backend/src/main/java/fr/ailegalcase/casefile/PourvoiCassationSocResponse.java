package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-05 : réponse de l'analyse d'un pourvoi en cassation devant la chambre
 * sociale (art. 612 CPC ; art. 604 CPC ; art. 973 CPC ; art. 1014 CPC). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record PourvoiCassationSocResponse(
        UUID caseFileId,
        LocalDate dateNotificationArret,
        LocalDate dateLimitePourvoi,
        long joursRestants,
        PourvoiCassationSocVerdictDelai verdictDelai,
        List<PourvoiCassationSocCasOuvertureAnalyse> casOuvertureAnalyses,
        PourvoiCassationSocRisqueNonAdmission risqueNonAdmission,
        boolean representationAvocatCassation,
        boolean moyenSerieuxIdentifie,
        PourvoiCassationSocVerdict verdict,
        List<PourvoiCassationSocChecklistItem> checklist,
        String country,
        String baseJuridique
) {}
