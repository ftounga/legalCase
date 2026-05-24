/**
 * SF-212-12 : modèles TypeScript de l'outil "Modification du contrat —
 * refus du salarié" (F-DT-70, FRANCE uniquement — Cass. soc. ;
 * L. 1222-6 CT).
 *
 * Contrat API importé de SF-212-11 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/modification-contrat-refus`).
 */

/** Verdict de qualification de la mesure — 3 niveaux. */
export type ModificationContratAnalyse =
  | 'MODIFICATION_CONTRAT'
  | 'CHANGEMENT_CONDITIONS_TRAVAIL'
  | 'INCERTAIN';

/** Élément du contrat objet de la modification. */
export type ModificationContratElementModifie =
  | 'REMUNERATION'
  | 'QUALIFICATION'
  | 'DUREE_TRAVAIL'
  | 'LIEU_TRAVAIL'
  | 'HORAIRES'
  | 'TACHES'
  | 'AUTRE';

/** Réponse du salarié à la proposition de modification. */
export type ModificationContratReponseSalarie =
  | 'REFUS'
  | 'ACCEPTATION'
  | 'EN_ATTENTE';

/** Type de conséquence du refus. */
export type ModificationContratTypeConsequence =
  | 'EMPLOYEUR_DOIT_RENONCER_OU_LICENCIER'
  | 'LICENCIEMENT_SANS_CAUSE_REELLE_SERIEUSE_SI_ILLEGITIME'
  | 'LICENCIEMENT_ECONOMIQUE_L1233_3'
  | 'PROCEDURE_L1222_6_NON_RESPECTEE'
  | 'ATTENTE_REPONSE_SALARIE'
  | 'ACCORD_FORMEL_REQUIS'
  | 'POUVOIR_DIRECTION_REFUS_FAUTIF';

/** Conséquence du refus exposée dans la réponse API. */
export interface ModificationContratConsequence {
  type: ModificationContratTypeConsequence;
  description: string;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/modification-contrat-refus`.
 *
 * 6 champs : élément modifié (select), élément contractualisé (toggle),
 * motif économique (toggle), notification L. 1222-6 (toggle nullable),
 * délai de réflexion en mois (entier nullable), réponse du salarié (select).
 */
export interface ModificationContratRefusRequest {
  elementModifie: ModificationContratElementModifie;
  elementExplicitementContractualise: boolean;
  motifEconomique: boolean;
  notificationEcriteL1222_6: boolean | null;
  delaiReflexionRespecteMois: number | null;
  reponseSalarie: ModificationContratReponseSalarie;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * conséquences, alerte L. 1222-6, bases juridiques, messages).
 */
export interface ModificationContratRefusResponse extends ModificationContratRefusRequest {
  caseFileId: string;
  analyseModification: ModificationContratAnalyse;
  scoreModificationContrat: number;
  consequences: ModificationContratConsequence[];
  alerteDelaiReflexion: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
