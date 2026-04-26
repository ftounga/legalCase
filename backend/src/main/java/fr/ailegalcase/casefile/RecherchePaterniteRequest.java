package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-18-05 : requête d'analyse de recevabilité d'une action en
 * recherche de paternité (FR — art. 327 + 340 + 16-11 + 321 Cciv).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record RecherchePaterniteRequest(
        RecherchePaterniteCalculator.QualiteDuDemandeur qualiteDuDemandeur,
        LocalDate dateNaissanceEnfant,
        Boolean presomptionPossessionEtat,
        Boolean expertiseAdnDemandee,
        Boolean pereDesigneRefuseADN,
        Boolean motifsSerieux
) {}
