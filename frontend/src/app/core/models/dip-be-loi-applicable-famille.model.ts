/**
 * SF-223-07 : modèles TypeScript de l'outil décisionnel "Loi applicable en droit
 * de la famille (Belgique — DIP)" (`dip-be-loi-applicable-famille`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-07 (endpoints
 * POST/GET). Détermine la loi applicable au divorce (Rome III), au régime
 * matrimonial (Règl. UE 2016/1103) ou à la succession (Règl. UE 650/2012),
 * complétés par le CDIP belge (loi du 16/07/2004 — à vérifier par avocat belge).
 * 1 outil = 1 situation (détermination de la loi applicable). DISTINCT de la
 * reconnaissance / exequatur d'une décision étrangère
 * (`dip-be-reconnaissance-decision-etrangere`, SF-223-08). BE-only pur.
 */

/** Matière de DIP familial couverte. */
export type Matiere = 'DIVORCE' | 'REGIME_MATRIMONIAL' | 'SUCCESSION';

/** Fondement de rattachement retenu (traçabilité du raisonnement). */
export type FondementRattachement =
  | 'CHOIX_LOI_PARTIES'
  | 'RESIDENCE_HABITUELLE_COMMUNE'
  | 'NATIONALITE_COMMUNE'
  | 'RESIDENCE_HABITUELLE_DEFUNT'
  | 'LOI_DU_FOR'
  | 'LIEN_LE_PLUS_ETROIT'
  | 'AUCUN';

/** Verdict de l'analyse. */
export type DipBeLoiApplicableFamilleVerdict =
  | 'LOI_APPLICABLE_DETERMINEE'
  | 'LOI_INDETERMINABLE';

/**
 * Requête POST
 * `/api/v1/case-files/{caseFileId}/dip-be-loi-applicable-famille-analysis`.
 *
 * `matiere` est requise (validation côté backend → 400). Les champs de
 * rattachement sont des codes pays ISO 3166-1 alpha-2 nullables (validés
 * `^[A-Z]{2}$` côté backend → 400). `dateMariageOuDeces` est facultative.
 */
export interface DipBeLoiApplicableFamilleRequest {
  matiere: Matiere;
  residenceHabituelleCommune: string | null;
  nationaliteCommune: string | null;
  choixLoiParLesParties: string | null;
  dateMariageOuDeces: string | null;
  lieuSituationBiensImmobiliers: string | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, loi applicable, fondement de
 * rattachement, motifs, conseils, bases juridiques, messages).
 */
export interface DipBeLoiApplicableFamilleResponse extends DipBeLoiApplicableFamilleRequest {
  caseFileId: string;
  verdict: DipBeLoiApplicableFamilleVerdict;
  loiApplicableDeterminee: string | null;
  fondementRattachement: FondementRattachement;
  motifs: string[];
  conseils: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
