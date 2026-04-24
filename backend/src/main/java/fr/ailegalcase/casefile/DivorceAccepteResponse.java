package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DivorceAccepteResponse(
        UUID caseFileId,
        boolean acceptationPrincipeSignee,
        LocalDate dateAcceptationPV,
        int dureeMariageAnnees,
        BigDecimal revenusAnnuelsEpoux1Eur,
        BigDecimal revenusAnnuelsEpoux2Eur,
        boolean patrimoineCommun,
        LocalDate dateAssignation,
        String country,
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
