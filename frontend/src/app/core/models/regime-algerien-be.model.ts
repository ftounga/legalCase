/**
 * SF-223-05 : modèles TypeScript de l'outil décisionnel "Régime algérien —
 * reconnaissance mariage / talaq / dot (Belgique)" (`regime-algerien-be`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-05 (endpoints
 * POST/GET). Corridor algérien (CDIP, loi du 16/07/2004 ; Convention
 * algéro-belge ; Code de la famille algérien — à vérifier par avocat belge).
 * Cadre la SPÉCIFICITÉ ALGÉRIENNE (dot/mahr, conditions du Code algérien,
 * Convention bilatérale) — DISTINCT de l'outil GÉNÉRAL
 * `mariage-etranger-be-reconnaissance` (F-217). BE-only pur.
 */

/** Nature de l'acte algérien soumis à reconnaissance. */
export type NatureActe = 'MARIAGE_ALGERIEN' | 'TALAQ_ALGERIEN' | 'DOT_MAHR';

/** Lien de rattachement de la situation à la Belgique. */
export type LienRattachement = 'RESIDENCE' | 'NATIONALITE' | 'AUCUN';

/** Verdict de l'analyse (4 niveaux). */
export type RegimeAlgerienBeVerdict =
  | 'RECONNAISSANCE_DE_PLEIN_DROIT'
  | 'RECONNAISSANCE_SOUS_CONDITIONS'
  | 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC'
  | 'QUALIFICATION_INCOMPLETE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/regime-algerien-be-analysis`.
 *
 * `natureActe` et `lienRattachementBelgique` sont requis (validation côté
 * backend → 400). Les autres champs sont nullables (informatifs /
 * pré-remplissables).
 */
export interface RegimeAlgerienBeRequest {
  natureActe: NatureActe;
  dateActe: string | null;
  consentementEpouxEpouse: boolean | null;
  dotMahrPrevue: boolean | null;
  montantDotConnu: number | null;
  conventionAlgeroBelgeInvoquee: boolean | null;
  lienRattachementBelgique: LienRattachement;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, motifs, effets de la dot, bases
 * juridiques, messages).
 */
export interface RegimeAlgerienBeResponse extends RegimeAlgerienBeRequest {
  caseFileId: string;
  verdict: RegimeAlgerienBeVerdict;
  motifs: string[];
  effetsDot: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
