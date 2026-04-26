package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-18-03 : requête d'analyse de recevabilité d'une action en
 * contestation de paternité (FR — art. 332-335 Cciv).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record ContestationPaterniteRequest(
        ContestationPaterniteCalculator.QualiteAagir qualiteAagir,
        LocalDate dateEtablissementFiliation,
        LocalDate dateConnaissanceVerite,
        LocalDate dateMajoriteEnfant,
        Boolean possessionEtatConforme5Ans,
        Boolean expertiseAdnDemandee,
        Boolean motifsSerieux
) {}
