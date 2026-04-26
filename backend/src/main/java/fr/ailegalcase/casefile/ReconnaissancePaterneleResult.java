package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-18-01 : résultat structuré de l'analyse de recevabilité d'une
 * reconnaissance paternelle (FR — art. 316 + 332-335 + 372 Cciv).
 */
public record ReconnaissancePaterneleResult(
        ReconnaissancePaterneleCalculator.SousType sousType,
        LocalDate dateNaissanceEnfant,
        LocalDate dateReconnaissance,
        boolean consentementLibreDuPere,
        boolean paterniteVraisemblable,
        boolean enfantNonReconnuParAutrePere,
        boolean procedureRespectee,
        boolean presenceParProcuration,
        String country,
        ReconnaissancePaterneleCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreEligibilite,
        LocalDate effetFiliation,
        List<String> risquesContestation,
        List<String> documentsRequis,
        int delaiContestationAns,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
