package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-03 : réponse de l'analyse de l'exécution forcée d'un jugement CPH
 * (art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s. Code travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record ExecutionJugementCphResponse(
        UUID caseFileId,
        LocalDate dateJugement,
        double montantCondamnation,
        boolean executionProvisoireOrdonnee,
        ExecutionJugementCphSituationEmployeur situationEmployeur,
        LocalDate dateOuvertureProcedureCollective,
        Integer ancienneteContratMois,
        Double creancesSuperPrivilegiees,
        ExecutionJugementCphVerdict verdict,
        boolean agsEligible,
        boolean relaisAgsRecommande,
        int agsCoefficientPlafond,
        double agsPlafondEuros,
        double agsPlafondMensuelSs,
        List<ExecutionJugementCphChecklistItem> checklist,
        String country,
        String baseJuridique
) {}
