package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-18-03 : résultat structuré de l'analyse de recevabilité d'une
 * contestation de paternité (FR — art. 332-335 + 311-1 + 321 + 372 Cciv).
 */
public record ContestationPaterniteResult(
        ContestationPaterniteCalculator.QualiteAagir qualiteAagir,
        LocalDate dateEtablissementFiliation,
        LocalDate dateConnaissanceVerite,
        LocalDate dateMajoriteEnfant,
        boolean possessionEtatConforme5Ans,
        boolean expertiseAdnDemandee,
        boolean motifsSerieux,
        String country,
        ContestationPaterniteCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreRecevabilite,
        int delaiPrescriptionAns,
        long delaiPrescriptionRestantMois,
        boolean expertiseAdnRecommandee,
        List<String> risquesRefus,
        List<String> documentsRequis,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
