package fr.ailegalcase.casefile;

/**
 * F-283 / SF-283-03 — une phase suggérée pour le formulaire d'ajout/édition :
 * le {@code type} (enum stocké, rétro-compatible) + un {@code defaultLabel}
 * pré-rempli et éditable par l'avocat.
 */
public record CasePhaseSuggestion(CasePhaseType type, String defaultLabel) {}
