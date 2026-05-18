package fr.ailegalcase.casefile;

/**
 * SF-217-04 : requête HTTP pour l'endpoint d'analyse de l'autorité parentale
 * belge.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record AutoriteParentaleBeRequest(
        Boolean filiationEtablieDeuxParents,
        Boolean accordParentalExiste,
        Boolean demandeAutoriteExclusive,
        Boolean desinteretDurableParent,
        Boolean miseEnDangerEnfant,
        Boolean incapaciteParent,
        Boolean decisionJudiciaireAnterieure,
        AutoriteParentaleBeCalculator.ModeHebergement modeHebergementPrincipal,
        String commentaire
) {

    AutoriteParentaleBeInput toInput() {
        return new AutoriteParentaleBeInput(
                filiationEtablieDeuxParents,
                accordParentalExiste,
                demandeAutoriteExclusive,
                desinteretDurableParent,
                miseEnDangerEnfant,
                incapaciteParent,
                decisionJudiciaireAnterieure,
                modeHebergementPrincipal,
                commentaire
        );
    }
}
