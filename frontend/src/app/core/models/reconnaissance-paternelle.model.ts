/**
 * SF-FA-18-02 : modèles TypeScript pour l'outil décisionnel
 * "Reconnaissance paternelle" (F-FA-18 — FR uniquement, art. 316 Cciv).
 *
 * Contrat figé dans SF-FA-18-01 (backend, mergé PR #652).
 */

/** Sous-type de reconnaissance paternelle (art. 316 Cciv). */
export type SousTypeReconnaissance =
  | 'RECONNAISSANCE_PRENATALE'
  | 'RECONNAISSANCE_POST_NATALE_NAISSANCE'
  | 'RECONNAISSANCE_POST_NATALE_ULTERIEURE';

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabiliteReconnaissance = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio sous-type. */
export const SOUS_TYPE_RECONNAISSANCE_LABELS:
  ReadonlyArray<{ code: SousTypeReconnaissance; label: string; sub: string }> = [
  {
    code: 'RECONNAISSANCE_PRENATALE',
    label: 'Prénatale (art. 316 al. 1)',
    sub: 'Avant la naissance, devant tout officier d\'état civil',
  },
  {
    code: 'RECONNAISSANCE_POST_NATALE_NAISSANCE',
    label: 'Lors de l\'acte de naissance (art. 316 al. 2)',
    sub: 'Pendant l\'établissement de l\'acte de naissance',
  },
  {
    code: 'RECONNAISSANCE_POST_NATALE_ULTERIEURE',
    label: 'Ultérieure (art. 316 al. 3)',
    sub: 'À tout moment après la naissance',
  },
];

export interface ReconnaissancePaternelleRequest {
  sousType: SousTypeReconnaissance;
  /** ISO YYYY-MM-DD — null/undefined pour PRENATALE. */
  dateNaissanceEnfant?: string | null;
  /** ISO YYYY-MM-DD — date à laquelle la reconnaissance est faite. */
  dateReconnaissance?: string | null;
  consentementLibreDuPere: boolean;
  paterniteVraisemblable: boolean;
  enfantNonReconnuParAutrePere: boolean;
  procedureRespectee: boolean;
  presenceParProcuration: boolean;
}

export interface ReconnaissancePaternelleResponse {
  caseFileId: string;
  sousType: SousTypeReconnaissance;
  verdictRecevabilite: VerdictRecevabiliteReconnaissance;
  scoreEligibilite: number;
  effetFiliation: string | null;
  risquesContestation: string[];
  documentsRequis: string[];
  delaiContestationAns: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
