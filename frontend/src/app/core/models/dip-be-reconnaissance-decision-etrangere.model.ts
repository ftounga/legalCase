/**
 * SF-223-08 : modèles TypeScript de l'outil décisionnel "Reconnaissance /
 * exequatur d'une décision familiale étrangère (Belgique — DIP)"
 * (`dip-be-reconnaissance-decision-etrangere`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-08 (endpoints
 * POST/GET). Qualifie la reconnaissance / l'exequatur d'une décision familiale
 * étrangère hors UE (CDIP art. 22-27) OU le mariage religieux non précédé d'un
 * mariage civil (art. 21 Const. / CC art. 161 — à vérifier par avocat belge).
 * 1 outil = 1 situation (reconnaissance / exequatur d'une décision déjà rendue +
 * mariage religieux non-civil). DISTINCT de la détermination de la loi
 * applicable (`dip-be-loi-applicable-famille`, SF-223-07) ET de la
 * reconnaissance d'un mariage / divorce valablement célébré à l'étranger
 * (`mariage-etranger-be-reconnaissance`, F-217). BE-only pur.
 */

/** Nature de la décision étrangère soumise à reconnaissance. */
export type NatureDecision =
  | 'JUGEMENT_ETRANGER_HORS_UE'
  | 'MARIAGE_RELIGIEUX_NON_CIVIL';

/** Verdict de l'analyse (4 niveaux). */
export type DipBeReconnaissanceVerdict =
  | 'RECONNAISSANCE_DE_PLEIN_DROIT'
  | 'EXEQUATUR_REQUIS'
  | 'RECONNAISSANCE_REFUSEE'
  | 'QUALIFICATION_INCOMPLETE';

/**
 * Requête POST
 * `/api/v1/case-files/{caseFileId}/dip-be-reconnaissance-decision-etrangere-analysis`.
 *
 * `natureDecision` est requise (validation côté backend → 400). `paysOrigine`
 * est un code pays ISO 3166-1 alpha-2 nullable (validé `^[A-Z]{2}$` côté backend
 * → 400). Les booleans de fond sont nullables ; `conformiteOrdrePublicBelge` est
 * un boolean (par défaut conforme côté UI). `mariageCivilPrealable` est propre
 * au cas du mariage religieux.
 */
export interface DipBeReconnaissanceDecisionEtrangereRequest {
  natureDecision: NatureDecision;
  paysOrigine: string | null;
  dateDecision: string | null;
  decisionDefinitive: boolean | null;
  droitsDefenseRespectes: boolean | null;
  conformiteOrdrePublicBelge: boolean;
  absenceFraude: boolean | null;
  mariageCivilPrealable: boolean | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, motifs, conseils, actes à
 * produire, bases juridiques, messages).
 */
export interface DipBeReconnaissanceDecisionEtrangereResponse
  extends DipBeReconnaissanceDecisionEtrangereRequest {
  caseFileId: string;
  verdict: DipBeReconnaissanceVerdict;
  motifs: string[];
  conseils: string[];
  actesAProduire: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
