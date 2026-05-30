package fr.ailegalcase.casefile;

/**
 * SF-218-13 : éligibilité à l'indemnité de licenciement / de rupture du salarié
 * du particulier employeur. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DUE : l'indemnité est due (ancienneté ≥ seuil conventionnel et cause de
 *       rupture ouvrant droit à indemnité).</li>
 *   <li>NON_DUE : l'indemnité n'est pas due (faute grave ou ancienneté
 *       inférieure au seuil conventionnel) — voir le motif associé.</li>
 * </ul>
 */
public enum ParticulierEmployeurCesuEligibilite {
    DUE,
    NON_DUE
}
