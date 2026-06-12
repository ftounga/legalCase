package fr.ailegalcase.casefile;

import java.util.List;

/**
 * F-284 — réponse de l'échéancier proactif d'un dossier.
 *
 * @param items    échéances triées par date croissante
 * @param nextItem l'échéance la plus urgente (premier élément) ou null si aucune
 * @param counts   décompte par niveau d'urgence
 */
public record EcheancierResponse(
        List<EcheancierItem> items,
        EcheancierItem nextItem,
        Counts counts
) {
    public record Counts(long overdue, long critical, long soon, long upcoming, long total) {}
}
