package fr.ailegalcase.casefile;

/**
 * SF-218-19 : qualification du salarié au regard du statut de cadre dirigeant
 * (art. L.3111-2 CT — 3 critères cumulatifs + exigence jurisprudentielle d'une
 * participation effective à la direction de l'entreprise). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CADRE_DIRIGEANT : les 3 critères légaux cumulatifs sont remplis ET la
 *       participation effective à la direction de l'entreprise est établie —
 *       exclusion des dispositions sur la durée du travail confirmée.</li>
 *   <li>CADRE_DIRIGEANT_FRAGILE : les 3 critères légaux sont remplis mais la
 *       participation effective à la direction n'est pas établie — risque de
 *       requalification (la jurisprudence post-2012 exige une participation
 *       effective à la direction de l'entreprise).</li>
 *   <li>NON_CADRE_DIRIGEANT : moins de 3 critères légaux remplis — le salarié
 *       reste soumis aux règles de durée du travail (rappel d'heures
 *       supplémentaires possible).</li>
 * </ul>
 */
public enum CadreDirigeantQualification {
    CADRE_DIRIGEANT,
    CADRE_DIRIGEANT_FRAGILE,
    NON_CADRE_DIRIGEANT
}
