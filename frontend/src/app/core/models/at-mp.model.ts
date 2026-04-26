/**
 * SF-DT-33-02 : types frontend pour l'outil décisionnel "Accident du travail /
 * Maladie professionnelle" (F-DT-33). FR uniquement (Code de la sécurité sociale —
 * art. L.411-1 / L.441-1 / L.441-6 / L.461-1 / L.461-5 / L.434-2 / L.142-2 /
 * R.142-1 et s. / R.441-13 / R.461-9).
 *
 * Contrat figé dans SF-DT-33-01 (backend, mergé PR #649).
 */

/** Dispositif AT/MP — 3 valeurs alignées backend. */
export type AtMpDispositif =
  | 'RECONNAISSANCE_AT'
  | 'RECONNAISSANCE_MP'
  | 'CONTESTATION_TAUX_IPP';

/** Compétence procédurale — 4 valeurs alignées backend. */
export type AtMpCompetence =
  | 'CPAM'
  | 'CRRMP'
  | 'CMRA'
  | 'TJ_POLE_SOCIAL';

/** Verdict de recevabilité (scoring niveau 5). */
export type AtMpVerdictRecevabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Constante "hors tableau" pour la reconnaissance MP via CRRMP. */
export const NUMERO_TABLEAU_HORS_TABLEAU = 'HORS_TABLEAU';

/** Libellés humains pour mat-radio dispositif. */
export const ATMP_DISPOSITIF_LABELS:
  ReadonlyArray<{ code: AtMpDispositif; label: string; sub: string }> = [
  {
    code: 'RECONNAISSANCE_AT',
    label: 'Reconnaissance accident du travail',
    sub: 'CSS art. L.411-1 — présomption d\'imputabilité au temps et lieu du travail',
  },
  {
    code: 'RECONNAISSANCE_MP',
    label: 'Reconnaissance maladie professionnelle',
    sub: 'CSS art. L.461-1 — tableau ou système complémentaire CRRMP',
  },
  {
    code: 'CONTESTATION_TAUX_IPP',
    label: 'Contestation taux d\'incapacité permanente',
    sub: 'CSS art. L.434-2 + L.142-2 — recours CMRA puis TJ Pôle Social',
  },
];

/** Libellés humains pour le verdict. */
export const ATMP_VERDICT_LABELS: Record<AtMpVerdictRecevabilite, string> = {
  ELEVEE: 'Recevabilité élevée',
  MOYENNE: 'Recevabilité moyenne',
  FAIBLE: 'Recevabilité faible',
};

/** Libellés humains pour la compétence. */
export const ATMP_COMPETENCE_LABELS: Record<AtMpCompetence, string> = {
  CPAM: 'CPAM (Caisse primaire d\'assurance maladie)',
  CRRMP: 'CRRMP (Comité régional de reconnaissance des maladies professionnelles)',
  CMRA: 'CMRA (Commission médicale de recours amiable)',
  TJ_POLE_SOCIAL: 'TJ — Pôle Social (compétence exclusive — L.142-2 CSS)',
};

export interface AtMpRequest {
  dispositif: AtMpDispositif;
  // RECONNAISSANCE_AT
  /** YYYY-MM-DD. */
  dateAccident?: string | null;
  lieuTravail?: boolean | null;
  declarationEmployeurDansLes48h?: boolean | null;
  // RECONNAISSANCE_AT + RECONNAISSANCE_MP
  certificatMedicalInitial?: boolean | null;
  // RECONNAISSANCE_MP
  /** Numéro de tableau MP, ou la constante `HORS_TABLEAU`. */
  numeroTableau?: string | null;
  delaiPriseEnChargeRespecte?: boolean | null;
  /** YYYY-MM-DD. */
  dateExposition?: string | null;
  // CONTESTATION_TAUX_IPP
  tauxFixeParCpam?: number | null;
  tauxRevendique?: number | null;
  expertiseMedicaleProduite?: boolean | null;
  /** YYYY-MM-DD. */
  datePremierAvisCpam?: string | null;
}

export interface AtMpResponse {
  caseFileId: string;
  country: string;
  dispositif: AtMpDispositif;
  dispositifLibelle: string;
  // AT
  dateAccident: string | null;
  lieuTravail: boolean | null;
  declarationEmployeurDansLes48h: boolean | null;
  certificatMedicalInitial: boolean | null;
  // MP
  numeroTableau: string | null;
  delaiPriseEnChargeRespecte: boolean | null;
  dateExposition: string | null;
  // IPP
  tauxFixeParCpam: number | null;
  tauxRevendique: number | null;
  expertiseMedicaleProduite: boolean | null;
  datePremierAvisCpam: string | null;
  // Verdict
  verdictRecevabilite: AtMpVerdictRecevabilite;
  delaiInstructionJours: number;
  competence: AtMpCompetence;
  expertiseRequise: boolean;
  documentsRequis: string[];
  risqueRefus: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
}
