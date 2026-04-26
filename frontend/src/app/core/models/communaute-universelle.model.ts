/**
 * SF-FA-16-02 : types frontend pour l'outil décisionnel "Communauté
 * universelle" (F-FA-16). FR uniquement — art. 1526 Cciv (4ᵉ régime
 * matrimonial conventionnel) + art. 1527 al. 2 Cciv (action en
 * retranchement des enfants non communs).
 *
 * Contrat figé dans SF-FA-16-01 (backend, mergé PR #648).
 */

/** Dispositif d'analyse — 2 modes mutuellement exclusifs. */
export type DispositifAnalyse = 'VALIDITE_CONVENTION' | 'LIQUIDATION_DECES';

/** Verdict de validité (scoring niveau 5). */
export type VerdictValidite = 'VALIDE' | 'CONTESTABLE' | 'NUL';

/** Libellés humains des dispositifs (radio-button). */
export const DISPOSITIF_ANALYSE_LABELS:
  ReadonlyArray<{ code: DispositifAnalyse; label: string; description: string }> = [
  {
    code: 'VALIDITE_CONVENTION',
    label: 'Validité de la convention',
    description: 'Vérifier la validité du contrat de mariage instituant la communauté universelle (art. 1394 + 1526 Cciv).',
  },
  {
    code: 'LIQUIDATION_DECES',
    label: 'Liquidation au décès',
    description: 'Liquider le régime suite au décès d\'un époux (clause d\'attribution intégrale + action en retranchement art. 1527 al. 2 Cciv).',
  },
];

export interface CommunauteUniverselleRequest {
  dispositifAnalyse: DispositifAnalyse;
  contratNotarie: boolean;
  // VALIDITE_CONVENTION
  inscriptionEtatCivil?: boolean | null;
  consentementLibreDesEpoux?: boolean | null;
  respectReserveHereditaire?: boolean | null;
  // LIQUIDATION_DECES
  clauseAttributionIntegrale?: boolean | null;
  enfantsNonCommuns?: boolean | null;
  valeurCommunauteEur?: number | null;
}

export interface CommunauteUniverselleResponse {
  caseFileId: string;
  dispositifAnalyse: DispositifAnalyse;
  verdictValidite: VerdictValidite;
  actionRetranchementPossible: boolean;
  partAttributionConjointPct: number;
  valeurAttributionEur: number;
  scoreValidite: number;
  risquesIdentifies: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
