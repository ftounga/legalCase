package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DivorceAlterationResponse(
        UUID caseFileId,
        LocalDate dateCessationVieCommune,
        boolean preuvesSeparationDocumentaires,
        boolean tentativesReconciliation,
        int dureeMariageAnnees,
        BigDecimal revenusAnnuelsEpoux1Eur,
        BigDecimal revenusAnnuelsEpoux2Eur,
        boolean patrimoineCommunSignificatif,
        LocalDate dateAssignation,
        String country,
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
