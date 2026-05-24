package fr.ailegalcase.casefile;

/**
 * SF-212-23 : données d'entrée du calculateur d'analyse d'une discrimination
 * salariale fondée sur le sexe (F-DT-56-egalite-salariale-femmes-hommes,
 * FRANCE — fondements : L. 1142-7 à L. 1142-10 CT — principe d'égalité de
 * rémunération et obligations employeur ; L. 1144-1 CT — charge de la preuve
 * aménagée ; loi 05/09/2018 « avenir professionnel » instituant l'index
 * égalité ; L. 1132-1 CT — interdiction des discriminations ; L. 3221-2 CT —
 * principe à travail égal salaire égal ; L. 1134-5 CT — prescription 5 ans).
 *
 * @param sexeSalarie                        sexe du salarié concerné par la
 *                                           comparaison (FEMME ou HOMME — le
 *                                           cadre L. 1142-7 cible
 *                                           historiquement le différentiel
 *                                           femmes/hommes mais L. 3221-2 et
 *                                           L. 1132-1 s'appliquent à tout
 *                                           salarié dans la même situation)
 * @param salaireMensuelBrutSalarieEuros     salaire mensuel brut du salarié
 *                                           en euros — référence pour le
 *                                           calcul de l'écart
 * @param ancienneteMois                     ancienneté du salarié dans
 *                                           l'entreprise exprimée en mois,
 *                                           pertinente pour identifier les
 *                                           comparants à situation comparable
 * @param qualification                      qualification ou poste du salarié
 *                                           — chaîne libre (intitulé du poste
 *                                           ou coefficient conventionnel)
 * @param nombreComparantsMieuxPayes         nombre de salariés de sexe
 *                                           opposé identifiés comme mieux
 *                                           rémunérés à qualification et
 *                                           ancienneté approximativement
 *                                           équivalentes
 * @param ecartSalaireMoyenComparantsEuros   écart moyen en euros entre le
 *                                           salaire du salarié et la moyenne
 *                                           des comparants identifiés (valeur
 *                                           positive = comparants mieux payés)
 * @param ecartPourcentage                   écart en pourcentage entre le
 *                                           salaire du salarié et la moyenne
 *                                           des comparants (≥ 20 % = présomption
 *                                           forte ; 5–20 % = écart à expliquer)
 * @param indexEgaliteConnu                  l'index égalité L. 1142-8 CT est
 *                                           connu (entreprises ≥ 50 salariés
 *                                           obligation de publication
 *                                           annuelle — loi 05/09/2018)
 * @param scoreIndexEgalite                  score sur 100 de l'index égalité,
 *                                           si connu (≥ 75 = conforme ;
 *                                           &lt; 75 = mesures correctives
 *                                           exigées sous 3 ans)
 * @param justificationsEmployeurObjectives  l'employeur produit des éléments
 *                                           objectifs étrangers à toute
 *                                           discrimination (compétences,
 *                                           expérience valorisée, performance
 *                                           mesurée) — bascule la charge de
 *                                           la preuve aménagée (L. 1144-1)
 */
public record EgaliteSalarialeFhInput(
        SexeSalarie sexeSalarie,
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

    /** Sexe du salarié concerné — Femme ou Homme. */
    public enum SexeSalarie {
        FEMME,
        HOMME
    }
}
