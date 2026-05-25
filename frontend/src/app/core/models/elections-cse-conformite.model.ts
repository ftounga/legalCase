/**
 * SF-212-32 : modèles TypeScript de l'outil "Élections CSE — conformité"
 * (F-DT-65-elections-cse-conformite, FRANCE uniquement — L. 2314-1 à
 * L. 2314-37 CT ; R. 2314-1+ CT ; ordonnance n°2017-1386 du 22/09/2017
 * instituant le CSE ; L. 2314-32 CT — délai de contestation 15 jours).
 *
 * Contrat API importé de SF-212-31 (backend, endpoints POST/GET figés
 * sur `/api/v1/case-files/{caseFileId}/elections-cse-conformite`).
 */

/** Verdict de conformité — 3 niveaux. */
export type ElectionsCseAnalyse =
  | 'CONFORME'
  | 'IRREGULARITE_MINEURE'
  | 'IRREGULARITE_MAJEURE';

/** Code structuré d'un point d'irrégularité F-IA-03. */
export type ElectionsCseCodeCritere =
  | 'DT65_PAP_NEGOCIE'
  | 'DT65_DELAI_INVITATION'
  | 'DT65_COLLEGES'
  | 'DT65_CONTESTATION';

/** Point d'irrégularité retourné dans la réponse API. */
export interface ElectionsCsePointIrregularite {
  code: ElectionsCseCodeCritere;
  libelle: string;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/elections-cse-conformite`.
 *
 * 7 champs : effectif entreprise, PAP négocié, délai d'invitation OS,
 * collèges conformes, résultats contestés, date élection (ISO),
 * motif de contestation libre.
 */
export interface ElectionsCseConformiteRequest {
  effectifEntreprise: number | null;
  papNegocieAvecOS: boolean | null;
  delaiInvitationOSRespectee: boolean | null;
  collegesConformes: boolean | null;
  resultatsContestes: boolean | null;
  /** Date ISO YYYY-MM-DD du 1er tour ; null si non documenté. */
  dateElection: string | null;
  motifContestation: string | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs
 * (pour ré-édition du formulaire UI) ET les sorties calculées (verdict
 * 3 niveaux, score, points d'irrégularité F-IA-03, délai de contestation
 * 15 jours, date limite éventuelle, alerte délai court).
 */
export interface ElectionsCseConformiteResponse extends ElectionsCseConformiteRequest {
  caseFileId: string;
  analyseElectionsCse: ElectionsCseAnalyse;
  scoreConformite: number;
  pointsIrregularite: ElectionsCsePointIrregularite[];
  /** Toujours 15 (L. 2314-32 CT). */
  delaiContestationJours: number;
  /** Date limite ISO YYYY-MM-DD ; null si `dateElection` absente. */
  dateLimitContestationSiConnue: string | null;
  /** True si la date limite approche (≤ 15 j) ou n'est pas encore expirée. */
  alerteDelaiContestation: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
