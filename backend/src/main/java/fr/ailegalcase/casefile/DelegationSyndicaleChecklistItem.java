package fr.ailegalcase.casefile;

/**
 * SF-218-33 : item de la checklist de régularité de la désignation d'un délégué
 * syndical / RSS (F-DT-69). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param item libellé de la condition de désignation vérifiée (effectif
 *        suffisant, organisation représentative, score personnel).
 * @param conforme true si la condition est satisfaite.
 * @param commentaire fondement / point de vigilance attaché à l'item.
 */
public record DelegationSyndicaleChecklistItem(
        String item,
        boolean conforme,
        String commentaire
) {}
