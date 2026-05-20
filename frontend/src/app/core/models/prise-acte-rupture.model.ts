/**
 * SF-206-06 : modèles TypeScript de l'outil "Prise d'acte de la rupture aux
 * torts de l'employeur" (F-DT-39 — FRANCE uniquement).
 *
 * Contrat API importé de SF-206-05 (backend, endpoints POST/GET figés).
 * Fondement : Cass. soc. 25/06/2003 n°01-42.679 ; Cass. soc. 26/03/2014
 * n°12-21.372 (critère central : manquement grave empêchant la poursuite du
 * contrat) ; Cass. soc. 26/03/2014 n°12-23.634 (grief ancien régularisé pèse
 * moins).
 */

/** Verdict 3 niveaux du scoring de prise d'acte. */
export type PriseActeRuptureVerdict =
  | 'PRISE_ACTE_FAVORABLE'
  | 'PRISE_ACTE_RISQUEE'
  | 'PRISE_ACTE_DEFAVORABLE';

/** Effet probable du jugement CPH sur la qualification de la rupture. */
export type PriseActeRuptureEffetProbable =
  | 'LICENCIEMENT_SANS_CAUSE'
  | 'LICENCIEMENT_NUL'
  | 'DEMISSION';

/** Code structuré d'un grief retenu — exposé dans la réponse API. */
export type PriseActeRuptureCodeGrief =
  | 'DT39_DEFAUT_PAIEMENT_SALAIRE'
  | 'DT39_HARCELEMENT'
  | 'DT39_MANQUEMENT_SECURITE'
  | 'DT39_MODIFICATION_UNILATERALE_CONTRAT'
  | 'DT39_DECLASSEMENT'
  | 'DT39_DISCRIMINATION'
  | 'DT39_HEURES_SUP_NON_PAYEES'
  | 'DT39_NON_RESPECT_DUREES_REPOS';

/** Grief retenu — structure exposée dans la réponse API. */
export interface PriseActeRuptureGriefRetenu {
  code: PriseActeRuptureCodeGrief;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/prise-acte-rupture`.
 *
 * 12 champs : 8 griefs (booléens) + montant impayés + 2 modulateurs + commentaire libre.
 * `montantImpayesEur` accepte null (pas de défaut de paiement, ou montant non chiffrable).
 */
export interface PriseActeRuptureRequest {
  defautPaiementSalaire: boolean;
  montantImpayesEur: number | null;
  harcelement: boolean;
  manquementSecurite: boolean;
  modificationUnilateraleContrat: boolean;
  declassement: boolean;
  discrimination: boolean;
  heuresSupNonPayees: boolean;
  nonRespectDureesRepos: boolean;
  griefsActuelsEtPersistants: boolean;
  griefRendImpossiblePoursuite: boolean;
  griefsCommentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose le snapshot des 12 inputs (ré-édition possible après recharge)
 * **plus** les champs calculés (verdict, score solidité 0-100, griefs retenus,
 * effet probable, bases juridiques, messages).
 */
export interface PriseActeRuptureResponse extends PriseActeRuptureRequest {
  caseFileId: string;
  verdict: PriseActeRuptureVerdict;
  scoreSolidite: number;
  griefsRetenus: PriseActeRuptureGriefRetenu[];
  effetProbable: PriseActeRuptureEffetProbable;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
