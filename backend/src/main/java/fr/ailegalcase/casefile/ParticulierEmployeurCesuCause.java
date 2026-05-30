package fr.ailegalcase.casefile;

/**
 * SF-218-13 : cause de la rupture du contrat du salarié du particulier
 * employeur. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>LICENCIEMENT_MOTIF_PERSONNEL : licenciement pour motif personnel
 *       (autre que faute grave).</li>
 *   <li>RETRAIT_ENFANT : retrait de l'enfant confié à l'assistant maternel
 *       (art. L. 423-24 CASF) — réservé à la catégorie ASSISTANT_MATERNEL ;
 *       vaut rupture et ouvre droit à l'indemnité de rupture.</li>
 *   <li>FAUTE_GRAVE : licenciement pour faute grave — prive de l'indemnité de
 *       licenciement.</li>
 *   <li>FORCE_MAJEURE : rupture pour force majeure.</li>
 *   <li>DEPART_RETRAITE : départ ou mise à la retraite.</li>
 * </ul>
 */
public enum ParticulierEmployeurCesuCause {
    LICENCIEMENT_MOTIF_PERSONNEL,
    RETRAIT_ENFANT,
    FAUTE_GRAVE,
    FORCE_MAJEURE,
    DEPART_RETRAITE
}
