package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-25-01 : réponse de l'endpoint indemnité compensatrice de préavis FR.
 * Schéma figé pour SF-DT-25-02 frontend.
 */
public record IndemnitePreavisResponse(
        UUID caseFileId,
        int ancienneteAnnees,
        int ancienneteMois,
        BigDecimal salaireMensuelBrutEur,
        String conventionCollectiveCode,
        IndemnitePreavisFonction fonction,
        boolean exemptionEmployeur,
        LocalDate dateRupture,
        int dureePreavisMois,
        IndemnitePreavisSourceDuree sourceDuree,
        BigDecimal montantIndemniteEur,
        boolean exemptionRetenue,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
