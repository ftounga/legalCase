/**
 * SF-212-16 : modèles TypeScript de l'outil "Télétravail — conformité et
 * litige" (F-DT-82-teletravail-accord, FRANCE uniquement — L. 1222-9 à
 * L. 1222-11 CT ; ANI télétravail 26/11/2020).
 *
 * Contrat API importé de SF-212-15 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/teletravail-accord`).
 */

/** Verdict d'analyse de la conformité du dispositif de télétravail — 3 niveaux. */
export type TeletravailAnalyse =
  | 'CONFORME'
  | 'NON_CONFORME'
  | 'LITIGE_IDENTIFIE';

/** Cadre juridique du télétravail (L. 1222-9 CT). */
export type TeletravailCadre =
  | 'ACCORD_COLLECTIF'
  | 'CHARTE_UNILATERALE'
  | 'ACCORD_INDIVIDUEL'
  | 'AUCUN';

/** Code structuré F-IA-03 d'un critère analysé. */
export type TeletravailCodeCritere =
  | 'DT82_CADRE_JURIDIQUE'
  | 'DT82_DOUBLE_VOLONTARIAT'
  | 'DT82_INDEMNITE_OCCUPATION'
  | 'DT82_ACCIDENT_DOMICILE';

/** Point d'analyse exposé dans la réponse API. */
export interface TeletravailPointAnalyse {
  code: TeletravailCodeCritere;
  libelle: string;
  fondement: string;
  conclusion: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/teletravail-accord`.
 *
 * 7 champs : cadre juridique (select), double volontariat (toggle), indemnité
 * versée (toggle), montant journalier en euros (nullable), accident domicile
 * (toggle), retour bureau unilatéral (toggle), refus invoqué (toggle).
 */
export interface TeletravailAccordRequest {
  cadreTeletravail: TeletravailCadre;
  doubleVolontariatRespectee: boolean;
  indemniteOccupationVersee: boolean;
  montantIndemniteJournalierEuros: number | null;
  accidentDomicileDetecte: boolean;
  retourBureauImposeUnilateralement: boolean;
  refusTeletravailCauseIncrimination: boolean;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * points d'analyse F-IA-03, alertes AT/refus-licenciement distinctes,
 * estimation indemnité due, bases juridiques, messages).
 */
export interface TeletravailAccordResponse extends TeletravailAccordRequest {
  caseFileId: string;
  analyseTeletravail: TeletravailAnalyse;
  scoreTeletravail: number;
  pointsTeletravail: TeletravailPointAnalyse[];
  alerteAccidentTravailDomicile: boolean;
  alerteRefusTeletravailLicenciement: boolean;
  indemniteDueEstimeeEuros: number | null;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
