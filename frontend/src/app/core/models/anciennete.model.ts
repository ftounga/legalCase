export interface AncienneteRequest {
  conventionCode: string;
  dateEntree: string;
  salaireBase: number;
  congesContrat: number;
  primeContrat: number;
}

export interface AncienneteEcart {
  champ: string;
  attendu: string;
  contractuel: string;
  verdict: string;
}

export interface AncienneteResponse {
  caseFileId: string;
  conventionCode: string;
  dateEntree?: string | null;
  salaireBase?: number | null;
  congesContrat?: number | null;
  primeContrat?: number | null;
  conventionLabel: string;
  country: string;
  ancienneteAnnees: number;
  ancienneteMois: number;
  congesLegauxJours: number;
  congesSupplementairesJours: number;
  congesTotalJours: number;
  primeAnciennetePourcentage: number;
  primeAncienneteMontant: number;
  baremePrimePourcentage: number;
  baremeCongesTotal: number;
  ecarts: AncienneteEcart[];
}
