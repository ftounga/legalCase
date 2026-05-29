package fr.ailegalcase.casefile;

/**
 * SF-214-07 : statut du délai de validation du VLS-TS (visa long séjour valant
 * titre de séjour) auprès de l'OFII — 3 mois à compter de l'entrée en France
 * (art. R. 311-3 CESEDA).
 *
 * <ul>
 *   <li>A_VALIDER : le délai court, joursRestants &gt; 15 — marge confortable.</li>
 *   <li>URGENT : joursRestants ∈ [1, 15] — fenêtre courte, action prioritaire.</li>
 *   <li>EXPIRE : joursRestants ≤ 0 et validation non effectuée — délai dépassé,
 *       risque d'irrégularité du séjour.</li>
 *   <li>VALIDE : la validation OFII a été effectuée (prioritaire sur les autres états).</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 */
public enum VlsTsValidationStatut {
    A_VALIDER,
    URGENT,
    EXPIRE,
    VALIDE
}
