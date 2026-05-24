/**
 * SF-212-24 : modèles TypeScript de l'outil "Égalité salariale femmes/hommes"
 * (F-DT-56-egalite-salariale-femmes-hommes, FRANCE uniquement —
 * L. 1142-7 à L. 1142-10 CT ; L. 1144-1 CT ; L. 3221-2 CT ; L. 1132-1 CT ;
 * L. 1134-5 CT ; loi 05/09/2018 « avenir professionnel »).
 *
 * Contrat API importé de SF-212-23 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/egalite-salariale-fh`).
 */

/** Sexe du salarié — Femme ou Homme. */
export type EgsSexeSalarie = 'FEMME' | 'HOMME';

/** Verdict d'analyse de la discrimination salariale — 3 niveaux. */
export type EgsAnalyse =
  | 'DISCRIMINATION_PROBABLE'
  | 'DISCRIMINATION_POSSIBLE'
  | 'PAS_DE_DISCRIMINATION_APPARENTE';

/** Code structuré F-IA-03 d'un facteur de disparité. */
export type EgsCodeCritere =
  | 'DT56_ECART_SALAIRE'
  | 'DT56_COMPARANTS'
  | 'DT56_INDEX_EGALITE'
  | 'DT56_JUSTIFICATIONS_OBJECTIVES';

/** Facteur de disparité exposé dans la réponse API. */
export interface EgsFacteurDisparite {
  code: EgsCodeCritere;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/egalite-salariale-fh`.
 *
 * 10 champs : sexe salarié (select), salaire mensuel brut, ancienneté (mois),
 * qualification (texte), nb comparants mieux payés (entier), écart moyen (€),
 * écart en %, index égalité connu (toggle), score index (0-100), justifications
 * objectives produites par l'employeur (toggle).
 */
export interface EgaliteSalarialeFhRequest {
  sexeSalarie: EgsSexeSalarie;
  salaireMensuelBrutSalarieEuros: number;
  ancienneteMois: number;
  qualification: string;
  nombreComparantsMieuxPayes: number;
  ecartSalaireMoyenComparantsEuros: number;
  ecartPourcentage: number;
  indexEgaliteConnu: boolean;
  scoreIndexEgalite: number | null;
  justificationsEmployeurObjectives: boolean;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * facteurs de disparité F-IA-03, prescription 5 ans, alerte non-plafonnement
 * — invariant produit toujours visible, bases juridiques, messages).
 */
export interface EgaliteSalarialeFhResponse extends EgaliteSalarialeFhRequest {
  caseFileId: string;
  analyseEgaliteSalariale: EgsAnalyse;
  scoreDiscrimination: number;
  facteursDisparite: EgsFacteurDisparite[];
  prescriptionActionAns: number;
  alerteNonPlafonnement: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
