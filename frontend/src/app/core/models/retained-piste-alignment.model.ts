/**
 * F-192 SF-192-02 / SF-192-03 — Représentation frontend de l'alignement
 * entre une piste stratégique 🟢 Retenue et l'outil décisionnel cible.
 *
 * Miroir du DTO `RetainedPisteAlignmentResponse` exposé par le backend
 * (cf. SF-192-01). Si SF-192-02 introduit ce fichier en parallèle, V1 doit
 * être strictement identique à la version SF-192-02 — toute divergence est
 * une dette de convergence à résorber au merge.
 *
 * matchStatus :
 *  - ALIGNED        : l'outil cible recommande cette piste (top-1 ou présent)
 *  - DIVERGENT      : l'outil cible a tourné mais ne recommande pas cette piste
 *  - NOT_ANALYZED   : l'outil cible n'a pas encore été analysé pour ce dossier
 *  - NO_TARGET_TOOL : aucun outil cible mappé pour cette piste (Famille/Travail V1)
 */
export type RetainedPisteMatchStatus =
  | 'ALIGNED'
  | 'DIVERGENT'
  | 'NOT_ANALYZED'
  | 'NO_TARGET_TOOL';

export interface RetainedPisteAlignment {
  pisteId: string;
  texte: string;
  baseJuridique?: string | null;
  horizonTemporel?: string | null;
  conditions: string[];
  toolIdCible?: string | null;
  matchStatus: RetainedPisteMatchStatus;
}
