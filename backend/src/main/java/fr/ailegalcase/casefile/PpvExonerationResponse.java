package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-39 : réponse de l'analyse d'exonération de la prime de partage de la
 * valeur (PPV — loi n° 2022-1158 du 16/08/2022 art. 1 + loi n° 2023-1107 du
 * 29/11/2023, F-DT-52). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record PpvExonerationResponse(
        UUID caseFileId,
        BigDecimal montantPrime,
        boolean accordInteressementPresent,
        BigDecimal remunerationAnnuelleBrute,
        boolean effectifMoins50,
        boolean versementPlanEpargne,
        BigDecimal plafondSocialApplique,
        BigDecimal montantExonere,
        BigDecimal montantImposable,
        boolean exonerationFiscaleIr,
        PpvExonerationStatut statut,
        List<String> notes,
        String country,
        String baseJuridique
) {}
