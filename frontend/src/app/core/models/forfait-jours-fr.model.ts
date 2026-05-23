/**
 * SF-212-04 : modèles TypeScript de l'outil "Forfait jours — validité et
 * rappel HS" (F-DT-50 — FRANCE uniquement).
 *
 * Contrat API importé de SF-212-03 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/forfait-jours-validite`).
 *
 * Fondement : L. 3121-58 à L. 3121-66 CT (régime forfait jours + garanties :
 * cadre autonome, accord collectif, entretien annuel, document de contrôle,
 * plafond 218 j), Cass. soc. 29/06/2011 n°09-71.107 (accord Syntec —
 * exigence de garantie effective du suivi de la charge), L. 3245-1 CT
 * (prescription 3 ans rappel de salaire).
 */

/** Verdict de validité de la convention de forfait jours — 3 niveaux. */
export type ForfaitJoursValiditeForfait =
  | 'VALIDE'
  | 'PARTIELLEMENT_NULLE'
  | 'NULLE';

/** Code structuré d'un facteur d'invalidité détecté. */
export type ForfaitJoursCodeFacteur =
  | 'DT50_ACCORD_COLLECTIF'
  | 'DT50_SUIVI_CHARGE'
  | 'DT50_ENTRETIEN_ANNUEL'
  | 'DT50_SUIVI_JOURS'
  | 'DT50_CATEGORIE_AUTONOME'
  | 'DT50_NB_JOURS';

/** Facteur d'invalidité — structure exposée dans la réponse API. */
export interface ForfaitJoursFacteurInvalidite {
  code: ForfaitJoursCodeFacteur;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/forfait-jours-validite`.
 *
 * 10 champs : 5 toggles de validité, nb jours forfait, ancienneté, salaire,
 * HS estimées/sem, nb semaines/an.
 */
export interface ForfaitJoursValiditeRequest {
  accordCollectifExiste: boolean | null;
  accordGarantitSuiviCharge: boolean | null;
  entretienAnnuelRealise: boolean | null;
  documentControleMensuelExiste: boolean | null;
  categorieAutonomeConfirmee: boolean | null;
  nbJoursForfait: number;
  ancienneteMois: number;
  salaireMensuelBrutEuros: number;
  hsEstimeesParSemaine: number;
  nbSemainesParAn: number;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * facteurs d'invalidité, rappel HS estimé sur 3 ans, bases juridiques,
 * messages).
 */
export interface ForfaitJoursValiditeResponse extends ForfaitJoursValiditeRequest {
  caseFileId: string;
  validiteForfait: ForfaitJoursValiditeForfait;
  scoreValidite: number;
  facteursInvalidite: ForfaitJoursFacteurInvalidite[];
  rappelHsEstimeEuros: number | null;
  prescriptionRappelAns: number;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
