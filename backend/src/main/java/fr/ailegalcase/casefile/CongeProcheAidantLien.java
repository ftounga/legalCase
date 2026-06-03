package fr.ailegalcase.casefile;

/**
 * SF-218-47 : lien entre le salarié et la personne aidée ouvrant droit au congé
 * de proche aidant (art. L.3142-16 CT, F-DT-79). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONJOINT : conjoint, concubin ou partenaire lié par un PACS.</li>
 *   <li>ASCENDANT : ascendant (parent, grand-parent…).</li>
 *   <li>DESCENDANT : descendant (enfant, petit-enfant…).</li>
 *   <li>COLLATERAL : collatéral jusqu'au 4e degré (frère/sœur, oncle/tante,
 *       cousin…), y compris collatéraux du conjoint.</li>
 *   <li>SANS_LIEN_RESIDENCE_COMMUNE : personne avec laquelle le salarié réside ou
 *       entretient des liens étroits et stables, à qui il vient en aide de
 *       manière régulière et fréquente à titre non professionnel.</li>
 * </ul>
 */
public enum CongeProcheAidantLien {
    CONJOINT,
    ASCENDANT,
    DESCENDANT,
    COLLATERAL,
    SANS_LIEN_RESIDENCE_COMMUNE
}
