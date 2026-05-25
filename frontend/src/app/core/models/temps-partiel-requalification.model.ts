/**
 * SF-212-34 : modèles TypeScript de l'outil "Temps partiel — requalification
 * en temps plein" (F-DT-49-temps-partiel-requalification, FRANCE uniquement —
 * L. 3123-1 à L. 3123-20 CT ; L. 3123-6 mentions obligatoires ; L. 3123-9
 * plafond heures complémentaires 1/10 ou 1/3 ; L. 3245-1 CT prescription
 * triennale rappel salaire ; Cass. soc. 22/01/1992 n°88-44.196 présomption
 * de temps complet réfragable).
 *
 * Contrat API importé de SF-212-33 (backend, endpoints POST/GET figés
 * sur `/api/v1/case-files/{caseFileId}/temps-partiel-requalification`).
 */

/** Verdict d'analyse de requalification — 3 niveaux. */
export type TempsPartielAnalyseRequalification =
  | 'REQUALIFICATION_PROBABLE'
  | 'REQUALIFICATION_POSSIBLE'
  | 'PAS_DE_REQUALIFICATION';

/** Code structuré d'un facteur d'analyse F-IA-03. */
export type TempsPartielCodeCritere =
  | 'DT49_MENTIONS_DUREE'
  | 'DT49_MENTIONS_REPARTITION'
  | 'DT49_HC_ABUSIVES'
  | 'DT49_MODIFICATION_UNILATERALE';

/** Facteur du dossier exposé dans la réponse API. */
export interface TempsPartielFacteurRequalification {
  code: TempsPartielCodeCritere;
  libelle: string;
  fondement: string;
  poids: number;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/temps-partiel-requalification`.
 *
 * 8 champs : durée contractuelle (h/sem), toggles mentions durée /
 * répartition / HC présentes, HC réelles moyennes (h/sem), modification
 * unilatérale de la répartition, ancienneté (mois), salaire mensuel
 * contractuel (€).
 */
export interface TempsPartielRequalificationRequest {
  dureeContractuelleHeures: number | null;
  mentionsDureePresentes: boolean | null;
  mentionsRepartitionPresentes: boolean | null;
  mentionsHCPresentes: boolean | null;
  heuresComplementairesReelleMoyenneHeures: number | null;
  modificationRepartitionUnilaterale: boolean | null;
  ancienneteMois: number | null;
  salaireMensuelContractuelEuros: number | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs
 * (pour ré-édition du formulaire UI) ET les sorties calculées (verdict
 * 3 niveaux, score, rappel de salaire estimé sur 3 ans, facteurs F-IA-03,
 * bases juridiques, messages).
 */
export interface TempsPartielRequalificationResponse extends TempsPartielRequalificationRequest {
  caseFileId: string;
  analyseRequalification: TempsPartielAnalyseRequalification;
  scoreRequalification: number;
  facteursRequalification: TempsPartielFacteurRequalification[];
  /** Rappel de salaire estimé sur 3 ans (L. 3245-1 CT), null si pas de requalification. */
  rappelSalaire3AnsEstimeEuros: number | null;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
