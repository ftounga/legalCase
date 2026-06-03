/**
 * SF-223-03 : modèles TypeScript de l'outil décisionnel "Recueil légal
 * (kafala) — Belgique" (`kafala-be-recueil-legal`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-03 (endpoints
 * POST/GET — CDIP, loi du 16/07/2004 ; CC art. 343 al. 2 nouveau — à vérifier
 * par avocat belge, renumérotation CC post-réformes 2017-2019). La kafala
 * n'est PAS une adoption (aucun lien de filiation) : distinct de `adoption-be`.
 * BE-only pur, aucun équivalent FR.
 */

/** Objectif que l'avocat envisage pour la kafala en Belgique. */
export type ObjectifEnvisage =
  | 'RECUEIL_PROTECTION'
  | 'TENTATIVE_ADOPTION'
  | 'SEJOUR'
  | 'SUCCESSION';

/** Verdict de l'analyse (4 niveaux). */
export type KafalaBeVerdict =
  | 'RECUEIL_RECONNU_COMME_PROTECTION'
  | 'RECONNAISSANCE_SOUS_CONDITIONS'
  | 'EFFET_ADOPTIF_EXCLU'
  | 'QUALIFICATION_INCOMPLETE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/kafala-be-recueil-legal-analysis`.
 *
 * `objectifEnvisage` est requis (validation côté backend → 400).
 * `paysOrigineKafala` (ISO 3166-1 alpha-2) et `dateActeKafala` (ISO YYYY-MM-DD)
 * sont nullables (informatifs / pré-remplissables).
 */
export interface KafalaBeRequest {
  paysOrigineKafala: string | null;
  dateActeKafala: string | null;
  acteOfficielJudiciaireOuAdoul: boolean | null;
  enfantAbandonneOuOrphelin: boolean | null;
  kafilDomicilieBelgique: boolean | null;
  consentementParentsBiologiquesOuAutorite: boolean | null;
  objectifEnvisage: ObjectifEnvisage;
  commentaire: string | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, motifs/conditions, actes à
 * produire, bases juridiques, messages).
 */
export interface KafalaBeResponse extends KafalaBeRequest {
  caseFileId: string;
  verdict: KafalaBeVerdict;
  motifsConditions: string[];
  actesAProduire: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
