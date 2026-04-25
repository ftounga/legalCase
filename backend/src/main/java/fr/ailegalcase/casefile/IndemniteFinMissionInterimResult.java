package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-18-01 : résultat du calcul d'indemnité de fin de mission intérim
 * (art. L.1251-32 Code du travail).
 */
public record IndemniteFinMissionInterimResult(
        BigDecimal totalRemunerationsBrutesEur,
        int dureeMissionJours,
        String motifExclusion,
        LocalDate dateFinMission,
        BigDecimal tauxApplique,
        BigDecimal montantIndemniteEur,
        boolean exclusionRetenue,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
