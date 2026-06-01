package fr.ailegalcase.casefile;

/**
 * SF-218-33 : niveau de risque de nullité d'un licenciement du DS / RSS
 * (art. L.2411-3 CT, F-DT-69). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ELEVE : un licenciement est envisagé sans autorisation préalable de
 *       l'inspecteur du travail — nullité du licenciement et droit à
 *       réintégration (art. L.2411-3).</li>
 *   <li>FAIBLE : un licenciement est envisagé mais l'autorisation de
 *       l'inspecteur du travail a été obtenue.</li>
 *   <li>SANS_OBJET : aucun licenciement n'est envisagé.</li>
 * </ul>
 */
public enum DelegationSyndicaleRisqueNullite {
    ELEVE,
    FAIBLE,
    SANS_OBJET
}
