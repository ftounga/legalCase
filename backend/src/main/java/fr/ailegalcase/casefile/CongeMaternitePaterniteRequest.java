package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-29 : requête HTTP de l'endpoint d'analyse du congé maternité /
 * paternité (F-DT-77-conge-paternite-maternite, FRANCE — L. 1225-1 à
 * L. 1225-40 CT ; L. 331-3 CSS ; loi du 16/03/2021).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le
 * body.</p>
 */
public record CongeMaternitePaterniteRequest(
        CongeMaternitePaterniteInput.TypeConge typeConge,
        int rangEnfant,
        boolean naissanceMultiple,
        double salaireMensuelBrutEuros,
        LocalDate dateDebutConge,
        boolean licenciementPendantConge,
        boolean retourPosteDifferent
) {

    CongeMaternitePaterniteInput toInput() {
        return new CongeMaternitePaterniteInput(
                typeConge,
                rangEnfant,
                naissanceMultiple,
                salaireMensuelBrutEuros,
                dateDebutConge,
                licenciementPendantConge,
                retourPosteDifferent
        );
    }
}
