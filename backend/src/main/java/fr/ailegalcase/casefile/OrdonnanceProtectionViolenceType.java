package fr.ailegalcase.casefile;

/**
 * SF-FA-14-01 : catégories de violences alléguées dans une demande d'ordonnance
 * de protection (art. 515-9 Cciv).
 *
 * <p>Les MENACES_MORT sont isolées des PHYSIQUES car elles relèvent d'une
 * qualification pénale distincte (art. 222-17 CP) et leur poids est traité
 * comme une catégorie de violence à part entière dans le scoring.
 */
public enum OrdonnanceProtectionViolenceType {
    PHYSIQUES,
    PSYCHOLOGIQUES,
    SEXUELLES,
    ECONOMIQUES,
    MENACES_MORT
}
