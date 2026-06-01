package fr.ailegalcase.casefile;

/**
 * SF-218-35 : item de la checklist de validité d'un règlement intérieur
 * (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param item libellé de l'obligation / interdiction / formalité vérifiée.
 * @param conforme true si l'item est satisfait. Pour un item de type
 *        {@link ReglementInterieurChecklistType#INTERDIT}, {@code conforme=true}
 *        signifie l'<b>absence</b> de la clause interdite.
 * @param type nature de l'item (OBLIGATOIRE / INTERDIT / PROCEDURE).
 * @param commentaire fondement / point de vigilance attaché à l'item.
 */
public record ReglementInterieurValiditeChecklistItem(
        String item,
        boolean conforme,
        ReglementInterieurChecklistType type,
        String commentaire
) {}
