package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-207-08 : projection complète (snapshot) renvoyée au client pour
 * l'outil outplacement obligatoire 45+ BE.
 *
 * <p>Inclut les inputs (pour pré-fill UI) et les outputs calculés
 * (cf. {@link OutplacementBeResult}).</p>
 */
public record OutplacementBeResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        LocalDate dateLicenciement,
        LocalDate dateNaissanceSalarie,
        BigDecimal ancienneteAnnees,
        OutplacementBeMotifLicenciement motifLicenciement,
        boolean contratTempsPlein,
        boolean offreOutplacementRecue,
        LocalDate dateOffreOutplacement,
        Boolean offreConformeCCT82,
        Boolean salarieAcceptantOffre,
        // Outputs calculés
        OutplacementBeResult.Verdict verdict,
        int ageALaDateLicenciement,
        boolean obligationEmployeurApplicable,
        OutplacementBeRaisonNonObligation raisonNonObligation,
        BigDecimal sanctionEmployeurEuros,
        OutplacementBeResult.SanctionSalarieRange sanctionSalarieRange,
        LocalDate dateLimiteOffre,
        Boolean delaiOffreRespecte,
        OutplacementBeResult.EtapeSuivante etapeSuivante,
        String baseJuridique,
        String formuleCalcul,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
