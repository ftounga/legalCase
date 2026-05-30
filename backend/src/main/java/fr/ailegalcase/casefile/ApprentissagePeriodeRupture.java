package fr.ailegalcase.casefile;

/**
 * SF-218-23 : période de la rupture du contrat d'apprentissage par rapport au
 * seuil légal des 45 premiers jours de formation pratique en entreprise
 * (art. L.6222-18 CT, F-DT-110). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DANS_45_PREMIERS_JOURS : la rupture intervient au plus tard au 45e jour
 *       — rupture libre par l'une ou l'autre partie, sans motif (forme écrite
 *       requise).</li>
 *   <li>APRES_45_JOURS : la rupture intervient au-delà du 45e jour — motifs
 *       limités (accord écrit, faute grave, force majeure, inaptitude,
 *       exclusion définitive du CFA).</li>
 * </ul>
 */
public enum ApprentissagePeriodeRupture {
    DANS_45_PREMIERS_JOURS,
    APRES_45_JOURS
}
