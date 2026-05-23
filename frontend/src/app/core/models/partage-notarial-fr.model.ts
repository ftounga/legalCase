/**
 * SF-216-28 : modèles TypeScript de l'outil "Partage successoral notarié"
 * (F-FA-PARTAGE-NOTARIAL, FRANCE uniquement — art. 816 et s. Cciv +
 * art. 870 Cciv + art. 1592 CGI + art. 641 CGI + art. 840 Cciv).
 *
 * Contrat API importé de SF-216-27 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/partage-notarial
 *   GET  /api/v1/case-files/{caseFileId}/partage-notarial
 */

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/partage-notarial`.
 *
 * 8 champs (cf. backend `PartageNotarialRequest`).
 */
export interface PartageNotarialRequest {
  /** ISO date YYYY-MM-DD — date d'ouverture de la succession (décès). */
  dateOuvertureSuccession: string | null;
  nombreCoheritiers: number | null;
  consentementsTousDetecte: boolean | null;
  presenceImmeuble: boolean | null;
  desaccordPersistant: boolean | null;
  valeurMasseSuccessoraleEur: number | null;
  notaireDesigne: boolean | null;
  /** ISO date YYYY-MM-DD — optionnel, calculé sinon (+6 mois). */
  declarationSuccessionEcheance: string | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface PartageNotarialResponse {
  caseFileId: string;
  notaireObligatoire: boolean;
  calendrierEtapes: string[];
  /** ISO date YYYY-MM-DD — échéance fiscale. */
  delaiDeclarationFiscale: string | null;
  alerteDelai: boolean;
  orientationJudiciaire: boolean;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
