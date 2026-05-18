package fr.ailegalcase.casefile;

/**
 * SF-217-04 : input du calcul de l'arbre décisionnel de l'autorité parentale
 * belge (CC art. 374-375).
 *
 * <p>Tous les booléens sont obligatoires (validés non-null par le Calculator) ;
 * le commentaire est nullable. Le pays est dérivé du workspace côté service —
 * pas transmis ici.</p>
 */
public record AutoriteParentaleBeInput(
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
}
