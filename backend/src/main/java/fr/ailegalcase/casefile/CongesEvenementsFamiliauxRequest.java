package fr.ailegalcase.casefile;

/**
 * SF-218-43 : requête POST pour l'analyse du congé pour évènement familial
 * (art. L.3142-1 à L.3142-5 CT, F-DT-76). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param typeEvenement nature de l'évènement familial (requis).
 * @param conventionPlusFavorable true si la convention collective prévoit une
 *        durée plus favorable que la loi (requis).
 * @param dureeConventionnelleJours durée prévue par la CCN en jours (optionnel /
 *        nullable ; requise si {@code conventionPlusFavorable = true},
 *        strictement positive).
 */
public record CongesEvenementsFamiliauxRequest(
        CongesEvenementsFamiliauxTypeEvenement typeEvenement,
        Boolean conventionPlusFavorable,
        Integer dureeConventionnelleJours
) {}
