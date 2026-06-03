package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-37 : réponse de l'analyse de monétisation de jours de RTT (loi
 * n° 2022-1157 du 16/08/2022 art. 5, F-DT-51). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record RttMonetisationResponse(
        UUID caseFileId,
        int nombreJoursRttRenonces,
        BigDecimal salaireJournalierBrut,
        double tauxApplique,
        boolean joursAcquisDansFenetre,
        BigDecimal montantBrut,
        String regimeSocialFiscal,
        RttMonetisationStatut statut,
        List<String> notes,
        String country,
        String baseJuridique
) {}
