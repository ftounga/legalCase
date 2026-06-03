package fr.ailegalcase.casefile;

/**
 * SF-218-53 : item de la checklist de conformité du droit à la déconnexion
 * (F-DT-83). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param item libellé de l'obligation / du point de contrôle.
 * @param conforme true si l'obligation est remplie (ou non applicable).
 * @param type {@code OBLIGATION} (obligation légale applicable),
 *        {@code PROCEDURE} (modalité / formalité procédurale) ou
 *        {@code INFORMATION} (point d'information).
 * @param commentaire précision / fondement.
 */
public record DroitDeconnexionConformiteItem(
        String item,
        boolean conforme,
        String type,
        String commentaire
) {}
