package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-03 : requête HTTP pour les endpoints du recueil légal (kafala) BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code objectifEnvisage} est requis (validation portée par le
 * Calculator → 400 via {@code IllegalArgumentException}).</p>
 */
public record KafalaBeRequest(
        String paysOrigineKafala,
        LocalDate dateActeKafala,
        Boolean acteOfficielJudiciaireOuAdoul,
        Boolean enfantAbandonneOuOrphelin,
        Boolean kafilDomicilieBelgique,
        Boolean consentementParentsBiologiquesOuAutorite,
        KafalaBeCalculator.ObjectifEnvisage objectifEnvisage,
        String commentaire
) {

    KafalaBeInput toInput() {
        return new KafalaBeInput(
                paysOrigineKafala,
                dateActeKafala,
                acteOfficielJudiciaireOuAdoul,
                enfantAbandonneOuOrphelin,
                kafilDomicilieBelgique,
                consentementParentsBiologiquesOuAutorite,
                objectifEnvisage,
                commentaire);
    }
}
