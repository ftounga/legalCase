/**
 * F-217 SF-217-13 : modèles TypeScript de l'outil décisionnel
 * "Succession BE — dévolution légale et réserve héréditaire post-2017"
 * (`succession-be-devolution-reserve`).
 *
 * BELGIQUE uniquement. Contrat API importé de SF-217-11 (backend, endpoints
 * POST/GET figés — Livre 4 CC BE réformé, loi du 31/07/2017 en vigueur
 * 01/09/2018).
 */

/** Verdict d'analyse de la dévolution / réserve. */
export type SuccessionBeDevolutionReserveVerdict =
  | 'DEVOLUTION_ETABLIE'
  | 'RESERVE_RESPECTEE'
  | 'QUOTITE_DEPASSEE'
  | 'QUALIFICATION_INCOMPLETE';

/** État civil du défunt à la date du décès. */
export type EtatCivilDefuntBe =
  | 'MARIE'
  | 'COHABITANT_LEGAL'
  | 'CELIBATAIRE'
  | 'DIVORCE'
  | 'VEUF';

/** Régime matrimonial du défunt s'il était marié. */
export type RegimeMatrimonialDefuntBe =
  | 'COMMUNAUTE_LEGALE'
  | 'SEPARATION_BIENS'
  | 'COMMUNAUTE_UNIVERSELLE'
  | 'PARTICIPATION_ACQUETS'
  | 'AUTRE';

/** Code structuré d'un héritier exposé dans la réponse. */
export type HeritierCodeBe =
  | 'CONJOINT_SURVIVANT'
  | 'COHABITANT_LEGAL_SURVIVANT'
  | 'ENFANT'
  | 'DESCENDANT_REPRESENTATION'
  | 'PARENT'
  | 'FRERE_SOEUR'
  | 'DESCENDANT_FRERE_SOEUR'
  | 'ETAT';

/** Héritier identifié — exposé dans la réponse. */
export interface HeritierBe {
  code: HeritierCodeBe;
  libelle: string;
  quotePartReserveFraction: string;
  quotePartReserveEur: number | null;
  quotePartGlobaleFraction: string | null;
  quotePartGlobaleEur: number | null;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/succession-be-devolution-reserve`.
 * `dateDeces` au format ISO `YYYY-MM-DD`, non future.
 * `regimeMatrimonialDefunt` est nullable (uniquement requis si MARIE).
 */
export interface SuccessionBeDevolutionReserveRequest {
  dateDeces: string;
  etatCivilDefunt: EtatCivilDefuntBe;
  regimeMatrimonialDefunt: RegimeMatrimonialDefuntBe | null;
  nombreEnfantsVivants: number;
  nombreEnfantsPredecedesAvecDescendants: number;
  presenceParentsVivants: boolean;
  presenceFreresSoeursOuDescendants: boolean;
  masseSuccessoraleEur: number;
  libertesConsentiesEur: number;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `SuccessionBeDevolutionReserveRequest`) pour permettre la ré-édition du
 * formulaire après rechargement, **plus** les champs calculés (verdict,
 * héritiers, réserve globale, quotité disponible, dépassement, bases
 * juridiques).
 */
export interface SuccessionBeDevolutionReserveResponse
  extends SuccessionBeDevolutionReserveRequest {
  caseFileId: string;
  verdict: SuccessionBeDevolutionReserveVerdict;
  heritiers: HeritierBe[];
  reserveGlobaleEur: number;
  reserveGlobaleFraction: string;
  quotiteDisponibleEur: number;
  quotiteDisponibleFraction: string;
  depassementQuotiteEur: number;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
