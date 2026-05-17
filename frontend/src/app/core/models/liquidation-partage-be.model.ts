/**
 * F-217 SF-217-03 : modèles TypeScript de l'outil décisionnel
 * "Liquidation-partage post-divorce (Belgique)" (`liquidation-partage-be`).
 * BELGIQUE uniquement.
 *
 * Contrat API importé de SF-217-02 (backend, endpoints POST/GET figés —
 * Code judiciaire belge art. 1207 et s. / 1218).
 */

/** Verdict d'avancement de la procédure de liquidation-partage. */
export type LiquidationPartageBeVerdict =
  | 'PROCEDURE_NON_ENGAGEE'
  | 'EN_COURS'
  | 'DELAI_CONTREDITS_CRITIQUE'
  | 'EN_ATTENTE_HOMOLOGATION'
  | 'CLOTUREE';

/** Statut d'une étape de la séquence procédurale. */
export type EtapeStatutBe = 'FAITE' | 'EN_COURS' | 'A_VENIR';

/** Statut d'un délai procédural. */
export type DelaiStatutBe = 'OK' | 'CRITIQUE' | 'DEPASSE' | 'NON_DEMARRE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/liquidation-partage-be`.
 * Les dates sont au format ISO `YYYY-MM-DD`, nullable (obligatoires seulement
 * si l'étape correspondante est `true` — contrôlé par le backend).
 */
export interface LiquidationPartageBeRequest {
  notaireDesigne: boolean;
  dateDesignationNotaire: string | null;
  operationsOuvertes: boolean;
  dateOuvertureOperations: string | null;
  inventaireEtabli: boolean;
  projetLiquidationEtabli: boolean;
  dateNotificationProjet: string | null;
  contreditsDeposes: boolean;
  procesVerbalDiresEtabli: boolean;
  homologationDemandee: boolean;
  dateHomologation: string | null;
  commentaire: string | null;
}

/** Étape positionnée par le calculateur (élément de `etapes`). */
export interface EtapeLiquidationBe {
  code: string;
  libelle: string;
  statut: EtapeStatutBe;
  ordre: number;
  fondement: string;
  explication: string;
}

/** Délai procédural calculé (élément de `delais`). */
export interface DelaiLiquidationBe {
  code: string;
  libelle: string;
  fondement: string;
  dateDepart: string | null;
  dateEcheance: string | null;
  joursRestants: number | null;
  statut: DelaiStatutBe;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `LiquidationPartageBeRequest`) pour permettre la ré-édition du formulaire
 * après rechargement, **plus** les champs calculés (verdict, checklist
 * d'étapes, délais, prochaine étape, bases juridiques).
 */
export interface LiquidationPartageBeResponse
  extends LiquidationPartageBeRequest {
  caseFileId: string;
  verdict: LiquidationPartageBeVerdict;
  etapes: EtapeLiquidationBe[];
  delais: DelaiLiquidationBe[];
  prochaineEtape: string | null;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
