package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-10-01 : résultat de l'analyse "divorce accepté" / acceptation du
 * principe de la rupture (art. 233-234 Cciv + art. 1123 CPC).
 * Outil single-country FR + DROIT_FAMILLE.
 */
public record DivorceAccepteResult(
        boolean acceptationPrincipeSignee,
        LocalDate dateAcceptationPV,
        int dureeMariageAnnees,
        BigDecimal revenusAnnuelsEpoux1Eur,
        BigDecimal revenusAnnuelsEpoux2Eur,
        boolean patrimoineCommun,
        LocalDate dateAssignation,
        boolean acceptationValide,
        boolean ordrePublic,
        boolean eligibilite,
        int scoreGlobal,
        String verdictEligibilite,
        int delaiProcedureMoisPrevisionnel,
        BigDecimal prestationCompensatoireFourchetteMin,
        BigDecimal prestationCompensatoireFourchetteMax,
        List<String> criteresNonRemplis,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
