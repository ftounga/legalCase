/**
 * SF-216-30 : modèles TypeScript de l'outil "Donation-partage"
 * (F-FA-DONATION-PARTAGE, FRANCE uniquement — art. 1075 à 1075-5 Cciv +
 * art. 1078, 1078-1, 1080 + art. 912-928).
 *
 * Contrat API importé de SF-216-29 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/donation-partage
 *   GET  /api/v1/case-files/{caseFileId}/donation-partage
 */

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/donation-partage`.
 *
 * 7 champs (cf. backend `DonationPartageRequest`).
 */
export interface DonationPartageRequest {
  nombreDescendants: number | null;
  presencePetitsEnfantsParSubstitution: boolean | null;
  donationPartageConjonctive: boolean | null;
  valeurPartageTotal: number | null;
  respectQuotiteDisponible: boolean | null;
  donationsAnterieuresAReinorporer: boolean | null;
  agesDonateurs: number[] | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface DonationPartageResponse {
  caseFileId: string;
  conditionsRemplies: boolean;
  /** "FORT" | "MOYEN" | "FAIBLE" | "INADAPTE". */
  interet: string;
  gelValeurEffet: string;
  rapportExclu: boolean;
  alerteQuotite: boolean;
  etapesNotariales: string[];
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
