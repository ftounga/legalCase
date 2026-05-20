package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-207-05 : projection complète (snapshot) renvoyée au client pour l'outil
 * d'éligibilité au référé tribunal du travail BE.
 *
 * <p>Inclut les inputs (pour pré-fill UI) et les outputs calculés
 * (cf. {@link RefereTribunalTravailBeResult}).</p>
 */
public record RefereTribunalTravailBeResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        RefereTribunalTravailBeMotifUrgence motifUrgence,
        String motifUrgenceDescription,
        LocalDate dateFaitGenerateur,
        LocalDate dateDemarcheAmiable,
        boolean preuveUrgenceJointe,
        String mesureProvisoireDemandee,
        boolean perilEnDemeure,
        boolean competenceTerritorialeIdentifiee,
        // Outputs calculés
        RefereTribunalTravailBeResult.Verdict verdict,
        List<RefereTribunalTravailBeCondition> conditionsNonRemplies,
        int scoreConditions,
        String requeteSquelette,
        String baseJuridique,
        RefereTribunalTravailBeResult.EtapeSuivante etapeSuivante,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
