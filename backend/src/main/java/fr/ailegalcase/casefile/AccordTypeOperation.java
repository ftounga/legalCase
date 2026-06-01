package fr.ailegalcase.casefile;

/**
 * SF-218-31 : type d'opération portant sur un accord d'entreprise (F-DT-67).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONCLUSION : conclusion d'un accord d'entreprise (validité appréciée aux
 *       seules conditions de majorité, art. L.2232-12).</li>
 *   <li>REVISION : avenant de révision (ajoute la vérification des parties
 *       habilitées à engager la procédure, art. L.2261-7 et s.).</li>
 *   <li>DENONCIATION : dénonciation d'un accord (préavis de 3 mois, survie de
 *       l'accord pendant 12 mois, art. L.2261-9 à L.2261-11).</li>
 * </ul>
 */
public enum AccordTypeOperation {
    CONCLUSION,
    REVISION,
    DENONCIATION
}
