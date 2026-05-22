package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-02 / SF-JU-02-01 — usage déclaré d'un outil décisionnel sur un dossier
 * spécifique (un toolId × une branche de calcul active à un instant donné).
 */
public record ToolUsage(String toolId, String brancheCalculId) {
}
