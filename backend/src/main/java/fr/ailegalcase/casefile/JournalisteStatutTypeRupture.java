package fr.ailegalcase.casefile;

/**
 * SF-218-15 : type de rupture invoqué pour le journaliste professionnel
 * (art. L.7111-1 et s. CT). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>LICENCIEMENT : rupture à l'initiative de l'employeur ouvrant droit à
 *       l'indemnité de congédiement (art. L.7112-3).</li>
 *   <li>CLAUSE_CESSION : rupture à l'initiative du journaliste fondée sur la
 *       cession ou la cessation de publication du titre (art. L.7112-5 1°) —
 *       assimilée à un licenciement.</li>
 *   <li>CLAUSE_CONSCIENCE : rupture à l'initiative du journaliste fondée sur un
 *       changement notable du caractère ou de l'orientation du journal portant
 *       atteinte à son honneur, sa réputation ou ses intérêts moraux
 *       (art. L.7112-5 2°/3°) — assimilée à un licenciement.</li>
 *   <li>DEMISSION : rupture à l'initiative du journaliste sans clause —
 *       n'ouvre pas droit à l'indemnité de congédiement.</li>
 *   <li>FAUTE_GRAVE : faute grave ou fautes répétées — renvoi à la commission
 *       arbitrale paritaire (art. L.7112-4), pas d'indemnité de congédiement de
 *       droit.</li>
 * </ul>
 */
public enum JournalisteStatutTypeRupture {
    LICENCIEMENT,
    CLAUSE_CESSION,
    CLAUSE_CONSCIENCE,
    DEMISSION,
    FAUTE_GRAVE
}
