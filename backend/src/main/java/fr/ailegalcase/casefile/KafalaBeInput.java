package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-03 : input du moteur décisionnel BE de qualification du recueil légal
 * (kafala).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code paysOrigineKafala} (ISO 3166-1 alpha-2) et {@code dateActeKafala} sont
 * nullables (informatifs / pré-remplissables). {@code enfantAbandonneOuOrphelin}
 * est nullable. {@code objectifEnvisage} est requis.</p>
 */
public record KafalaBeInput(
        String paysOrigineKafala,
        LocalDate dateActeKafala,
        Boolean acteOfficielJudiciaireOuAdoul,
        Boolean enfantAbandonneOuOrphelin,
        Boolean kafilDomicilieBelgique,
        Boolean consentementParentsBiologiquesOuAutorite,
        KafalaBeCalculator.ObjectifEnvisage objectifEnvisage,
        String commentaire
) {}
