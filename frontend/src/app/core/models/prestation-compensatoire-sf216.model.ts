/**
 * SF-216-02 : modèles TypeScript de l'outil "Prestation compensatoire" (F-FA-01,
 * FRANCE uniquement — art. 270-281 Cciv).
 *
 * Contrat API importé de SF-216-01 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/prestation-compensatoire
 *   GET  /api/v1/case-files/{caseFileId}/prestation-compensatoire
 *
 * Fondement :
 *  - art. 270 Cciv : disparité dans les conditions de vie respectives des époux.
 *  - art. 271-272 Cciv : critères d'appréciation (durée mariage, revenus,
 *    patrimoine, âge, état de santé, qualifications professionnelles…).
 *  - art. 274-275 Cciv : formes (capital, rente, mixte).
 *
 * Cet outil **remplace** le wrapper présentationnel SF-198-01 (qui restituait
 * `synthesis.prestationCompensatoireEstimate`) par un vrai simulateur backend
 * persistant (table `prestation_compensatoire_analyses_sf216`).
 */

/** Forme de prestation (cf. backend `FormePrestationEnum` — art. 274 Cciv). */
export type FormePrestation = 'CAPITAL' | 'RENTE' | 'RENTE_CONVERTIBLE' | 'INCERTAIN';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/prestation-compensatoire`.
 *
 * 9 champs (cf. backend `PrestationCompensatoireRequest`). Tous les `Integer`
 * Java sont des `number | null` côté frontend ; `null` est interdit pour les
 * 5 champs obligatoires (durée + 2 revenus + 2 âges) — validation backend.
 */
export interface PrestationCompensatoireRequest {
  dureeMariageAnnees: number | null;
  revenusAnnuelsEpoux1Eur: number | null;
  revenusAnnuelsEpoux2Eur: number | null;
  patrimoinePropre1Eur: number | null;
  patrimoinePropre2Eur: number | null;
  ageEpoux1Annees: number | null;
  ageEpoux2Annees: number | null;
  formePrestationDemandee: FormePrestation | null;
  avantageMatrimonialDetecte: boolean | null;
}

/**
 * Réponse POST / GET. Le backend ne ré-expose PAS les inputs (contrairement à
 * SF-206-08) — seuls les champs calculés sont retournés. La ré-édition du
 * formulaire repart des dernières valeurs saisies en mémoire frontend.
 */
export interface PrestationCompensatoireResponse {
  caseFileId: string;
  montantCapitalIndicatifEur: number;
  renteIndicativeMensuelleEur: number;
  formeRecommandee: FormePrestation;
  /**
   * Notez le 'A' majuscule au début de "Arite" — alignement strict sur le
   * record Java `PrestationCompensatoireResponse` (SF-216-01) qui expose le
   * champ sous le nom JSON `dispAriteNiveauxVie` (typo backend conservée pour
   * éviter une migration de contrat — corrigée en F-216 suivant).
   */
  dispAriteNiveauxVie: string;
  baseJuridique: string;
  messages: string[];
  alertes: string[];
  country: string;
}
