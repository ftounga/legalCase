package fr.ailegalcase.casefile;

/**
 * SF-207-01 : type de créance pour le calcul du délai de prescription
 * d'une action de droit du travail belge.
 *
 * <p>Base juridique : Loi du 3 juillet 1978 relative aux contrats de travail
 * art. 15 (al. 1 — 1 an post-rupture ; al. 2 — 5 ans pendant le contrat /
 * arriérés de salaire) et CCT 109 art. 11 (action en motivation du licenciement
 * — 1 an post-rupture).</p>
 *
 * <p>Outil BE-only — aucun équivalent FR direct (la prescription FR L.1471-1+
 * relève d'un régime distinct).</p>
 */
public enum PrescriptionBeLitigeTravailTypeCreance {

    /** Créance ex-contrat générale — 1 an à compter de la rupture (Loi 03/07/1978 art. 15 al. 1). */
    EX_CONTRAT_GENERAL,

    /** Action en motivation du licenciement — 1 an à compter de la rupture (CCT 109 art. 11). */
    EX_CONTRAT_CCT_109,

    /** Action pendant le contrat — 5 ans à compter du fait générateur (Loi 03/07/1978 art. 15 al. 2). */
    PENDANT_CONTRAT,

    /** Arriérés de salaire — 5 ans à compter de l'échéance (Loi 03/07/1978 art. 15 al. 2 ; CC art. 2262bis). */
    ARRIERES_SALAIRE
}
