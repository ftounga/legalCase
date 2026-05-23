/**
 * SF-212-08 : modèles TypeScript de l'outil "CSP/CRP — conformité de la
 * proposition" (F-DT-44 — FRANCE uniquement).
 *
 * Contrat API importé de SF-212-07 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/csp-crp-conformite`).
 *
 * Fondement : L. 1233-65 à L. 1233-70 CT (obligation de proposition du CSP
 * dans les entreprises < 1 000 salariés lors d'un licenciement économique,
 * délai de réflexion 21 jours calendaires, adhésion = rupture amiable hors
 * préavis L. 1233-67, refus = licenciement normal + préavis) ; ANI CSP
 * 19/07/2011 révisé ; DARES (statistiques ASP — 75 % du SJR pendant 12 mois
 * en régime droit commun).
 */

/** Verdict de conformité de la proposition CSP — 3 niveaux. */
export type CspCrpConformiteCsp =
  | 'CONFORME'
  | 'PARTIELLEMENT_CONFORME'
  | 'NON_CONFORME';

/** Code structuré d'un point de non-conformité détecté. */
export type CspCrpCodeNonConformite =
  | 'DT44_OBLIGATION_CSP'
  | 'DT44_DOCUMENT_REMIS'
  | 'DT44_DELAI_REFLEXION'
  | 'DT44_DATE_REMISE';

/** Point de non-conformité — structure exposée dans la réponse API. */
export interface CspCrpPointNonConformite {
  code: CspCrpCodeNonConformite;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/csp-crp-conformite`.
 *
 * 9 champs : effectif entreprise, toggles CSP proposé / document remis /
 * délai mentionné, dates remise et entretien préalable, adhésion tri-état,
 * salaire mensuel brut, rémunération 12 mois.
 */
export interface CspCrpConformiteRequest {
  effectifEntreprise: number;
  cspPropose: boolean | null;
  documentInformationRemis: boolean | null;
  delaiReflexionMentionne: boolean | null;
  dateRemise: string | null;
  dateEntretienPrealable: string | null;
  adhesionSalarie: boolean | null;
  salaireMensuelBrutEuros: number;
  remunerationBrute12MoisEuros: number;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * points de non-conformité, ASP estimée journalière + annuelle, durée 12 mois,
 * bases juridiques, messages).
 */
export interface CspCrpConformiteResponse extends CspCrpConformiteRequest {
  caseFileId: string;
  obligationCspApplicable: boolean;
  conformiteCsp: CspCrpConformiteCsp;
  scoreConformite: number;
  pointsNonConformite: CspCrpPointNonConformite[];
  aspEstimeeJournaliereEuros: number | null;
  aspEstimeeAnnuelleEuros: number | null;
  dureeAspMois: number;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
