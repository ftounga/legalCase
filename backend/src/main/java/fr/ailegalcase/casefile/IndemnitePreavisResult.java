package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-25-01 : résultat structuré du calcul d'indemnité compensatrice de préavis FR
 * (art. L.1234-1 Code du travail + CCN). Persisté en JSON dans
 * {@code indemnite_preavis_analyses.result_data}.
 */
public record IndemnitePreavisResult(
        int ancienneteMois,
        IndemnitePreavisFonction fonction,
        String conventionCollectiveCode,
        BigDecimal salaireMensuelBrutEur,
        boolean exemptionEmployeur,
        boolean exemptionRetenue,
        LocalDate dateRupture,
        int dureePreavisMois,
        IndemnitePreavisSourceDuree sourceDuree,
        BigDecimal montantIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
