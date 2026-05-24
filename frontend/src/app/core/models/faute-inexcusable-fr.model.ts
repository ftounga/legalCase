/**
 * SF-212-10 : modèles TypeScript de l'outil "Faute inexcusable de
 * l'employeur" (F-DT-91, FRANCE uniquement — L. 452-1 à L. 452-5 CSS ;
 * Cass. ass. plén. 24/06/2005 ; L. 4121-1 CT).
 *
 * Contrat API importé de SF-212-09 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/faute-inexcusable-employeur`).
 */

/** Verdict d'évaluation de la faute inexcusable — 3 niveaux. */
export type FauteInexcusableFrEvaluation =
  | 'FAUTE_INEXCUSABLE_PROBABLE'
  | 'FAUTE_INEXCUSABLE_POSSIBLE'
  | 'FAUTE_INEXCUSABLE_PEU_PROBABLE';

/** Code structuré d'un facteur de faute inexcusable détecté. */
export type FauteInexcusableFrCodeFacteur =
  | 'DT91_CONSCIENCE_DANGER'
  | 'DT91_SIGNALEMENT_PRIOR'
  | 'DT91_MESURES_PREVENTION'
  | 'DT91_DUER'
  | 'DT91_FORMATION_SECURITE';

/** Facteur détecté — structure exposée dans la réponse API. */
export interface FauteInexcusableFrFacteur {
  code: FauteInexcusableFrCodeFacteur;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/faute-inexcusable-employeur`.
 *
 * 8 champs : 5 toggles (conscience danger / signalement / mesures prévention /
 * DUER / formation sécurité), taux IPP, rente mensuelle, salaire mensuel brut.
 */
export interface FauteInexcusableFrRequest {
  conscienceDangerEmployeurEtablie: boolean | null;
  signalementDangerPrior: boolean | null;
  mesuresPreventionPrises: boolean | null;
  documentUniqueEvalue: boolean | null;
  formationSecuriteProdiguee: boolean | null;
  tauxIpp: number;
  renteMensuelleEuros: number | null;
  salaireMensuelBrutEuros: number;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * facteurs, majoration rente, alerte procédure pôle social, bases
 * juridiques, messages).
 *
 * <p>Invariant : `alerteProcedurePolesSocial` est toujours présente
 * (jamais null, jamais omise — distinction procédurale TJ vs CPH).</p>
 */
export interface FauteInexcusableFrResponse extends FauteInexcusableFrRequest {
  caseFileId: string;
  evaluationFauteInexcusable: FauteInexcusableFrEvaluation;
  scoreFauteInexcusable: number;
  facteursFauteInexcusable: FauteInexcusableFrFacteur[];
  majorationRenteEstimeeEuros: number | null;
  alerteProcedurePolesSocial: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
