/**
 * SF-218-12 : modèles TypeScript de l'outil "VRP : statut, préavis et indemnité
 * de clientèle" (F-DT-104 — FRANCE uniquement).
 *
 * Contrat API importé de SF-218-11 (backend, endpoints POST/GET figés).
 * Fondement : art. L.7311-1 et s. CT (statut VRP), L.7313-13 (indemnité de
 * clientèle), L.7313-9 (préavis VRP), non-cumul / option la plus favorable
 * (jurisprudence constante Cass. soc.).
 */

/** Cause de la rupture du contrat VRP. */
export type VrpCauseRupture =
  | 'LICENCIEMENT_CAUSE_REELLE'
  | 'FAUTE_GRAVE'
  | 'FAUTE_LOURDE'
  | 'DEMISSION'
  | 'DEPART_RETRAITE'
  | 'RUPTURE_CONVENTIONNELLE';

/** Type de VRP (exclusif d'un seul employeur ou multicartes). */
export type VrpType = 'EXCLUSIF' | 'MULTICARTES';

/** Verdict d'éligibilité à l'indemnité de clientèle (L.7313-13). */
export type VrpEligibiliteClientele = 'DUE' | 'NON_DUE';

/** Option d'indemnité recommandée (non-cumul — la plus favorable). */
export type VrpOptionRecommandee = 'INDEMNITE_CLIENTELE' | 'INDEMNITE_LEGALE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/vrp-indemnite-clientele-analysis`.
 * Dates au format ISO `YYYY-MM-DD`.
 */
export interface VrpIndemniteClienteleRequest {
  dateEntree: string | null;
  dateRupture: string | null;
  causeRupture: VrpCauseRupture;
  typeVrp: VrpType;
  commissionsAnnuellesMoyennes: number;
  salaireMensuelMoyen: number;
  clienteleDeveloppee: boolean;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose le snapshot d'inputs pour permettre la ré-édition du formulaire
 * après rechargement, **plus** les champs calculés (préavis, éligibilité,
 * fourchette indemnité de clientèle, indemnité légale comparée, option
 * recommandée, base juridique).
 */
export interface VrpIndemniteClienteleResponse extends VrpIndemniteClienteleRequest {
  caseFileId: string;
  /** Préavis VRP spécifique (1 / 2 / 3 mois selon ancienneté — L.7313-9). */
  dureePreavisMois: number;
  eligibiliteClientele: VrpEligibiliteClientele;
  /** Motif de non-due (présent ssi `eligibiliteClientele === 'NON_DUE'`). */
  motifNonDue: string | null;
  /** Fourchette indicative — 1 × à 2 × commissions annuelles moyennes. */
  indemniteClienteleMin: number;
  indemniteClienteleMax: number;
  /** Indemnité légale de licenciement comparée (R.1234-2). */
  indemniteLegaleLicenciement: number;
  optionRecommandee: VrpOptionRecommandee;
  baseJuridique: string;
  country: string;
  calculatedAt: string;
}
