export type OrigineInaptitudeFr =
  | 'PROFESSIONNELLE'
  | 'NON_PROFESSIONNELLE';

export type OrigineInaptitudeBe =
  | 'PROFESSIONNELLE_BE'
  | 'NON_PROFESSIONNELLE_BE';

export type OrigineInaptitude = OrigineInaptitudeFr | OrigineInaptitudeBe;

export interface InaptitudeRequest {
  salaireMensuelReference: number;
  ancienneteAnnees: number;
  origineInaptitude: OrigineInaptitude;
  reclassementRespecte: boolean;
  avisMedecinTravailDate?: string; // YYYY-MM-DD
}

export interface InaptitudeResponse {
  caseFileId: string;
  salaireMensuelReference: number;
  ancienneteAnnees: number;
  origineInaptitude: OrigineInaptitude;
  reclassementRespecte: boolean;
  avisMedecinTravailDate: string | null;
  country: 'FRANCE' | 'BELGIQUE';
  indemniteLegale: number;
  indemniteCompensatricePreavis: number;
  damagesReclassement: number;
  total: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface OrigineInaptitudeOption {
  code: OrigineInaptitude;
  label: string;
}

export const ORIGINES_FR: OrigineInaptitudeOption[] = [
  { code: 'PROFESSIONNELLE', label: 'Origine professionnelle (AT/MP)' },
  { code: 'NON_PROFESSIONNELLE', label: 'Origine non professionnelle' },
];

export const ORIGINES_BE: OrigineInaptitudeOption[] = [
  { code: 'PROFESSIONNELLE_BE', label: 'Origine professionnelle (BE)' },
  { code: 'NON_PROFESSIONNELLE_BE', label: 'Origine non professionnelle (BE)' },
];
