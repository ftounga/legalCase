/**
 * SF-216-24 : modèles TypeScript de l'outil "Donation entre époux"
 * (F-FA-DONATION-ENTRE-EPOUX, FRANCE uniquement — art. 1091-1100 Cciv +
 * art. 265 al. 2 + art. 1527 al. 2 + art. 912-928).
 *
 * Contrat API importé de SF-216-23 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/donation-entre-epoux
 *   GET  /api/v1/case-files/{caseFileId}/donation-entre-epoux
 */

/** Type d'avantage matrimonial / donation entre époux envisagé. */
export type AvantageMatrimonialType =
  | 'DONATION_BIEN_PRESENT'
  | 'DONATION_BIENS_FUTURS'
  | 'AVANTAGE_MATRIMONIAL'
  | 'ASSURANCE_VIE'
  | 'CLAUSE_ATTRIBUTION_INTEGRALE';

/** Régime matrimonial applicable au couple. */
export type RegimeMatrimonial =
  | 'COMMUNAUTE_LEGALE'
  | 'COMMUNAUTE_UNIVERSELLE'
  | 'SEPARATION_DE_BIENS'
  | 'PARTICIPATION_AUX_ACQUETS'
  | 'AUTRE';

/** Type de bien donné (informatif). */
export type BienDonneType =
  | 'IMMOBILIER'
  | 'MOBILIER'
  | 'PORTEFEUILLE'
  | 'NUMERAIRE'
  | 'AUTRE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/donation-entre-epoux`.
 *
 * 8 champs (cf. backend `DonationEntreEpouxRequest`).
 */
export interface DonationEntreEpouxRequest {
  avantageMatrimonialType: AvantageMatrimonialType | null;
  /** ISO date YYYY-MM-DD. */
  dateContrat: string | null;
  regimeMatrimonial: RegimeMatrimonial | null;
  mariageDissous: boolean | null;
  revocabiliteDetectee: boolean | null;
  bienDonneType: BienDonneType | null;
  valeurBienDonneEur: number | null;
  enfantsNonCommunsDetected: boolean | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface DonationEntreEpouxResponse {
  caseFileId: string;
  /** "VALIDE" | "REVOQUEE" | "A_VERIFIER". */
  validite: string;
  /**
   * "REVOCABLE_AD_NUTUM" | "REVOCATION_DE_PLEIN_DROIT" | "REVOCATION_CONSTATEE"
   * | "REVOCABILITE_LIMITEE" | "IRREVOCABLE" | "INDETERMINEE".
   */
  revocabilite: string;
  effetDissolution: string;
  actionRetranchementPossible: boolean;
  impactReserve: string;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
