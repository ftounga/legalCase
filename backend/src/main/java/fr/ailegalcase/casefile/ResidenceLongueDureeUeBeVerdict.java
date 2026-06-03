package fr.ailegalcase.casefile;

/**
 * SF-221-03 : verdict de l'analyse d'éligibilité au statut de RÉSIDENT LONGUE DURÉE UE
 * (art. 15bis Loi 15/12/1980 transposant la directive 2003/109/CE).
 *
 * <ul>
 *   <li>ELIGIBLE : ≥ 5 ans (60 mois) de séjour légal ininterrompu, ressources stables
 *       et suffisantes, assurance maladie, condition d'intégration remplie et pas
 *       d'absences hors UE excessives.</li>
 *   <li>DUREE_INSUFFISANTE : moins de 60 mois de séjour légal — indiquer les mois
 *       restants avant l'ouverture du droit.</li>
 *   <li>CONDITIONS_MATERIELLES_NON_REUNIES : durée et continuité acquises mais une
 *       condition matérielle (ressources / assurance / intégration) n'est pas remplie —
 *       lister les conditions manquantes.</li>
 *   <li>CONTINUITE_ROMPUE : le séjour légal n'a pas été ininterrompu OU les absences
 *       hors UE dépassent les limites admises — la continuité requise est rompue.</li>
 *   <li>A_EXAMINER : données partielles — examen au cas par cas (verdict par défaut).</li>
 * </ul>
 */
public enum ResidenceLongueDureeUeBeVerdict {
    ELIGIBLE,
    DUREE_INSUFFISANTE,
    CONDITIONS_MATERIELLES_NON_REUNIES,
    CONTINUITE_ROMPUE,
    A_EXAMINER
}
