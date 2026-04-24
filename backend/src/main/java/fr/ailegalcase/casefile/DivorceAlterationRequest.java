package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DivorceAlterationRequest(
        LocalDate dateCessationVieCommune,
        Boolean preuvesSeparationDocumentaires,
        Boolean tentativesReconciliation,
        Integer dureeMariageAnnees,
        BigDecimal revenusAnnuelsEpoux1Eur,
        BigDecimal revenusAnnuelsEpoux2Eur,
        Boolean patrimoineCommunSignificatif,
        LocalDate dateAssignation
) {}
