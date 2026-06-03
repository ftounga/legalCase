package fr.ailegalcase.casefile;

/**
 * SF-223-01 : input du moteur décisionnel BE de la cohabitation légale.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code modeDissolutionEnvisage} est nullable sauf si
 * {@code vue = DISSOLUTION} (validation portée par le Calculator).
 * {@code logementFamilialEnJeu} est nullable (pertinent surtout pour la
 * vue EFFETS).</p>
 */
public record CohabitationLegaleBeInput(
        CohabitationLegaleBeCalculator.VueCohabitationLegaleBe vue,
        Boolean deuxPersonnesNonMariees,
        Boolean capaciteJuridique,
        Boolean pasDejaLieParMariageOuAutreCohabitation,
        Boolean domicileCommun,
        Boolean logementFamilialEnJeu,
        CohabitationLegaleBeCalculator.ModeDissolutionCohabitationLegaleBe modeDissolutionEnvisage,
        String commentaire
) {}
