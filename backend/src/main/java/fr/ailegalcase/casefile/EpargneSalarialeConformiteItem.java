package fr.ailegalcase.casefile;

/**
 * SF-218-41 : item de la checklist de conformité d'épargne salariale (F-DT-53).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param item libellé de l'obligation / du point de contrôle.
 * @param conforme true si l'obligation est remplie (ou non applicable).
 * @param type {@code OBLIGATION} (obligation légale applicable) ou
 *        {@code INFORMATION} (point d'information / dispositif facultatif).
 * @param commentaire précision / fondement.
 */
public record EpargneSalarialeConformiteItem(
        String item,
        boolean conforme,
        String type,
        String commentaire
) {}
