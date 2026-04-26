package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-18-05 : résultat structuré de l'analyse de recevabilité d'une
 * action en recherche de paternité (FR — art. 327 + 340 + 16-11 + 321 Cciv).
 */
public record RecherchePaterniteResult(
        RecherchePaterniteCalculator.QualiteDuDemandeur qualiteDuDemandeur,
        LocalDate dateNaissanceEnfant,
        boolean presomptionPossessionEtat,
        boolean expertiseAdnDemandee,
        boolean pereDesigneRefuseADN,
        boolean motifsSerieux,
        String country,
        RecherchePaterniteCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreRecevabilite,
        int delaiPrescriptionAns,
        long delaiPrescriptionRestantMois,
        boolean expertiseAdnRecommandee,
        boolean presomptionRefusADN,
        List<String> risquesRefus,
        List<String> documentsRequis,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
