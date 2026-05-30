package fr.ailegalcase.casefile;

/**
 * SF-218-19 : évaluation d'un des 3 critères cumulatifs de l'art. L.3111-2 CT
 * caractérisant le cadre dirigeant. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param critere libellé du critère légal évalué.
 * @param rempli true si le critère est rempli au vu des éléments fournis.
 * @param commentaire explication synthétique du verdict du critère.
 */
public record CadreDirigeantCritere(
        String critere,
        boolean rempli,
        String commentaire
) {}
