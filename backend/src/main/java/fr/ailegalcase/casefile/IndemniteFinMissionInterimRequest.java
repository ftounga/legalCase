package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-18-01 : requête de calcul de l'indemnité de fin de mission intérim
 * (art. L.1251-32 Code du travail).
 *
 * @param totalRemunerationsBrutesEur somme des rémunérations brutes perçues pendant la mission (>0)
 * @param dureeMissionJours           durée de la mission en jours calendaires (>0)
 * @param motifExclusion              code d'exclusion L.1251-32/33 (nullable). Valeurs autorisées :
 *                                    CONTRAT_INDETERMINEE_PROPOSE, RUPTURE_ANTICIPEE_SALARIE,
 *                                    FAUTE_GRAVE, FORCE_MAJEURE, MISSION_PEPINIERE_QUALIFIANTE,
 *                                    INTERIMAIRE_REFUS_PROPOSITION_CDI
 * @param dateFinMission              date de fin de mission (ISO date, optionnelle, traçabilité)
 */
public record IndemniteFinMissionInterimRequest(
        BigDecimal totalRemunerationsBrutesEur,
        Integer dureeMissionJours,
        String motifExclusion,
        LocalDate dateFinMission
) {}
