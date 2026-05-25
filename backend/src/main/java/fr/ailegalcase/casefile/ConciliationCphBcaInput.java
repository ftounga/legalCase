package fr.ailegalcase.casefile;

/**
 * SF-212-37 : données d'entrée du calculateur de conciliation au Bureau de
 * Conciliation et d'Orientation (BCO) du Conseil de Prud'hommes
 * (F-DT-84-conciliation-cph-bca, FRANCE — R. 1454-7 à R. 1454-12 CT ;
 * L. 1235-1 CT al. 3 — barème transactions BCA ; formulaire D4121-1 CPC).
 *
 * <p>L'outil prépare la phase BCO du CPH. Le BCO est la première étape de
 * la procédure prud'homale — obligatoire avant le Bureau de Jugement (BJ).
 * Deux issues possibles : conciliation (accord homologué par le BCO,
 * exécutoire) ou orientation (renvoi au BJ ou à la formation de référé).</p>
 *
 * <p><b>Barème de transactions BCA</b> (L. 1235-1 al. 3 CT) : si accord au
 * BCO, l'indemnité peut être inférieure au barème Macron mais respecte des
 * seuils minimaux fixés par décret selon ancienneté :
 * <ul>
 *   <li>&lt; 2 ans : 2 mois</li>
 *   <li>2 à 8 ans : 3 mois</li>
 *   <li>8 à 15 ans : 4 mois</li>
 *   <li>15 à 25 ans : 8 mois</li>
 *   <li>&ge; 25 ans : 14 mois</li>
 * </ul>
 *
 * @param ancienneteMois            ancienneté en mois — sert au calcul du
 *                                  palier BCA applicable
 * @param salaireMensuelBrutEuros   salaire mensuel brut de référence (€) —
 *                                  base de calcul de l'indemnité minimale BCA
 * @param montantDemandesEuros      montant total des demandes du salarié (€) —
 *                                  sert à la comparaison BCA vs Macron
 * @param opportuniteConciliation   évaluation par l'avocat de l'opportunité
 *                                  de la conciliation (FAVORABLE / DEFAVORABLE
 *                                  / INCERTAIN) — pilote le message d'opportunité
 */
public record ConciliationCphBcaInput(
        Integer ancienneteMois,
        Double salaireMensuelBrutEuros,
        Double montantDemandesEuros,
        ConciliationCphBcaOpportunite opportuniteConciliation
) {

    /** Évaluation avocat de l'opportunité de la conciliation BCO. */
    public enum ConciliationCphBcaOpportunite {
        FAVORABLE,
        DEFAVORABLE,
        INCERTAIN
    }
}
