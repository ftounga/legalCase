package fr.ailegalcase.casefile;

/**
 * SF-221-02 : verdict de l'analyse de passage carte A → carte B (séjour ILLIMITÉ
 * d'un ressortissant tiers, art. 14 Loi 15/12/1980).
 *
 * <ul>
 *   <li>ELIGIBLE : ≥ 5 ans (60 mois) de séjour régulier ininterrompu, sans absence
 *       excédant les limites, motif de séjour stable et aucun risque d'ordre public.</li>
 *   <li>DUREE_INSUFFISANTE : moins de 60 mois de séjour régulier — indiquer les mois
 *       restants avant l'ouverture du droit.</li>
 *   <li>CONTINUITE_ROMPUE : le séjour n'a pas été ininterrompu OU les absences
 *       dépassent les limites admises — la continuité requise est rompue.</li>
 *   <li>RISQUE_ORDRE_PUBLIC : un risque d'ordre public est signalé — refus ou examen
 *       renforcé probable.</li>
 *   <li>A_EXAMINER : motif de séjour instable ou données partielles — examen au cas
 *       par cas (verdict par défaut).</li>
 * </ul>
 */
public enum CarteBSejourIllimiteBeVerdict {
    ELIGIBLE,
    DUREE_INSUFFISANTE,
    CONTINUITE_ROMPUE,
    RISQUE_ORDRE_PUBLIC,
    A_EXAMINER
}
