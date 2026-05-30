package fr.ailegalcase.casefile;

/**
 * SF-218-21 : niveau de risque de requalification du stage en contrat de travail
 * (CDI) au sens de l'art. L.124-8 du code de l'éducation (interdiction d'occuper
 * un poste de travail permanent ; missions hors projet pédagogique ; dépassement
 * de la durée maximale de 6 mois — art. L.124-5). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>FAIBLE : aucun indice de requalification — le stage respecte son objet
 *       pédagogique et les durées légales.</li>
 *   <li>MODERE : un seul indice présent — vigilance, la qualification de stage
 *       reste fragilisée.</li>
 *   <li>ELEVE : au moins deux indices présents, ou dépassement de la durée
 *       maximale de 6 mois — requalification en CDI probable.</li>
 * </ul>
 */
public enum StagiaireRisqueRequalification {
    FAIBLE,
    MODERE,
    ELEVE
}
