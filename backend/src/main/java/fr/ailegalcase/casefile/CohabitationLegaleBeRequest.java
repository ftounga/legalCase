package fr.ailegalcase.casefile;

/**
 * SF-223-01 : requête HTTP pour les endpoints de la cohabitation légale BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code modeDissolutionEnvisage} est nullable sauf si
 * {@code vue = DISSOLUTION} — validation portée par le Calculator (réponse 400
 * via {@code IllegalArgumentException}).</p>
 */
public record CohabitationLegaleBeRequest(
        CohabitationLegaleBeCalculator.VueCohabitationLegaleBe vue,
        Boolean deuxPersonnesNonMariees,
        Boolean capaciteJuridique,
        Boolean pasDejaLieParMariageOuAutreCohabitation,
        Boolean domicileCommun,
        Boolean logementFamilialEnJeu,
        CohabitationLegaleBeCalculator.ModeDissolutionCohabitationLegaleBe modeDissolutionEnvisage,
        String commentaire
) {

    CohabitationLegaleBeInput toInput() {
        return new CohabitationLegaleBeInput(
                vue,
                deuxPersonnesNonMariees,
                capaciteJuridique,
                pasDejaLieParMariageOuAutreCohabitation,
                domicileCommun,
                logementFamilialEnJeu,
                modeDissolutionEnvisage,
                commentaire);
    }
}
