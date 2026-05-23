/**
 * SF-212-06 : modèles TypeScript de l'outil "Transfert d'entreprise —
 * maintien des contrats L. 1224-1" (F-DT-72 — FRANCE uniquement).
 *
 * Contrat API importé de SF-212-05 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/transfert-entreprise-l1224-1`).
 *
 * Fondement : art. L. 1224-1 CT (maintien automatique des contrats au
 * repreneur), L. 1224-3 CT (information-consultation du CSE),
 * Directive 2001/23/CE, Cass. soc. 18/07/2000 n°98-46.071 (notion
 * d'entité économique autonome), Cass. soc. — licenciements pré-transfert
 * frauduleux nuls.
 */

/** Type de transfert envisagé / réalisé — 6 valeurs. */
export type TransfertEntrepriseFrTypeTransfert =
  | 'CESSION'
  | 'FUSION'
  | 'APPORT_PARTIEL_ACTIF'
  | 'EXTERNALISATION'
  | 'REPRISE_ACTIVITE'
  | 'AUTRE';

/** Verdict d'applicabilité de L. 1224-1 — 3 niveaux. */
export type TransfertEntrepriseFrVerdict =
  | 'L1224_APPLICABLE'
  | 'L1224_INAPPLICABLE'
  | 'L1224_INCERTAIN';

/** Code structuré d'un point d'analyse détecté. */
export type TransfertEntrepriseFrCodePoint =
  | 'DT72_EEA_IDENTIFIEE'
  | 'DT72_ACTIVITE_PRESERVEE'
  | 'DT72_LICENCIEMENTS_PRE_TRANSFERT'
  | 'DT72_CONSULTATION_CSE';

/** Point d'analyse — structure exposée dans la réponse API. */
export interface TransfertEntrepriseFrPointAnalyse {
  code: TransfertEntrepriseFrCodePoint;
  libelle: string;
  fondement: string;
  conclusion: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/transfert-entreprise-l1224-1`.
 *
 * 9 champs : type de transfert (enum), 6 toggles (EEA, activité, salariés,
 * contrats modifiés, licenciements pré-transfert, CSE), nb licenciements,
 * date du transfert.
 */
export interface TransfertEntrepriseFrRequest {
  typeTransfert: TransfertEntrepriseFrTypeTransfert;
  eeaIdentifieeAvantTransfert: boolean | null;
  activiteEconomiquePreservee: boolean | null;
  salariesTransferes: boolean | null;
  contratsModifiesParRepreneur: boolean | null;
  licenciementsPreTransfert: boolean | null;
  nbLicenciementsPreTransfert: number;
  informationConsultationCseRealisee: boolean | null;
  dateTransfert: string | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * points d'analyse, alertes, bases juridiques, messages).
 */
export interface TransfertEntrepriseFrResponse extends TransfertEntrepriseFrRequest {
  caseFileId: string;
  analyseL1224_1: TransfertEntrepriseFrVerdict;
  scoreApplicabilite: number;
  pointsAnalyse: TransfertEntrepriseFrPointAnalyse[];
  alerteLicenciementsFrauduleux: boolean;
  alerteDefautConsultationCse: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
