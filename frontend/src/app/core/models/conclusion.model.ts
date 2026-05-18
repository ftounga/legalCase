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
 * F-98 / SF-98-52 — Cycle de vie d'une version de conclusions.
 *
 * `DRAFT` (brouillon) à la création ; l'avocat peut faire évoluer vers
 * `VALIDATED` (validé) puis `DEPOSITED` (déposé), avec retours possibles.
 * Le passage à `VALIDATED`/`DEPOSITED` exige une génération `DONE`.
 */
export type ConclusionLifecycleStatus = 'DRAFT' | 'VALIDATED' | 'DEPOSITED';

/**
 * Corps de la réponse `GET /api/v1/case-files/{id}/conclusions`.
 *
 * Quand `status === 'NOT_GENERATED'`, tous les champs sauf `caseFileId` et
 * `status` valent `null`.
 *
 * SF-98-52 : ajout de `versionNumber` + `lifecycleStatus` (champs additifs,
 * rétrocompatibles).
 *
 * SF-98-48 : ajout de `styleApplied` (optionnel) — fourni par SF-98-47.
 * Quand `true`, la version a été générée en appliquant le corpus de style du
 * cabinet. Absent ou `false` ⇒ aucune adaptation de style (dégradation propre
 * tant que SF-98-47 n'est pas déployée).
 */
export interface ConclusionResponse {
  id: string | null;
  caseFileId: string;
  status: ConclusionStatus;
  versionNumber: number | null;
  lifecycleStatus: ConclusionLifecycleStatus | null;
  content: string | null;
  jurisdictionLabel: string | null;
  stageLabel: string | null;
  positionLabel: string | null;
  modelUsed: string | null;
  generatedAt: string | null;
  errorMessage: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  /** SF-98-48/47 — vrai si le style du cabinet a été appliqué. Optionnel. */
  styleApplied?: boolean;
}

/**
 * F-98 / SF-98-52 — Résumé d'une version de conclusions.
 *
 * Corps de chaque élément de la réponse
 * `GET /api/v1/case-files/{id}/conclusions/versions`.
 */
export interface ConclusionVersionSummary {
  id: string;
  versionNumber: number;
  lifecycleStatus: ConclusionLifecycleStatus;
  status: ConclusionStatus;
  generatedAt: string | null;
  createdAt: string | null;
}

/**
 * Corps de la réponse `POST /api/v1/case-files/{id}/conclusions/generate`
 * (`202`).
 *
 * SF-98-52 : ajout de `versionNumber` (numéro de la version créée).
 */
export interface ConclusionGenerationResponse {
  status: ConclusionStatus;
  versionNumber: number;
}
