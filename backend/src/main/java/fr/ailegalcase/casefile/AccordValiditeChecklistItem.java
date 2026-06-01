package fr.ailegalcase.casefile;

/**
 * SF-218-31 : item de la checklist de validité d'un accord d'entreprise
 * (conditions de majorité, référendum, parties habilitées, préavis de
 * dénonciation — art. L.2232-12 et L.2261-7 et s. CT, F-DT-67). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param item libellé de la condition vérifiée.
 * @param conforme true si la condition est satisfaite.
 * @param commentaire fondement / point de vigilance attaché à l'item.
 */
public record AccordValiditeChecklistItem(
        String item,
        boolean conforme,
        String commentaire
) {}
