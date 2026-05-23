/**
 * SF-216-26 : modèles TypeScript de l'outil "Présomption de paternité du
 * mari et désaveu" (F-FA-PRESOMPTION-PATERNITE, FRANCE uniquement —
 * art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1).
 *
 * Contrat API importé de SF-216-25 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/presomption-paternite
 *   GET  /api/v1/case-files/{caseFileId}/presomption-paternite
 */

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/presomption-paternite`.
 *
 * 9 champs (cf. backend `PresomptionPaterniteRequest`).
 */
export interface PresomptionPaterniteRequest {
  /** ISO date YYYY-MM-DD. Requis. */
  dateNaissanceEnfant: string | null;
  /** ISO date YYYY-MM-DD. Requis. */
  dateConclusionMariage: string | null;
  /** ISO date YYYY-MM-DD. Optionnel — si mariage dissous. */
  dateDissolutionMariage: string | null;
  /** ISO date YYYY-MM-DD. Optionnel — sauf accouchement posthume. */
  dateAccouchement: string | null;
  conceptionEn180PremiersMoisMariage: boolean | null;
  enfantNeApresDisso: boolean | null;
  desaveuEnvisage: boolean | null;
  possessionEtatConformeDetecte: boolean | null;
  /** ISO date YYYY-MM-DD. Optionnel — point de départ délai désaveu. */
  dateConnaissanceNaissance: string | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface PresomptionPaterniteResponse {
  caseFileId: string;
  presomptionApplicable: boolean;
  presomptionRenversee: boolean;
  /**
   * "DESAVEU_RECEVABLE" | "DESAVEU_DELAI_FORCLOS" |
   * "DESAVEU_DIFFICILE_POSSESSION_ETAT" | "DESAVEU_SANS_OBJET" |
   * "INDETERMINE".
   */
  voieDesaveu: string;
  delaiDesaveu: string;
  possessionEtatImpact: string;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
