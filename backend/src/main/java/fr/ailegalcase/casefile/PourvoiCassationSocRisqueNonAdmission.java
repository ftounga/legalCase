package fr.ailegalcase.casefile;

/**
 * SF-218-05 : niveau de risque de non-admission du pourvoi par la procédure de
 * non-admission (NPC, art. 1014 CPC — formation restreinte déclarant non admis
 * les pourvois irrecevables ou non fondés sur un moyen sérieux de cassation).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>FAIBLE : au moins un cas d'ouverture de force FORTE OU moyen sérieux
 *       identifié ;</li>
 *   <li>MODERE : pas de cas FORT mais des cas de force MOYENNE et un moyen
 *       sérieux non confirmé ;</li>
 *   <li>ELEVE : aucun cas FORT et aucun moyen sérieux identifié — risque élevé
 *       de non-admission.</li>
 * </ul>
 */
public enum PourvoiCassationSocRisqueNonAdmission {
    FAIBLE,
    MODERE,
    ELEVE
}
