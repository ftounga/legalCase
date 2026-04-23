package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-IA-04-01 : réponse de l'endpoint de résolution de visibilité des outils
 * décisionnels pour un dossier.
 *
 * Trois couches disjointes :
 * <ul>
 *     <li>{@code alwaysOn} — outils structurellement affichés pour ce domaine,
 *         sans dépendance à une détection IA</li>
 *     <li>{@code contextual} — outils activés par la présence d'une situation
 *         détectée sur le dossier (type_rupture, type_titre_sejour_code, etc.)</li>
 *     <li>{@code catalog} — outils contextuels du domaine+pays qui ne sont pas
 *         activés actuellement, exposés pour un éventuel ajout manuel</li>
 * </ul>
 *
 * Les identifiants sont des strings stables documentés dans la mini-spec
 * SF-IA-04-01 (annexe). Chaque outil n'apparaît qu'au plus une fois, et jamais
 * à la fois dans {@code contextual} et {@code catalog}.
 */
public record VisibleToolSetResponse(
        List<String> alwaysOn,
        List<String> contextual,
        List<String> catalog
) {
}
