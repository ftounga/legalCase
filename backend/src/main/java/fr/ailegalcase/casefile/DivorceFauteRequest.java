package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DivorceFauteRequest(
        List<String> fautesInvoquees,
        Boolean preuvesDocumentaires,
        Boolean tortsAdverseInvoques,
        Integer dureeMariageAnnees,
        BigDecimal revenusAnnuelsDemandeurEur,
        BigDecimal revenusAnnuelsDefendeurEur,
        LocalDate dateDepotAssignation
) {}
