package fr.ailegalcase.casefile;

/**
 * SF-221-06 : phase de la procédure « victime de la traite des êtres humains » (BE)
 * — circulaire du 26/09/2008 (3 phases indicatives, à vérifier par avocat).
 *
 * <ul>
 *   <li>REFLEXION_45J : délai de réflexion (~45 j) avant déclaration.</li>
 *   <li>DECLARATION_FAITE : déclaration faite auprès des autorités via un centre spécialisé.</li>
 *   <li>PROCEDURE_PENALE_EN_COURS : procédure pénale en cours (utilité de la déclaration).</li>
 *   <li>AUCUNE : aucune démarche engagée — orienter vers un centre spécialisé.</li>
 * </ul>
 */
public enum VictimeTraiteBePhase {
    REFLEXION_45J,
    DECLARATION_FAITE,
    PROCEDURE_PENALE_EN_COURS,
    AUCUNE
}
