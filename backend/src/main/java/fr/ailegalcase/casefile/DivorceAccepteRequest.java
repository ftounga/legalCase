package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DivorceAccepteRequest(
        Boolean acceptationPrincipeSignee,
        LocalDate dateAcceptationPV,
        Integer dureeMariageAnnees,
        BigDecimal revenusAnnuelsEpoux1Eur,
        BigDecimal revenusAnnuelsEpoux2Eur,
        Boolean patrimoineCommun,
        LocalDate dateAssignation
) {}
