package fr.ailegalcase.casefile;

/**
 * SF-218-15 : verdict global de l'analyse du statut de journaliste professionnel
 * lors d'une rupture (art. L.7111-1 et s. CT). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>RUPTURE_ASSIMILEE_LICENCIEMENT : clause de cession ou de conscience
 *       valide — la rupture est assimilée à un licenciement et ouvre droit à
 *       l'indemnité de congédiement (art. L.7112-5).</li>
 *   <li>INDEMNITE_DUE : licenciement ouvrant droit à l'indemnité de congédiement
 *       (art. L.7112-3) sans passage par la commission arbitrale.</li>
 *   <li>COMMISSION_ARBITRALE : ancienneté supérieure à 15 ans ou faute grave /
 *       fautes répétées — l'indemnité est fixée souverainement par la commission
 *       arbitrale paritaire (art. L.7112-4).</li>
 *   <li>INDEMNITE_NON_DUE : aucune indemnité de congédiement de droit (démission,
 *       ou clause invoquée mais non valide).</li>
 * </ul>
 */
public enum JournalisteStatutVerdict {
    RUPTURE_ASSIMILEE_LICENCIEMENT,
    INDEMNITE_DUE,
    COMMISSION_ARBITRALE,
    INDEMNITE_NON_DUE
}
