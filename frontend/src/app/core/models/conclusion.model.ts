/**
 * F-98 / SF-98-01 — Modèle TypeScript du projet de conclusions juridiques.
 *
 * Types miroir du contrat API figé dans la mini-spec
 * `docs/features/F-98/SF-98-01-conclusions-cph-fond-fr.md` (section Technique).
 */

/**
 * Statut d'une génération de conclusions.
 *
 * `NOT_GENERATED` est une valeur synthétique de réponse (DTO) : elle n'est
 * jamais persistée en base, elle signale qu'aucune ligne `case_conclusions`
 * n'existe encore pour le dossier.
 */
export type ConclusionStatus =
  | 'NOT_GENERATED'
  | 'PENDING'
  | 'PROCESSING'
  | 'DONE'
  | 'FAILED';

/**
 * Corps de la réponse `GET /api/v1/case-files/{id}/conclusions`.
 *
 * Quand `status === 'NOT_GENERATED'`, tous les champs sauf `caseFileId` et
 * `status` valent `null`.
 */
export interface ConclusionResponse {
  id: string | null;
  caseFileId: string;
  status: ConclusionStatus;
  content: string | null;
  jurisdictionLabel: string | null;
  stageLabel: string | null;
  positionLabel: string | null;
  modelUsed: string | null;
  generatedAt: string | null;
  errorMessage: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/**
 * Corps de la réponse `POST /api/v1/case-files/{id}/conclusions/generate`
 * (`202`).
 */
export interface ConclusionGenerationResponse {
  status: ConclusionStatus;
}
