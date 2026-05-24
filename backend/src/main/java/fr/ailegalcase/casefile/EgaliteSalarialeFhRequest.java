package fr.ailegalcase.casefile;

/**
 * SF-212-23 : requête HTTP pour l'endpoint d'analyse d'une discrimination
 * salariale fondée sur le sexe (F-DT-56-egalite-salariale-femmes-hommes,
 * FRANCE — L. 1142-7 à L. 1142-10 CT ; L. 1144-1 CT ; L. 3221-2 CT ;
 * L. 1132-1 CT ; loi 05/09/2018).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record EgaliteSalarialeFhRequest(
        EgaliteSalarialeFhInput.SexeSalarie sexeSalarie,
        double salaireMensuelBrutSalarieEuros,
        int ancienneteMois,
        String qualification,
        int nombreComparantsMieuxPayes,
        double ecartSalaireMoyenComparantsEuros,
        double ecartPourcentage,
        boolean indexEgaliteConnu,
        Integer scoreIndexEgalite,
        boolean justificationsEmployeurObjectives
) {

    EgaliteSalarialeFhInput toInput() {
        return new EgaliteSalarialeFhInput(
                sexeSalarie,
                salaireMensuelBrutSalarieEuros,
                ancienneteMois,
                qualification,
                nombreComparantsMieuxPayes,
                ecartSalaireMoyenComparantsEuros,
                ecartPourcentage,
                indexEgaliteConnu,
                scoreIndexEgalite,
                justificationsEmployeurObjectives
        );
    }
}
