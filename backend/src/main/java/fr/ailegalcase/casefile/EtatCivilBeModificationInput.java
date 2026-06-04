package fr.ailegalcase.casefile;

/**
 * SF-223-09 : input du moteur décisionnel BE qualifiant une <b>modification de
 * l'état civil</b> en Belgique — changement de nom / de prénom (loi du
 * 18/06/2018) ou changement de sexe (loi du 25/06/2017, auto-déclaration
 * administrative — à vérifier par avocat belge).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code typeModification} est requis (validation portée par le Calculator →
 * 400). {@code personneMajeure} et {@code nationaliteBelgeOuResident} sont des
 * booleans primitifs (par défaut false → conditions non remplies). Les booleans
 * de fond propres à chaque branche sont nullables :</p>
 * <ul>
 *   <li>{@code motifLegitime} — branche NOM (motif sérieux / absence de
 *       confusion ou d'atteinte aux tiers) ;</li>
 *   <li>{@code secondeDemandePrenom} — branche PRÉNOM (la 1re demande bénéficie
 *       d'un tarif réduit / gratuité) ;</li>
 *   <li>{@code declarationSexeReiteree} — branche SEXE (délai de réflexion + 2e
 *       déclaration confirmative) ;</li>
 *   <li>{@code consentementRepresentantsSiMineur} — pertinent pour un mineur
 *       (consentement des représentants légaux requis).</li>
 * </ul>
 */
public record EtatCivilBeModificationInput(
        EtatCivilBeModificationCalculator.TypeModification typeModification,
        boolean personneMajeure,
        boolean nationaliteBelgeOuResident,
        Boolean motifLegitime,
        Boolean secondeDemandePrenom,
        Boolean declarationSexeReiteree,
        Boolean consentementRepresentantsSiMineur
) {}
