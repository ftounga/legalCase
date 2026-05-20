/**
 * SF-207-04b : modèle frontend de l'outil F-207 — Déclaration AT Fedris
 * (Belgique uniquement, Loi 10/04/1971 art. 62 + AR 21/12/1971 art. 25).
 * Contrat figé dans SF-207-04 backend (PR #1142).
 *
 * Délai de 8 jours calendaires pour l'employeur belge à compter de la
 * connaissance de l'accident du travail. Deux modes :
 *  - prospectif : pas de date de déclaration effective fournie. Verdict
 *    parmi {DELAI_OUVERT, DELAI_IMMINENT, DELAI_DEPASSE}.
 *  - rétrospectif : dateDeclarationEffectuee fournie → verdict parmi
 *    {DECLARATION_DANS_LES_TEMPS, DECLARATION_HORS_DELAI}.
 */

/** Verdict global, 5 états (3 prospectifs + 2 rétrospectifs). */
export type AtFedrisDeclarationVerdict =
  | 'DELAI_OUVERT'
  | 'DELAI_IMMINENT'
  | 'DELAI_DEPASSE'
  | 'DECLARATION_DANS_LES_TEMPS'
  | 'DECLARATION_HORS_DELAI';

export interface AtFedrisDeclarationRequest {
  /** Requis, ISO YYYY-MM-DD — date de survenance de l'accident du travail. */
  dateAccident: string;
  /** Optionnel ; si null, le service utilise dateAccident. */
  dateConnaissanceEmployeur?: string | null;
  /** Optionnel ; si null, le service utilise today (Europe/Brussels). */
  dateActionEnvisagee?: string | null;
  /**
   * Optionnel — si renseigné, le calculateur bascule en mode rétrospectif
   * et émet un verdict DECLARATION_DANS_LES_TEMPS ou DECLARATION_HORS_DELAI.
   */
  dateDeclarationEffectuee?: string | null;
}

export interface AtFedrisDeclarationResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  dateAccident: string;
  dateConnaissanceEmployeur: string;
  dateActionEnvisagee: string;
  dateDeclarationEffectuee: string | null;
  // Outputs calculés
  verdict: AtFedrisDeclarationVerdict;
  /** Date butoir ISO YYYY-MM-DD = dateConnaissanceEmployeur + 8 jours. */
  dateLimiteDeclaration: string;
  /** Jours restants ; négatif si déjà dépassé. */
  joursRestants: number;
  regleAppliquee: string;
  baseJuridique: string;
  formuleCalcul: string;
  consequencesNonRespect: string;
  country: 'BELGIQUE';
}

/** Libellés humains FR du verdict — alignés sur le pilote UI. */
const VERDICT_LABELS: Record<AtFedrisDeclarationVerdict, string> = {
  DELAI_OUVERT: 'Délai ouvert',
  DELAI_IMMINENT: 'Délai imminent',
  DELAI_DEPASSE: 'Délai dépassé',
  DECLARATION_DANS_LES_TEMPS: 'Déclaration dans les temps',
  DECLARATION_HORS_DELAI: 'Déclaration hors délai',
};

/** Libellé humain FR du verdict. */
export function humanizeVerdict(v: AtFedrisDeclarationVerdict): string {
  return VERDICT_LABELS[v] ?? v;
}
