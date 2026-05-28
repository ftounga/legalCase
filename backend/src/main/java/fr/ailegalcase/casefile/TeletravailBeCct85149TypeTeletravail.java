package fr.ailegalcase.casefile;

/**
 * SF-219-16 : type de télétravail invoqué par les parties — détermine
 * la convention applicable (CCT n° 85 du 09/11/2005 du CNT pour le
 * structurel ; CCT n° 149 du 26/01/2021 du CNT pour l'occasionnel).
 *
 * <p>La qualification du type repose sur la régularité de la formule
 * (art. 2 CCT n° 85) : structurel = exécuté régulièrement, à raison
 * d'au moins une partie du temps de travail dans le cadre du contrat
 * de travail ; occasionnel = ponctuel (force majeure, raison
 * personnelle, COVID).</p>
 *
 * <h2>Régimes</h2>
 * <ul>
 *   <li>{@link #STRUCTUREL_CCT_85} — télétravail régulier, à raison
 *       d'au moins une partie du temps de travail, encadré par la CCT
 *       n° 85 du 09/11/2005 (CNT). Exige une <b>convention écrite
 *       individuelle</b> par travailleur (art. 6 CCT n° 85), reprenant
 *       fréquence, jours, lieu, équipement fourni, indemnité.</li>
 *   <li>{@link #OCCASIONNEL_CCT_149} — télétravail occasionnel régi
 *       par la CCT n° 149 (force majeure, raison personnelle, COVID).
 *       Pas de convention écrite obligatoire au cas par cas — accord
 *       cadre suffit. Indemnité forfaitaire frais possible mais non
 *       structurelle.</li>
 *   <li>{@link #INDETERMINE} — qualification non encore opérée ou
 *       litige sur la convention applicable — analyse au cas par cas.</li>
 * </ul>
 */
public enum TeletravailBeCct85149TypeTeletravail {
    /** Télétravail régulier structurel — CCT n° 85 du 09/11/2005. */
    STRUCTUREL_CCT_85,

    /** Télétravail occasionnel / force majeure / COVID — CCT n° 149. */
    OCCASIONNEL_CCT_149,

    /** Qualification indéterminée — analyse cas par cas requise. */
    INDETERMINE
}
