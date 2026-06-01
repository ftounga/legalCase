package fr.ailegalcase.casefile;

/**
 * SF-218-31 : qualification de la condition de majorité d'un accord d'entreprise
 * (art. L.2232-12 CT, F-DT-67). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>MAJORITE_50 : signature par des syndicats représentatifs ayant recueilli
 *       plus de 50 % des suffrages exprimés au 1er tour des dernières élections —
 *       accord valide sans référendum.</li>
 *   <li>REFERENDUM_30 : signataires entre 30 % (inclus) et 50 % (exclu) et accord
 *       validé par référendum des salariés à la majorité des suffrages exprimés —
 *       accord valide sous réserve de la régularité du référendum.</li>
 *   <li>INSUFFISANTE : signataires &lt; 30 %, ou entre 30 % et 50 % sans référendum
 *       approuvé — accord non valide en l'état.</li>
 * </ul>
 */
public enum AccordConditionMajorite {
    MAJORITE_50,
    REFERENDUM_30,
    INSUFFISANTE
}
