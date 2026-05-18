/**
 * SF-217-07 : modèles TypeScript de l'outil décisionnel "Contribution
 * alimentaire des enfants (Belgique)" (Vague 2 Famille BE — méthode Renard,
 * CC art. 203 / 203bis). BELGIQUE uniquement.
 *
 * Contrat API importé de SF-217-06 (backend, endpoints POST/GET figés).
 */

/** Verdict de l'estimation de contribution alimentaire. */
export type ContributionAlimentaireEnfantsBeVerdict =
  | 'CONTRIBUTION_DUE'
  | 'CONTRIBUTION_EQUILIBREE'
  | 'DONNEES_INSUFFISANTES';

/** Tranche d'âge des enfants concernés. */
export type TrancheAgeEnfantBe =
  | 'ENFANT_0_5'
  | 'ENFANT_6_11'
  | 'ENFANT_12_17'
  | 'ENFANT_18_PLUS';

/** Parent identifié comme débiteur de la contribution nette. */
export type ParentDebiteurBe = 'PARENT_1' | 'PARENT_2' | 'AUCUN';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be`.
 * Montants décimaux en euros. Commentaire nullable (max 1000).
 */
export interface ContributionAlimentaireEnfantsBeRequest {
  nombreEnfants: number;
  trancheAgeEnfants: TrancheAgeEnfantBe;
  revenuMensuelParent1: number;
  revenuMensuelParent2: number;
  coutMensuelGlobalEnfants: number | null;
  nuitsHebergementParent1: number;
  nuitsHebergementParent2: number;
  allocationsFamilialesMensuelles: number | null;
  fraisExtraordinairesMensuels: number | null;
  parentDebiteurEstParent1: boolean | null;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `ContributionAlimentaireEnfantsBeRequest`) pour permettre la ré-édition du
 * formulaire après rechargement, **plus** les champs calculés (méthode Renard).
 */
export interface ContributionAlimentaireEnfantsBeResponse
  extends ContributionAlimentaireEnfantsBeRequest {
  caseFileId: string;
  verdict: ContributionAlimentaireEnfantsBeVerdict;
  coutMensuelRetenu: number;
  coutNetApresAllocations: number;
  quotePartParent1Pct: number;
  quotePartParent2Pct: number;
  partContributiveParent1: number;
  partContributiveParent2: number;
  partHebergementParent1: number;
  partHebergementParent2: number;
  contributionMensuelleNette: number;
  parentDebiteur: ParentDebiteurBe;
  fraisExtraordinairesQuotePartParent1: number;
  fraisExtraordinairesQuotePartParent2: number;
  detailCalcul: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
