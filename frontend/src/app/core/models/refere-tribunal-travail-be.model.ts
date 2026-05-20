/**
 * SF-207-05b : modèle frontend de l'outil F-207 — Référé tribunal du travail BE
 * (Belgique uniquement, Code judiciaire art. 584 — référé devant le président
 * du tribunal du travail ; CJ art. 627, 9° — compétence territoriale contrat
 * de travail ; Loi 03/07/1978 — fondement substantiel).
 *
 * Contrat figé dans SF-207-05 backend (PR #1147).
 *
 * Le calculateur évalue 5 conditions cumulatives ({@link Condition}) ; le
 * verdict ({@link Verdict}) dépend du score 0-5 :
 *   - 5 → REFERE_ELIGIBLE (squelette généré + DEPOSER_REQUETE)
 *   - 3-4 → REFERE_INCERTAIN (squelette généré + RENFORCER_DOSSIER)
 *   - 0-2 → REFERE_NON_ELIGIBLE (squelette null + ALTERNATIVE_PROCEDURE_FOND)
 */

/**
 * Verdict d'éligibilité au référé devant le président du tribunal du travail BE.
 * 3 états (cf. backend `RefereTribunalTravailBeResult.Verdict`).
 */
export type Verdict =
  | 'REFERE_ELIGIBLE'
  | 'REFERE_INCERTAIN'
  | 'REFERE_NON_ELIGIBLE';

/**
 * Orientation procédurale recommandée à l'avocat selon le verdict (cf.
 * backend `RefereTribunalTravailBeResult.EtapeSuivante`).
 */
export type EtapeSuivante =
  | 'DEPOSER_REQUETE'
  | 'RENFORCER_DOSSIER'
  | 'ALTERNATIVE_PROCEDURE_FOND';

/**
 * Motifs d'urgence type ouvrant le référé (CJ art. 584). 4 valeurs alignées
 * sur backend `RefereTribunalTravailBeMotifUrgence`. {@code AUTRE} impose une
 * description circonstanciée > 30 caractères (le calculateur ne reconnaît
 * pas l'urgence sinon).
 */
export type MotifUrgence =
  | 'HARCELEMENT'
  | 'SALAIRE_IMPAYE'
  | 'MODIFICATION_UNILATERALE'
  | 'AUTRE';

/**
 * 5 conditions cumulatives évaluées par le calculateur (cf. backend
 * `RefereTribunalTravailBeCondition`). Ordre déclaratif préservé pour
 * l'affichage UI (`conditionsNonRemplies`).
 *
 * Note : le code backend conserve la typo historique `PEUVE_URGENCE_JOINTE`
 * (devrait être `PREUVE_URGENCE_JOINTE`) — la sérialisation JSON suit la
 * casse Java exacte, on miroite la typo côté contrat TS pour préserver
 * la compatibilité réseau.
 */
export type Condition =
  | 'URGENCE_QUALIFIABLE'
  | 'PEUVE_URGENCE_JOINTE'
  | 'PERIL_EN_DEMEURE'
  | 'MESURE_PROVISOIRE_PRECISE'
  | 'COMPETENCE_IDENTIFIEE';

export interface RefereTribunalTravailBeRequest {
  /** Requis — motif d'urgence (cf. {@link MotifUrgence}). */
  motifUrgence: MotifUrgence;
  /** Requis, non vide ; ≥ 30 caractères recommandés si `motifUrgence = AUTRE`. */
  motifUrgenceDescription: string;
  /** Requis, ISO YYYY-MM-DD — date du fait générateur de l'urgence. */
  dateFaitGenerateur: string;
  /** Optionnel — date d'une démarche amiable préalable (mise en demeure, courrier). */
  dateDemarcheAmiable?: string | null;
  /** Requis (boolean) — pièce factuelle d'urgence jointe au dossier. */
  preuveUrgenceJointe: boolean;
  /** Requis, non vide ; > 10 caractères recommandés (mesure provisoire précise). */
  mesureProvisoireDemandee: string;
  /** Requis (boolean) — péril en demeure caractérisé. */
  perilEnDemeure: boolean;
  /** Requis (boolean) — juridiction territorialement compétente identifiée. */
  competenceTerritorialeIdentifiee: boolean;
}

export interface RefereTribunalTravailBeResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  motifUrgence: MotifUrgence;
  motifUrgenceDescription: string;
  dateFaitGenerateur: string;
  dateDemarcheAmiable: string | null;
  preuveUrgenceJointe: boolean;
  mesureProvisoireDemandee: string;
  perilEnDemeure: boolean;
  competenceTerritorialeIdentifiee: boolean;
  // Outputs calculés
  verdict: Verdict;
  conditionsNonRemplies: Condition[];
  /** 0-5 — nombre de conditions remplies. */
  scoreConditions: number;
  /** Texte du squelette de requête en référé ; null si verdict NON_ELIGIBLE. */
  requeteSquelette: string | null;
  baseJuridique: string;
  etapeSuivante: EtapeSuivante;
  country: 'BELGIQUE';
}

// --------------------------------------------------------------------------
// Humanisation FR — libellés UI (alignés sur les enums backend).
// --------------------------------------------------------------------------

const VERDICT_LABELS: Record<Verdict, string> = {
  REFERE_ELIGIBLE: 'Référé éligible',
  REFERE_INCERTAIN: 'Référé incertain',
  REFERE_NON_ELIGIBLE: 'Référé non éligible',
};

const CONDITION_LABELS: Record<Condition, string> = {
  URGENCE_QUALIFIABLE: 'Urgence qualifiable (motif explicite ou description circonstanciée)',
  PEUVE_URGENCE_JOINTE: 'Preuve d\'urgence jointe au dossier',
  PERIL_EN_DEMEURE: 'Péril en demeure caractérisé (préjudice imminent)',
  MESURE_PROVISOIRE_PRECISE: 'Mesure provisoire précise demandée (> 10 caractères)',
  COMPETENCE_IDENTIFIEE: 'Compétence territoriale identifiée (CJ art. 627, 9°)',
};

const MOTIF_LABELS: Record<MotifUrgence, string> = {
  HARCELEMENT: 'Harcèlement (moral ou sexuel)',
  SALAIRE_IMPAYE: 'Non-paiement de salaire / indemnité',
  MODIFICATION_UNILATERALE: 'Modification unilatérale du contrat',
  AUTRE: 'Autre motif d\'urgence',
};

const ETAPE_LABELS: Record<EtapeSuivante, string> = {
  DEPOSER_REQUETE: 'Déposer la requête au greffe du tribunal du travail compétent.',
  RENFORCER_DOSSIER: 'Renforcer le dossier (preuve d\'urgence, compétence) avant dépôt.',
  ALTERNATIVE_PROCEDURE_FOND: 'La procédure de fond est plus adaptée — basculer sur la voie ordinaire.',
};

/** Libellé humain FR du verdict (badge + en-tête résultat). */
export function humanizeVerdict(v: Verdict): string {
  return VERDICT_LABELS[v] ?? v;
}

/** Libellé humain FR d'une condition (liste « non remplies »). */
export function humanizeCondition(c: Condition): string {
  return CONDITION_LABELS[c] ?? c;
}

/** Libellé humain FR d'un motif d'urgence (select). */
export function humanizeMotif(m: MotifUrgence): string {
  return MOTIF_LABELS[m] ?? m;
}

/** Libellé humain FR de l'étape suivante recommandée (encart conclusion). */
export function humanizeEtape(e: EtapeSuivante): string {
  return ETAPE_LABELS[e] ?? e;
}
