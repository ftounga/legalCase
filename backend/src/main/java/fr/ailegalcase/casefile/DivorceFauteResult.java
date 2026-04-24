package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-09-01 : résultat de l'analyse divorce pour faute FR (art. 242-246, 266, 270
 * Cciv). Outil single-country FR — pas d'équivalent BE direct dans cette SF.
 */
public record DivorceFauteResult(
        List<String> fautesInvoquees,
        boolean preuvesDocumentaires,
        boolean tortsAdverseInvoques,
        int dureeMariageAnnees,
        BigDecimal revenusAnnuelsDemandeurEur,
        BigDecimal revenusAnnuelsDefendeurEur,
        LocalDate dateDepotAssignation,
        int nombreFautesInvoquees,
        boolean solidariteeFautesOk,
        boolean risqueTortsPartages,
        int scoreGlobal,
        String verdictProbabiliteDivorceFaute,
        String verdictTortsEstimes,
        BigDecimal damagesInteretsArt266FourchetteMin,
        BigDecimal damagesInteretsArt266FourchetteMax,
        BigDecimal prestationCompensatoireFourchetteMin,
        BigDecimal prestationCompensatoireFourchetteMax,
        List<String> criteresNonRemplis,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
