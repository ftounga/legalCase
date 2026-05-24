/**
 * SF-212-14 : modèles TypeScript de l'outil "Mutation — validité de la clause
 * de mobilité" (F-DT-71-mutation-clause-mobilite, FRANCE uniquement —
 * jurisprudence Cass. soc. constante).
 *
 * Contrat API importé de SF-212-13 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/mutation-clause-mobilite`).
 */

/** Verdict d'analyse de la validité de la clause de mobilité — 3 niveaux. */
export type MutationAnalyse =
  | 'CLAUSE_VALIDE'
  | 'CLAUSE_INVALIDE'
  | 'ABSENCE_CLAUSE';

/** Réponse du salarié à la mutation. */
export type MutationReponseSalarie =
  | 'REFUS'
  | 'ACCEPTATION'
  | 'EN_ATTENTE';

/** Code structuré F-IA-03 d'un critère analysé. */
export type MutationCodeCritere =
  | 'DT71_CLAUSE_PRESENTE'
  | 'DT71_ZONE_PRECISE'
  | 'DT71_DELAI_PREVENANCE'
  | 'DT71_MOTIF_PROFESSIONNEL';

/** Type de conséquence du refus de mutation. */
export type MutationTypeConsequence =
  | 'REFUS_FAUTE_LICENCIEMENT_POSSIBLE'
  | 'REFUS_SANS_FAUTE_CLAUSE_INVALIDE'
  | 'REFUS_SANS_FAUTE_ABSENCE_CLAUSE'
  | 'MODIFICATION_CONTRAT_REQUISE'
  | 'DELAI_PREVENANCE_INSUFFISANT'
  | 'ATTENTE_REPONSE_SALARIE'
  | 'ACCORD_FORMEL_REQUIS';

/** Point d'analyse exposé dans la réponse API. */
export interface MutationPointAnalyse {
  code: MutationCodeCritere;
  libelle: string;
  fondement: string;
  conclusion: string;
}

/** Conséquence du refus exposée dans la réponse API. */
export interface MutationConsequence {
  type: MutationTypeConsequence;
  description: string;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/mutation-clause-mobilite`.
 *
 * 7 champs : clause présente (toggle), zone précise (toggle), intérêt légitime
 * (toggle), délai de prévenance en semaines (entier nullable), situation
 * familiale contraignante (toggle), motif professionnel (toggle), réponse
 * du salarié (select).
 */
export interface MutationClauseMobiliteRequest {
  clauseMobilitePresente: boolean;
  zoneGeographiquePrecise: boolean;
  interetLegitimeEmployeur: boolean;
  delaiPrevenanceSemaines: number | null;
  situationFamilialeSalarieContraignante: boolean;
  motifMutationProfessionnel: boolean;
  reponseSalarie: MutationReponseSalarie;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * points d'analyse F-IA-03, conséquences, bases juridiques, messages).
 */
export interface MutationClauseMobiliteResponse extends MutationClauseMobiliteRequest {
  caseFileId: string;
  analyseMutation: MutationAnalyse;
  scoreMutation: number;
  pointsAnalyse: MutationPointAnalyse[];
  consequences: MutationConsequence[];
  consequencesRefus: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
