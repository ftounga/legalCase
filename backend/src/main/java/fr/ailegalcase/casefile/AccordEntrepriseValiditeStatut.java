package fr.ailegalcase.casefile;

/**
 * SF-218-31 : verdict global de validité d'un accord d'entreprise au regard des
 * conditions de majorité (art. L.2232-12 CT, F-DT-67). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>VALIDE : conditions de majorité remplies (signature &gt; 50 % des
 *       suffrages exprimés au 1er tour) et, selon l'opération, parties habilitées
 *       (révision) ou préavis respecté (dénonciation).</li>
 *   <li>VALIDE_SOUS_RESERVE : majorité atteinte par référendum (signataires ≥ 30 %
 *       et référendum approuvé) — validité subordonnée à la régularité du
 *       référendum (art. L.2232-12).</li>
 *   <li>NON_VALIDE : conditions de majorité non remplies, ou item d'opération non
 *       satisfait (parties non habilitées en révision, préavis non respecté en
 *       dénonciation).</li>
 * </ul>
 */
public enum AccordEntrepriseValiditeStatut {
    VALIDE,
    VALIDE_SOUS_RESERVE,
    NON_VALIDE
}
