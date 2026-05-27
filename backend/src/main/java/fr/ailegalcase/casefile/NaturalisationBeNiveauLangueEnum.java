package fr.ailegalcase.casefile;

/**
 * SF-215-07 : niveau de langue (français, néerlandais ou allemand) attesté par le
 * demandeur de la déclaration de nationalité belge art. 12bis CNB.
 *
 * <p>L'AR du 14/01/2013 fixe les modes de preuve du niveau A2 minimum requis pour
 * la déclaration 12bis :
 *  <ul>
 *    <li>Diplôme A2 DELF (français) ;</li>
 *    <li>Certificat NT2 (néerlandais) ou diplôme équivalent ;</li>
 *    <li>Diplôme d'enseignement secondaire supérieur belge (6e année) ;</li>
 *    <li>Preuve d'avoir travaillé 5 ans en Belgique avec contrat soumis à la
 *        sécurité sociale (preuve indirecte du niveau A2).</li>
 *  </ul>
 *
 * <ul>
 *   <li>INFERIEUR_A2 : niveau de langue insuffisant — bloque les deux voies 12bis.</li>
 *   <li>A2 : niveau strictement A2 (minimum requis art. 12bis §2 2°).</li>
 *   <li>SUPERIEUR_A2 : niveau supérieur (B1, B2, C1, C2) — couvre A2 a fortiori.</li>
 * </ul>
 *
 * <p>Enum partageable avec SF-215-09 (naturalisation conjoint Belge art. 16 CNB)
 * — la condition de langue A2 minimum est commune.
 */
public enum NaturalisationBeNiveauLangueEnum {
    INFERIEUR_A2,
    A2,
    SUPERIEUR_A2
}
