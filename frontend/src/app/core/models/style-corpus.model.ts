/**
 * F-98 / SF-98-48 — Modèle TypeScript du corpus de style du cabinet.
 *
 * Types miroir du contrat API figé dans la mini-spec
 * `docs/features/F-98/SF-98-46-ingestion-corpus-style.md` (section Contrat API).
 *
 * La `style_signature` n'est jamais exposée par l'API (usage interne
 * SF-98-47) — elle n'apparaît donc pas dans ce modèle.
 */

/**
 * Statut de traitement d'un document du corpus de style.
 *
 * `PENDING`/`PROCESSING` sont des états transitoires suivis par polling ;
 * `DONE`/`FAILED` sont terminaux.
 */
export type StyleCorpusDocumentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'DONE'
  | 'FAILED';

/**
 * Résumé d'un document du corpus de style.
 *
 * Corps de chaque élément de la réponse
 * `GET /api/v1/workspaces/{workspaceId}/style-corpus/documents`
 * et de la réponse `PATCH .../{id}`.
 */
export interface StyleCorpusDocumentSummary {
  id: string;
  originalFilename: string;
  status: StyleCorpusDocumentStatus;
  active: boolean;
  createdAt: string;
  errorMessage: string | null;
}
