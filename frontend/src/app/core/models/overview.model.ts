/**
 * F-289 / SF-289-01 — Modèle TS du contrat d'agrégation « Vue d'ensemble du dossier ».
 *
 * Reflète exactement la réponse `GET /api/v1/case-files/{caseFileId}/overview`
 * produite par `OverviewService` côté backend (agrégation lecture seule, fail-open).
 * Le frontend consomme ce contrat tel quel — aucune transformation métier ici.
 */

/** Partie dont c'est le tour dans le cycle contradictoire. */
export type OverviewParty = 'OURS' | 'ADVERSE';

/** Acteur d'un événement du fil. `null` = système / non attribué. */
export type OverviewActeur = 'OURS' | 'ADVERSE' | 'SYSTEME' | null;

/** Voie (catégorie) d'un événement du fil. */
export type OverviewVoie = 'PROCEDURE' | 'ECHANGE' | 'PIECES' | 'PRODUCTION' | 'ECHEANCE';

/** Position temporelle d'un événement par rapport au repère « Aujourd'hui ». */
export type OverviewTemps = 'PASSE' | 'AUJOURDHUI' | 'FUTUR';

/** Type d'item du bloc « ce qui requiert ton attention ». */
export type AttentionType = 'ECHEANCE' | 'PIECE_MANQUANTE' | 'QUESTION_IA' | 'ANALYSE_OBSOLETE';

/**
 * Nature de l'action portée par un item d'attention ou un événement du fil.
 * Les actions « lourdes » (réponse IA, arbitrage) routent vers l'écran dédié (V1).
 */
export type OverviewActionKind =
  | 'NAVIGATE'
  | 'GENERATE_REPLY'
  | 'RELAUNCH_ANALYSIS'
  | 'VALIDATE_DEADLINE'
  | 'OPEN_DOC'
  | 'ANSWER_QUESTION'
  | 'MARK_PIECE';

/** Onglet cible d'une action de routage interne au détail dossier. */
export type OverviewTargetTab = 'dossier' | 'analyse' | 'decision' | 'suivi';

/** Action déclenchable depuis un item d'attention ou un événement du fil. */
export interface OverviewAction {
  kind: OverviewActionKind;
  /** Onglet du détail dossier vers lequel router (mécanisme `selectedTabIndex`). */
  targetTab?: OverviewTargetTab | null;
  /** Ancre à scroller dans l'onglet cible (`section-*`). */
  anchor?: string | null;
  /** Route absolue alternative (page dédiée, ex. /conclusions). */
  route?: string | null;
}

/** Prochaine échéance datée (le « couperet »). */
export interface OverviewDeadline {
  label: string;
  dueDate: string;
  /** Jours restants (négatif = dépassé). */
  daysUntil: number;
  urgency: string;
}

/** Indicateurs de santé du dossier. */
export interface OverviewSante {
  admissibility: string | null;
  prescriptionStatus: string | null;
  riskLevelAvocat: string | null;
}

/** Registre ① — état d'ensemble du dossier. */
export interface OverviewPilotage {
  currentPhaseLabel: string | null;
  awaitingParty: OverviewParty | null;
  nextDeadline: OverviewDeadline | null;
  sante: OverviewSante;
  analysisStale: boolean;
  pendingPiecesCount: number;
  toolsCalculated: number;
  toolsVisible: number;
}

/** Item du bloc « ce qui requiert ton attention ». */
export interface AttentionItem {
  type: AttentionType;
  label: string;
  urgency: string;
  action: OverviewAction;
  /**
   * SF-289-02 — id de la question IA à répondre inline. Non null uniquement
   * pour un item `QUESTION_IA` portant UNE seule question sans réponse
   * (plusieurs questions agrégées → null → l'avocat est routé vers l'écran).
   */
  questionId?: string | null;
  /**
   * SF-289-02 — libellé original exact de la pièce manquante (clé du PUT
   * `/pieces-manquantes/status`). Non null pour un item `PIECE_MANQUANTE`.
   */
  pieceLibelle?: string | null;
}

/** Pièce attachée à un événement du fil (vague, round sourcé). */
export interface OverviewAttachment {
  documentId: string;
  filename: string;
}

/** Registre ② — événement du fil chronologique. */
export interface TimelineEvent {
  date: string;
  voie: OverviewVoie;
  acteur: OverviewActeur;
  titre: string;
  detail: string | null;
  temps: OverviewTemps;
  urgency: string | null;
  attachments: OverviewAttachment[];
  action: OverviewAction | null;
}

/** Réponse complète d'agrégation de la vue d'ensemble. */
export interface OverviewResponse {
  pilotage: OverviewPilotage;
  attention: AttentionItem[];
  attentionTotal: number;
  fil: TimelineEvent[];
}
