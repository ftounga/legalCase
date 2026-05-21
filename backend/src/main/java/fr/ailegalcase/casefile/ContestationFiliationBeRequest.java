package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-18 : requête HTTP pour les endpoints d'analyse de recevabilité d'une
 * contestation de filiation BE.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.
 * Tous les champs sont obligatoires sauf {@code commentaire}.</p>
 */
public record ContestationFiliationBeRequest(
        ContestationFiliationBeCalculator.NatureActionFiliationBe natureActionFiliation,
        ContestationFiliationBeCalculator.QualiteDemandeurFiliationBe qualiteDemandeur,
        LocalDate dateNaissanceEnfant,
        LocalDate dateConnaissanceFaitContestation,
        Boolean possessionEtatConforme,
        Integer dureePossessionEtatAnnees,
        Boolean expertiseAdnDisponible,
        Boolean demandeExpertiseAdnEnvisagee,
        String commentaire
) {

    ContestationFiliationBeInput toInput() {
        return new ContestationFiliationBeInput(
                natureActionFiliation,
                qualiteDemandeur,
                dateNaissanceEnfant,
                dateConnaissanceFaitContestation,
                Boolean.TRUE.equals(possessionEtatConforme),
                dureePossessionEtatAnnees == null ? 0 : dureePossessionEtatAnnees,
                Boolean.TRUE.equals(expertiseAdnDisponible),
                Boolean.TRUE.equals(demandeExpertiseAdnEnvisagee),
                commentaire
        );
    }
}
