/**
 * SF-218-04 : modèles miroirs du contrat API (backend SF-218-03) pour l'outil
 * décisionnel « Exécution du jugement CPH / AGS » (F-DT-88-execution-jugement-cph).
 * FRANCE uniquement — exécution forcée d'un jugement du Conseil de prud'hommes
 * (art. 514 CPC ; R. 1454-28 CPC) et relais de la garantie AGS lorsque l'employeur
 * est en redressement ou en liquidation judiciaire (L. 3253-6 à L. 3253-21 Code travail).
 *
 * Checklist d'exécution forcée (signification, exécution provisoire de droit des
 * créances salariales, commandement de payer, mandatement huissier, mesures
 * conservatoires) + détecteur AGS (plafonds, déclaration de créance au mandataire,
 * saisine CGEA) + verdict d'orientation (3 états : exécution directe vs relais AGS
 * vs information manquante).
 */

/**
 * Situation de l'employeur condamné (aligné EXACTEMENT sur l'enum backend
 * {@code SituationEmployeur}) :
 *  - IN_BONIS : employeur in bonis → exécution directe contre l'employeur.
 *  - REDRESSEMENT : redressement judiciaire ouvert → relais AGS.
 *  - LIQUIDATION : liquidation judiciaire ouverte → relais AGS.
 */
export type ExecutionJugementCphSituationEmployeur =
  | 'IN_BONIS'
  | 'REDRESSEMENT'
  | 'LIQUIDATION';

/**
 * Verdict d'orientation de l'exécution (aligné EXACTEMENT sur l'enum backend
 * {@code ExecutionJugementCphVerdict}) :
 *  - EXECUTION_DIRECTE : employeur in bonis — exécution forcée de droit commun.
 *  - RELAIS_AGS : employeur en procédure collective — relais garantie AGS / CGEA.
 *  - BLOQUE_INFO_MANQUANTE : procédure collective sans date d'ouverture — info à compléter.
 */
export type ExecutionJugementCphVerdict =
  | 'EXECUTION_DIRECTE'
  | 'RELAIS_AGS'
  | 'BLOQUE_INFO_MANQUANTE';

/** Item de la checklist d'exécution — miroir de ExecutionJugementCphChecklistItem. */
export interface ExecutionJugementCphChecklistItem {
  libelle: string;
  obligatoire: boolean;
  bloquant: boolean;
  baseJuridique: string;
}

/**
 * Bloc AGS — plafonds de la garantie et démarches associées (miroir de
 * ExecutionJugementCphAgsInfo). Présent uniquement quand `agsEligible = true`.
 * Les plafonds reposent sur le plafond mensuel SS (constante backend à actualiser
 * annuellement) — la mention « barème à actualiser annuellement » est rendue côté UI.
 */
export interface ExecutionJugementCphAgsInfo {
  plafondMensuelSs: number; // plafond mensuel de la sécurité sociale (€) — base du barème
  plafondGarantie: number; // plafond de la garantie AGS applicable (€)
  coefficientPlafond: number; // 6 / 5 / 4 selon l'ancienneté du contrat
  relaisAgsRecommande: boolean;
  demarches: ExecutionJugementCphChecklistItem[]; // déclaration de créance, saisine CGEA…
}

export interface ExecutionJugementCphRequest {
  dateJugement: string; // YYYY-MM-DD — requis
  montantCondamnation: number; // > 0 — requis (total des condamnations en faveur du salarié)
  executionProvisoireOrdonnee: boolean; // exécution provisoire de droit des créances salariales (R. 1454-28)
  situationEmployeur: ExecutionJugementCphSituationEmployeur; // IN_BONIS | REDRESSEMENT | LIQUIDATION
  dateOuvertureProcedureCollective: string | null; // YYYY-MM-DD — requis si REDRESSEMENT/LIQUIDATION
  creancesSuperPrivilegiees: number | null; // 60 derniers jours de salaire (L. 3253-8) — optionnel
}

export interface ExecutionJugementCphResponse {
  caseFileId: string;
  dateJugement: string;
  montantCondamnation: number;
  executionProvisoireOrdonnee: boolean;
  situationEmployeur: ExecutionJugementCphSituationEmployeur;
  dateOuvertureProcedureCollective: string | null;
  creancesSuperPrivilegiees: number | null;
  verdict: ExecutionJugementCphVerdict;
  agsEligible: boolean;
  agsInfo: ExecutionJugementCphAgsInfo | null; // présent ssi agsEligible
  checklist: ExecutionJugementCphChecklistItem[];
  country: string;
  baseJuridique: string;
}
