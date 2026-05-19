/**
 * SF-207-01b : modèle frontend de l'outil F-207 — Prescription Travail BE
 * (Belgique uniquement, Loi 03/07/1978 art. 15 al. 1 / al. 2 + CCT 109 art. 11).
 * Contrat figé dans SF-207-01 backend (PR #1119).
 */

/**
 * Types de créance — 4 régimes de prescription distincts.
 * Mapping vers la base juridique BE :
 *   - EX_CONTRAT_GENERAL  → 1 an post-rupture (Loi 03/07/1978 art. 15 al. 1)
 *   - EX_CONTRAT_CCT_109  → 1 an post-rupture (CCT 109 art. 11)
 *   - PENDANT_CONTRAT     → 5 ans (Loi 03/07/1978 art. 15 al. 2)
 *   - ARRIERES_SALAIRE    → 5 ans (Loi 03/07/1978 art. 15 al. 2 / CC art. 2262bis)
 */
export type TypeCreance =
  | 'EX_CONTRAT_GENERAL'
  | 'EX_CONTRAT_CCT_109'
  | 'PENDANT_CONTRAT'
  | 'ARRIERES_SALAIRE';

/** Verdict du calcul de prescription. */
export type PrescriptionVerdict = 'PRESCRIT' | 'IMMINENT' | 'NON_PRESCRIT';

export interface PrescriptionBeLitigeTravailRequest {
  /** Date de rupture (ou fait générateur) au format ISO YYYY-MM-DD. */
  dateRupture: string;
  /** Type de créance — 4 valeurs cf. {@link TypeCreance}. */
  typeCreance: TypeCreance;
  /**
   * Date à laquelle l'avocat envisage d'agir (ISO YYYY-MM-DD). Si absente
   * côté requête, le backend la remplace par {@code LocalDate.now()} (Europe/Brussels).
   */
  dateActionEnvisagee?: string | null;
}

export interface PrescriptionBeLitigeTravailResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  dateRupture: string;
  typeCreance: TypeCreance;
  dateActionEnvisagee: string;
  // Outputs calculés
  verdict: PrescriptionVerdict;
  dateLimitePrescription: string;
  joursRestants: number;
  regleAppliquee: string;
  baseJuridique: string;
  formuleCalcul: string;
  country: 'BELGIQUE';
}

export interface TypeCreanceOption {
  code: TypeCreance;
  label: string;
}

export const TYPES_CREANCE: TypeCreanceOption[] = [
  { code: 'EX_CONTRAT_GENERAL', label: 'Ex-contrat — créance générale (1 an post-rupture)' },
  { code: 'EX_CONTRAT_CCT_109', label: 'Ex-contrat — motivation licenciement CCT 109 (1 an post-rupture)' },
  { code: 'PENDANT_CONTRAT', label: 'Pendant le contrat (5 ans)' },
  { code: 'ARRIERES_SALAIRE', label: 'Arriérés de salaire (5 ans)' },
];
