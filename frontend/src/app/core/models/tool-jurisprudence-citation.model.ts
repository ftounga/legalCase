/**
 * F-JU-01 / SF-JU-01-04 — citation jurisprudentielle exposée par le backend
 * pour une branche de calcul d'un outil décisionnel.
 *
 * Préfixe `Tool*` pour éviter toute confusion avec les modèles de F-242
 * (citations workspace-scoped per-point juridique).
 */
export interface ToolJurisprudenceCitation {
  id: string;
  arretRef: string;
  juridiction: string;
  dateArret: string; // ISO date YYYY-MM-DD
  numeroPourvoi: string;
  lienLegifrance: string;
  chapeauOfficiel: string;
  lastVerifiedAt: string; // ISO instant
  confidenceScore: number; // 0.00 à 1.00
}

/** Payload optionnel envoyé au POST signal. */
export interface ToolJurisprudenceSignalRequest {
  comment?: string;
}
