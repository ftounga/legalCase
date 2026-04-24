package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DivorceFauteResponse(
        UUID caseFileId,
        List<String> fautesInvoquees,
        boolean preuvesDocumentaires,
        boolean tortsAdverseInvoques,
        int dureeMariageAnnees,
        BigDecimal revenusAnnuelsDemandeurEur,
        BigDecimal revenusAnnuelsDefendeurEur,
        LocalDate dateDepotAssignation,
        String country,
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
