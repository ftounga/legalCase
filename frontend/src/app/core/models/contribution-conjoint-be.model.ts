/**
 * SF-217-09 : modèles TypeScript de l'outil décisionnel "Pension alimentaire
 * entre ex-époux (Belgique)" (Vague 2 Famille BE — CC art. 301). BELGIQUE
 * uniquement.
 *
 * Contrat API importé de SF-217-08 (backend, endpoints POST/GET figés).
 */

/** Verdict de l'analyse de la pension alimentaire entre ex-époux. */
export type ContributionConjointBeVerdict =
  | 'PENSION_DUE'
  | 'PENSION_NON_DUE'
  | 'PENSION_CONVENTIONNELLE'
  | 'DONNEES_INSUFFISANTES';

/** Type de divorce prononcé. */
export type TypeDivorceBe = 'DC' | 'DDI';

/** Code structuré d'un motif d'exclusion de la pension. */
export type MotifExclusionPensionBe =
  | 'FAUTE_GRAVE_CREANCIER'
  | 'CREANCIER_HORS_BESOIN'
  | 'RENONCIATION_CONVENTIONNELLE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/contribution-conjoint-be`.
 * Montants décimaux en euros. Commentaire nullable (max 1000).
 */
export interface ContributionConjointBeRequest {
  typeDivorce: TypeDivorceBe;
  renonciationPensionConvention: boolean;
  creancierEnEtatDeBesoin: boolean;
  fauteGraveCreancier: boolean;
  dureeMariageAnnees: number;
  revenuMensuelCreancier: number;
  revenuMensuelDebiteur: number;
  degradationEconomiqueLieeAuMariage: boolean;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `ContributionConjointBeRequest`) pour permettre la ré-édition du formulaire
 * après rechargement, **plus** les champs calculés.
 */
export interface ContributionConjointBeResponse
  extends ContributionConjointBeRequest {
  caseFileId: string;
  verdict: ContributionConjointBeVerdict;
  dureeMaximaleMois: number;
  montantMensuelIndicatif: number;
  plafondTiersRevenusDebiteur: number;
  motifsExclusion: MotifExclusionPensionBe[];
  detailCalcul: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
