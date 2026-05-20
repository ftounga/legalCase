/**
 * SF-206-08 : modèles TypeScript de l'outil "Résiliation judiciaire du
 * contrat de travail aux torts de l'employeur" (F-DT-40 — FRANCE uniquement).
 *
 * Contrat API importé de SF-206-07 (backend, endpoints POST/GET figés).
 *
 * Fondement :
 *  - Cass. soc. 16/03/1989 ; Cass. soc. 20/01/1998 (consécration de la voie
 *    aux torts de l'employeur).
 *  - art. L.1411-1 CT (compétence CPH) ; art. 1224, 1227-1228 C. civ.
 *  - Cass. soc. 11/01/2007 n°05-40.626 (effets à la date du jugement).
 *  - Cass. soc. 21/12/2006 n°05-42.251 (effets reportés à la date du
 *    licenciement intervenu en cours d'instance).
 *  - Cass. soc. 30/03/2010 (manquement régularisé ou non persistant pèse moins).
 *
 * Outil jumeau de F-DT-39 (prise d'acte) — mêmes griefs, sortie distincte :
 * la résiliation judiciaire est une voie sans risque (rejet ≠ rupture, le
 * contrat se poursuit).
 */

/** Verdict 3 niveaux du scoring de résiliation judiciaire. */
export type ResiliationJudiciaireCphVerdict =
  | 'RESILIATION_FAVORABLE'
  | 'RESILIATION_INCERTAINE'
  | 'RESILIATION_DEFAVORABLE';

/**
 * Date d'effet probable de la résiliation prononcée — effets licenciement
 * sans cause réelle et sérieuse, à la date :
 *  - DATE_DECISION : jugement de résiliation (cas nominal).
 *  - DATE_LICENCIEMENT : licenciement intervenu en cours d'instance.
 */
export type ResiliationJudiciaireCphDateEffetProbable =
  | 'DATE_DECISION'
  | 'DATE_LICENCIEMENT';

/** Code structuré d'un manquement retenu — exposé dans la réponse API. */
export type ResiliationJudiciaireCphCodeManquement =
  | 'DT40_DEFAUT_PAIEMENT_SALAIRE'
  | 'DT40_HARCELEMENT'
  | 'DT40_MANQUEMENT_SECURITE'
  | 'DT40_MODIFICATION_UNILATERALE_CONTRAT'
  | 'DT40_DECLASSEMENT'
  | 'DT40_DISCRIMINATION'
  | 'DT40_HEURES_SUP_NON_PAYEES'
  | 'DT40_NON_RESPECT_DUREES_REPOS';

/** Manquement retenu — structure exposée dans la réponse API. */
export interface ResiliationJudiciaireCphManquementRetenu {
  code: ResiliationJudiciaireCphCodeManquement;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/resiliation-judiciaire-cph`.
 *
 * 13 champs : 8 manquements (booléens) + montant impayés + 3 modulateurs +
 * commentaire libre. `montantImpayesEur` accepte null (pas de défaut de
 * paiement, ou montant non chiffrable).
 */
export interface ResiliationJudiciaireCphRequest {
  defautPaiementSalaire: boolean;
  montantImpayesEur: number | null;
  harcelement: boolean;
  manquementSecurite: boolean;
  modificationUnilateraleContrat: boolean;
  declassement: boolean;
  discrimination: boolean;
  heuresSupNonPayees: boolean;
  nonRespectDureesRepos: boolean;
  manquementsPersistantsAuJourDemande: boolean;
  salarieToujoursEnPoste: boolean;
  licenciementIntervenuEnCoursInstance: boolean;
  manquementsCommentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose le snapshot des 13 inputs (ré-édition possible après recharge)
 * **plus** les champs calculés (verdict, score solidité 0-100, manquements
 * retenus, date d'effet probable, bases juridiques, messages).
 */
export interface ResiliationJudiciaireCphResponse extends ResiliationJudiciaireCphRequest {
  caseFileId: string;
  verdict: ResiliationJudiciaireCphVerdict;
  scoreSolidite: number;
  manquementsRetenus: ResiliationJudiciaireCphManquementRetenu[];
  dateEffetProbable: ResiliationJudiciaireCphDateEffetProbable;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
