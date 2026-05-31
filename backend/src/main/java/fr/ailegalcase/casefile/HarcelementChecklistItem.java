package fr.ailegalcase.casefile;

/**
 * SF-218-27 : item de la checklist de conformité de la procédure interne de
 * traitement d'un signalement de harcèlement (F-DT-59). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param item libellé de l'obligation vérifiée.
 * @param conforme true si l'obligation est satisfaite.
 * @param obligatoire true si l'item est obligatoire dans le contexte du dossier
 *        (selon l'effectif, la réception d'un signalement, etc.) ; false s'il est
 *        seulement recommandé.
 * @param commentaire fondement / point de vigilance attaché à l'item.
 */
public record HarcelementChecklistItem(
        String item,
        boolean conforme,
        boolean obligatoire,
        String commentaire
) {}
