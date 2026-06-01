package fr.ailegalcase.casefile;

/**
 * SF-218-29 : item de la checklist de conformité de la négociation annuelle
 * obligatoire (NAO, F-DT-66). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param item libellé de l'obligation vérifiée (bloc de négociation, périodicité,
 *        PV de désaccord).
 * @param conforme true si l'obligation est satisfaite.
 * @param obligatoire true si l'item est obligatoire dans le contexte du dossier
 *        (selon la présence d'un DS, l'aboutissement de la négociation, etc.) ;
 *        false s'il est seulement recommandé / sans objet.
 * @param commentaire fondement / point de vigilance attaché à l'item.
 */
public record NaoChecklistItem(
        String item,
        boolean conforme,
        boolean obligatoire,
        String commentaire
) {}
