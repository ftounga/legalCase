/**
 * SF-FA-17-02 : types frontend pour l'outil décisionnel "Partage judiciaire"
 * (F-FA-17). FR uniquement — art. 840 et s. Code civil + 1364 et s. + 1366
 * Code de procédure civile.
 *
 * Contrat figé dans SF-FA-17-01 (backend, mergé PR #636).
 */

/** Type de bien en indivision (4 valeurs alignées backend). */
export type TypeBienIndivision =
  | 'IMMEUBLE_DIVISIBLE'
  | 'IMMEUBLE_INDIVISIBLE'
  | 'MEUBLES_DIVERS'
  | 'MIXTE';

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio type bien. */
export const TYPE_BIEN_INDIVISION_LABELS:
  ReadonlyArray<{ code: TypeBienIndivision; label: string }> = [
  { code: 'IMMEUBLE_DIVISIBLE', label: 'Immeuble divisible (lots séparables)' },
  { code: 'IMMEUBLE_INDIVISIBLE', label: 'Immeuble indivisible (licitation potentielle)' },
  { code: 'MEUBLES_DIVERS', label: 'Meubles divers (mobilier, comptes bancaires)' },
  { code: 'MIXTE', label: 'Mixte (immeuble + meubles)' },
];

export interface PartageJudiciaireRequest {
  pvDifficultesEtabli: boolean;
  tentativeAmiableEpuiseuee: boolean;
  typeBienIndivision: TypeBienIndivision;
  nombreCoindivisaires: number;
  desaccordMotive: boolean;
  valeurEstimeeBiensEur: number;
}

export interface PartageJudiciaireResponse {
  caseFileId: string;
  verdictRecevabilite: VerdictRecevabilite;
  dureeProcedureMois: number;
  fraisEstimesEur: number;
  risqueLicitation: boolean;
  scoreEligibilite: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
