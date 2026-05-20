package fr.ailegalcase.casefile;

/**
 * SF-207-05 : les 5 conditions cumulatives évaluées par le
 * {@link RefereTribunalTravailBeCalculator} pour qualifier l'éligibilité au
 * référé devant le président du tribunal du travail belge (CJ art. 584).
 *
 * <p>Le {@code scoreConditions} (0-5) est le nombre de conditions remplies ;
 * le verdict est :
 * <ul>
 *   <li>{@code REFERE_ELIGIBLE} si score = 5 (toutes conditions),</li>
 *   <li>{@code REFERE_INCERTAIN} si 3 ≤ score ≤ 4,</li>
 *   <li>{@code REFERE_NON_ELIGIBLE} si score ≤ 2.</li>
 * </ul></p>
 *
 * <p>Les conditions non remplies sont listées explicitement dans
 * {@link RefereTribunalTravailBeResult#conditionsNonRemplies()} pour orienter
 * la décision de l'avocat (renforcer le dossier vs basculer sur la procédure
 * de fond).</p>
 */
public enum RefereTribunalTravailBeCondition {

    /**
     * Urgence qualifiable — motif explicite ({@code != AUTRE}) ou, si AUTRE,
     * description circonstanciée (> 30 caractères) caractérisant l'urgence.
     */
    URGENCE_QUALIFIABLE,

    /** Preuve d'urgence jointe au dossier (pièce factuelle). */
    PEUVE_URGENCE_JOINTE,

    /** Péril en demeure caractérisé (préjudice imminent / irréversible). */
    PERIL_EN_DEMEURE,

    /**
     * Mesure provisoire précise demandée (libellé > 10 caractères — un référé
     * sans mesure précise est de facto irrecevable, le juge ne pouvant
     * statuer sur une demande imprécise).
     */
    MESURE_PROVISOIRE_PRECISE,

    /**
     * Compétence territoriale identifiée — juridiction du domicile du salarié
     * (CJ art. 627 al. 9° pour les contestations en matière de contrat de
     * travail).
     */
    COMPETENCE_IDENTIFIEE
}
