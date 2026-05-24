/**
 * SF-212-36 : modèles TypeScript pour l'outil décisionnel "PDV / RCC —
 * conformité" (F-DT-46-pdv-rcc-conformite, FR — L. 1237-17 à L. 1237-19-14
 * CT — RCC instituée par l'ord. n°2017-1387 du 22/09/2017 ; décret 2017-1718
 * du 20/12/2017 ; circulaire DGT du 19/09/2018 ; L. 1237-19-1 al. 5 CT —
 * indemnités au moins égales à l'indemnité légale de licenciement ;
 * L. 1237-19-3 CT — validation DREETS dans 15 jours).
 *
 * Contrat API importé de SF-212-35 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/pdv-rcc-conformite`).
 */

/** Type de dispositif — RCC ou PDV. */
export type PdvRccTypeDispositif = 'RCC' | 'PDV';

/** Verdict d'analyse de la conformité — 3 niveaux. */
export type PdvRccAnalyse =
  | 'CONFORME'
  | 'PARTIELLEMENT_CONFORME'
  | 'NON_CONFORME';

/** Code structuré F-IA-03 d'un point d'irrégularité. */
export type PdvRccCodeCritere =
  | 'DT46_ACCORD_MAJORITAIRE'
  | 'DT46_VALIDATION_DREETS'
  | 'DT46_INDEMNITEES'
  | 'DT46_ADHESION_VOLONTAIRE';

/** Point d'irrégularité exposé dans la réponse API. */
export interface PdvRccPointIrregularite {
  code: PdvRccCodeCritere;
  libelle: string;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/pdv-rcc-conformite`.
 *
 * 7 champs : type dispositif (select RCC/PDV), 6 toggles
 * (accord majoritaire, validation DREETS, délai 15 j, indemnités ≥ légales,
 * adhésion volontaire, information complète).
 */
export interface PdvRccConformiteRequest {
  typeDispositif: PdvRccTypeDispositif;
  accordCollectifMajoritaire: boolean;
  validationDREETSObtenue: boolean;
  delaiValidation15JRespectee: boolean | null;
  indemnitesAuMoinsLegales: boolean;
  adhesionVolontaireIndividuelle: boolean;
  informationIndividuelleComplete: boolean;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * points d'irrégularité F-IA-03, droit ARE confirmé si RCC conforme, bases
 * juridiques, messages).
 */
export interface PdvRccConformiteResponse extends PdvRccConformiteRequest {
  caseFileId: string;
  analysePdvRcc: PdvRccAnalyse;
  scoreConformite: number;
  pointsIrregularite: PdvRccPointIrregularite[];
  droitAreConfirme: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
