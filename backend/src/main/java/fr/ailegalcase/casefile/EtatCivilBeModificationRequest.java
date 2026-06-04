package fr.ailegalcase.casefile;

/**
 * SF-223-09 : requête HTTP pour les endpoints de modification de l'état civil BE
 * (changement de nom / prénom — loi 18/06/2018 ; changement de sexe — loi
 * 25/06/2017).
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code typeModification} est requis (validation portée par le Calculator
 * → 400 via {@code IllegalArgumentException}). {@code personneMajeure} et
 * {@code nationaliteBelgeOuResident} sont des booleans primitifs (absent → false
 * interprété comme « condition non remplie » ; l'UI envoie toujours la valeur
 * saisie).</p>
 */
public record EtatCivilBeModificationRequest(
        EtatCivilBeModificationCalculator.TypeModification typeModification,
        boolean personneMajeure,
        boolean nationaliteBelgeOuResident,
        Boolean motifLegitime,
        Boolean secondeDemandePrenom,
        Boolean declarationSexeReiteree,
        Boolean consentementRepresentantsSiMineur
) {

    EtatCivilBeModificationInput toInput() {
        return new EtatCivilBeModificationInput(
                typeModification,
                personneMajeure,
                nationaliteBelgeOuResident,
                motifLegitime,
                secondeDemandePrenom,
                declarationSexeReiteree,
                consentementRepresentantsSiMineur);
    }
}
