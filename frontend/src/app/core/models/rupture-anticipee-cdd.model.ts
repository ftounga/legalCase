/**
 * SF-212-18 : modèles TypeScript de l'outil "Rupture anticipée du CDD"
 * (F-DT-43-rupture-anticipee-cdd, FRANCE uniquement — L. 1243-1 à
 * L. 1243-4 CT ; L. 1243-8 CT — indemnité de précarité ; L. 1226-4-2 CT ;
 * Cass. soc. constante).
 *
 * Contrat API importé de SF-212-17 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/rupture-anticipee-cdd`).
 */

/** Partie ayant rompu anticipativement le CDD. */
export type RacAuteurRupture = 'EMPLOYEUR' | 'SALARIE';

/** Motif invoqué pour la rupture anticipée — 6 valeurs. */
export type RacMotifRupture =
  | 'ACCORD_PARTIES'
  | 'FAUTE_GRAVE'
  | 'FORCE_MAJEURE'
  | 'INAPTITUDE'
  | 'CDI_EMBAUCHE'
  | 'AUTRE';

/** Verdict d'analyse de la rupture anticipée du CDD — 3 niveaux. */
export type RacAnalyse =
  | 'LEGITIME'
  | 'ILLEGITIME_EMPLOYEUR'
  | 'ILLEGITIME_SALARIE';

/** Code structuré F-IA-03 d'un point d'analyse. */
export type RacCodeCritere =
  | 'DT43_MOTIF_RUPTURE'
  | 'DT43_AUTEUR_RUPTURE'
  | 'DT43_DATE_TERME';

/** Point d'analyse exposé dans la réponse API. */
export interface RacPointAnalyse {
  code: RacCodeCritere;
  libelle: string;
  fondement: string;
  conclusion: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/rupture-anticipee-cdd`.
 *
 * 6 champs : auteur de la rupture (select), motif (select), date terme du
 * CDD (date), date de rupture effective (date), salaire mensuel brut,
 * rémunération brute totale du contrat (base précarité 10 %).
 */
export interface RuptureAnticipeeCddRequest {
  auteurRupture: RacAuteurRupture;
  motifRupture: RacMotifRupture;
  dateTermeCdd: string; // ISO date YYYY-MM-DD
  dateRupture: string;
  salaireMensuelBrutEuros: number;
  remunerationBruteTotaleContratEuros: number;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, mois
 * restants, points d'analyse F-IA-03, indemnités dues au salarié, bases
 * juridiques, messages).
 */
export interface RuptureAnticipeeCddResponse extends RuptureAnticipeeCddRequest {
  caseFileId: string;
  analyseRuptureAnticipee: RacAnalyse;
  moisRestantsContrat: number;
  pointsAnalyse: RacPointAnalyse[];
  indemnitesRuptureAnticipeeEuros: number;
  indemnitePrecariteEuros: number;
  totalDuSalarieEuros: number;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
