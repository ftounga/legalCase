/**
 * SF-207-03b : modèle frontend de l'outil F-207 — Contestation C4 ONEM Travail BE
 * (Belgique uniquement, AR 25/11/1991 art. 144 + Code judiciaire art. 580 2°).
 * Contrat figé dans SF-207-03 backend (PR #1137).
 */

/**
 * Verdict global de la contestation, 6 états selon le palier pertinent
 * (ADMIN pour cas A — recours admin non encore formé ;
 *  TRIBUNAL pour cas B — recours admin déjà formé).
 */
export type ContestationC4OnemVerdict =
  | 'RECOURS_ADMIN_OUVERT'
  | 'RECOURS_ADMIN_IMMINENT'
  | 'RECOURS_ADMIN_PRESCRIT'
  | 'RECOURS_TRIBUNAL_OUVERT'
  | 'RECOURS_TRIBUNAL_IMMINENT'
  | 'RECOURS_TRIBUNAL_PRESCRIT';

/** Orientation procédurale recommandée à l'avocat selon le verdict. */
export type ContestationC4OnemEtapeSuivante =
  | 'RECOURS_ADMIN_DIRECTEUR'
  | 'RECOURS_TRIBUNAL_TRAVAIL'
  | 'FORCLUSION_TOTALE'
  | 'AUCUNE';

/** Type d'un palier : ADMIN = Directeur du Bureau du chômage ; TRIBUNAL = tribunal du travail. */
export type ContestationC4OnemPalierType = 'ADMIN' | 'TRIBUNAL';

export interface ContestationC4OnemPalier {
  type: ContestationC4OnemPalierType;
  /** Date butoir au format ISO YYYY-MM-DD. `null` si indéterminé (TRIBUNAL avant décision Directeur). */
  dateLimite: string | null;
  /** Jours restants ; peut être négatif si déjà prescrit. `null` si indéterminé. */
  joursRestants: number | null;
  /** Référence légale du palier (AR 25/11/1991 art. 144 ; CJ art. 580 2°). */
  baseJuridique: string;
}

export interface ContestationC4OnemRequest {
  /** Requis, ISO YYYY-MM-DD — date de notification de la décision ONEM. */
  dateNotificationDecisionOnem: string;
  /** Optionnel ; si null, le service utilise LocalDate.now() Europe/Brussels. */
  dateActionEnvisagee?: string | null;
  /** Cas A (false) / Cas B (true). Si true, dateDecisionDirecteur requis. */
  recoursAdminDejaForme: boolean;
  /** Requis si recoursAdminDejaForme=true, sinon null. */
  dateDecisionDirecteur?: string | null;
}

export interface ContestationC4OnemResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  dateNotificationDecisionOnem: string;
  dateActionEnvisagee: string;
  recoursAdminDejaForme: boolean;
  dateDecisionDirecteur: string | null;
  // Outputs calculés
  verdict: ContestationC4OnemVerdict;
  paliers: ContestationC4OnemPalier[];
  etapeSuivante: ContestationC4OnemEtapeSuivante;
  baseJuridique: string;
  formuleCalcul: string;
  country: 'BELGIQUE';
}

/** Libellés humains FR du verdict — alignés sur le pilote UI. */
const VERDICT_LABELS: Record<ContestationC4OnemVerdict, string> = {
  RECOURS_ADMIN_OUVERT: 'Recours administratif ouvert',
  RECOURS_ADMIN_IMMINENT: 'Recours administratif imminent',
  RECOURS_ADMIN_PRESCRIT: 'Recours administratif prescrit',
  RECOURS_TRIBUNAL_OUVERT: 'Recours tribunal du travail ouvert',
  RECOURS_TRIBUNAL_IMMINENT: 'Recours tribunal du travail imminent',
  RECOURS_TRIBUNAL_PRESCRIT: 'Recours tribunal du travail prescrit',
};

/** Libellé humain FR du verdict. */
export function humanizeVerdict(v: ContestationC4OnemVerdict): string {
  return VERDICT_LABELS[v] ?? v;
}

/** Libellé humain FR d'un palier. */
export function palierLabel(p: ContestationC4OnemPalierType): string {
  return p === 'ADMIN'
    ? 'Recours administratif (Directeur du Bureau du chômage)'
    : 'Recours juridictionnel (tribunal du travail)';
}
