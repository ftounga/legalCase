package fr.ailegalcase.casefile;

/**
 * SF-218-23 : motif invoqué pour la rupture du contrat d'apprentissage
 * (art. L.6222-18 et s. CT, F-DT-110). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ACCORD_PARTIES : rupture par accord écrit signé des deux parties.</li>
 *   <li>FAUTE_GRAVE : faute grave de l'apprenti (résiliation par voie de
 *       licenciement depuis la réforme 2018).</li>
 *   <li>FORCE_MAJEURE : événement de force majeure rendant l'exécution
 *       impossible.</li>
 *   <li>INAPTITUDE : inaptitude médicale constatée par le médecin du travail
 *       (art. L.6222-18-1, employeur dispensé de reclassement).</li>
 *   <li>EXCLUSION_DEFINITIVE_CFA : exclusion définitive de l'apprenti du CFA
 *       (art. L.6222-21, motif de rupture par l'employeur).</li>
 *   <li>SANS_MOTIF : aucun motif invoqué (rupture libre uniquement durant les
 *       45 premiers jours ; irrégulière au-delà).</li>
 * </ul>
 */
public enum ApprentissageMotifRupture {
    ACCORD_PARTIES,
    FAUTE_GRAVE,
    FORCE_MAJEURE,
    INAPTITUDE,
    EXCLUSION_DEFINITIVE_CFA,
    SANS_MOTIF
}
