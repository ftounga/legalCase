/**
 * F-179 SF-179-03 — vérification d'une référence jurisprudentielle citée dans
 * un document uploadé du dossier (modèle miroir du backend `JurisprudenceCheck`).
 */

/** Statut d'une vérification de jurisprudence. */
export type JurisprudenceCheckStatut =
  | 'VERIFIED'
  | 'SUSPECT'
  | 'NOT_FOUND'
  | 'UNCERTAIN';

/** Une vérification de référence jurisprudentielle. */
export interface JurisprudenceCheck {
  id: string;
  /** Nom du document où la référence a été détectée. */
  documentName: string;
  /** Libellé canonique de la référence (ex. "Cass. soc. n° 12-17.516"). */
  reference: string;
  /** VERIFIED / SUSPECT / NOT_FOUND / UNCERTAIN. */
  statut: JurisprudenceCheckStatut;
  /** Explication courte du verdict (réalité de l'arrêt). */
  explication: string | null;
  /** Position juridique que le document prête à l'arrêt. */
  positionAlleguee: string | null;
  /** Lien source (Légifrance / Juridat), si disponible. */
  sourceUrl: string | null;
  /** Confiance Claude : HIGH / MEDIUM / LOW. */
  claudeConfidence: string | null;
  /** true si le fallback web search a été tenté. */
  webSearchUsed: boolean;
}

/** Réponse de `GET /api/v1/case-files/{id}/jurisprudence-checks`. */
export interface JurisprudenceCheckResponse {
  checks: JurisprudenceCheck[];
}
