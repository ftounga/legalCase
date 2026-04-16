export interface CongesSupplementaireData {
  ancienneteMinAnnees: number;
  joursSupplementaires: number;
}

export interface PrimeAncienneteData {
  ancienneteMinAnnees: number;
  pourcentage: number;
}

export interface BaremeResponse {
  conventionCode: string;
  conventionLabel: string;
  country: string;
  congesLegauxJours: number;
  congesSupplementaires: CongesSupplementaireData[];
  primesAnciennete: PrimeAncienneteData[];
}
