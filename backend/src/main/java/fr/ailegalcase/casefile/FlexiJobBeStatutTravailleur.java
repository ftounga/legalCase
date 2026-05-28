package fr.ailegalcase.casefile;

/**
 * SF-219-12 : statut du travailleur candidat flexi-job (Loi-programme
 * du 26/12/2013, art. 13 — conditions personnelles).
 *
 * <p>Le flexi-job est <b>strictement réservé</b> à deux catégories :</p>
 *
 * <ul>
 *   <li>{@link #PENSIONNE} — bénéficiaire d'une pension de retraite ou
 *       de survie (toutes catégories depuis l'extension 2018, sans
 *       plafond de cumul). Cumul autorisé sans restriction trimestrielle.</li>
 *   <li>{@link #SALARIE_4_5_TEMPS_T_MOINS_3} — salarié occupé chez un
 *       <b>autre</b> employeur à concurrence d'au moins <b>4/5 ETP</b>
 *       au cours du <b>3e trimestre civil précédant</b> l'occupation
 *       flexi (référence ONSS DmfA T-3). La règle T-3 = filtre
 *       anti-substitution destiné à éviter qu'un salarié à temps
 *       partiel ne soit basculé en flexi pour échapper aux cotisations.</li>
 *   <li>{@link #AUTRE} — ni l'un ni l'autre — pas de statut flexi
 *       possible (catégorie résiduelle).</li>
 * </ul>
 *
 * <p>Cas spéciaux non couverts V1 : étudiant (régime étudiant
 * spécifique distinct), allocataire de chômage (interdit), allocataire
 * d'incapacité (autorisation INAMI préalable requise).</p>
 */
public enum FlexiJobBeStatutTravailleur {
    /** Pensionné (retraite / survie) — cumul flexi autorisé sans restriction trimestrielle. */
    PENSIONNE,

    /** Salarié 4/5 ETP au T-3 chez un autre employeur — éligible flexi. */
    SALARIE_4_5_TEMPS_T_MOINS_3,

    /** Hors deux catégories ci-dessus — pas de statut flexi possible. */
    AUTRE
}
