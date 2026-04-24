package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-08-01 : résultat de l'analyse de divorce pour altération définitive
 * du lien conjugal (art. 237-238 Cciv, loi 23/03/2019). Outil single-country
 * FR — équivalent BE = F-FA-11 (procédure distincte, art. 229 CC belge).
 */
public record DivorceAlterationResult(
        LocalDate dateCessationVieCommune,
        boolean preuvesSeparationDocumentaires,
        boolean tentativesReconciliation,
        int dureeMariageAnnees,
        BigDecimal revenusAnnuelsEpoux1Eur,
        BigDecimal revenusAnnuelsEpoux2Eur,
        boolean patrimoineCommunSignificatif,
        LocalDate dateAssignation,
        double dureeSeparationAnnees,
        boolean delaiObjectifOk,
        boolean absencePreuveReconciliation,
        boolean conditionsReunies,
        int scoreGlobal,
        String verdictProbabilite,
        List<String> criteresNonRemplis,
        BigDecimal prestationCompensatoireFourchetteMin,
        BigDecimal prestationCompensatoireFourchetteMax,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
