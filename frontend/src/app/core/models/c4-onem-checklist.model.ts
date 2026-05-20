/**
 * SF-207-02b : modèle frontend de l'outil F-207 — Checklist C4 ONEM Travail BE
 * (Belgique uniquement, AR 25/11/1991 art. 92 / art. 144 + Loi 03/07/1978).
 * Contrat figé dans SF-207-02 backend (PR #1123).
 */

/**
 * Verdict global de conformité du C4 ONEM.
 * Priorité : RISQUE_EXCLUSION_FAUTE_GRAVE domine NON_CONFORME qui domine CONFORME.
 */
export type C4OnemVerdict = 'CONFORME' | 'NON_CONFORME' | 'RISQUE_EXCLUSION_FAUTE_GRAVE';

/** Orientation procédurale recommandée à l'avocat selon le verdict. */
export type C4OnemEtapeSuivante =
  | 'CONTESTATION_C4'
  | 'RECTIFICATION_AUPRES_EMPLOYEUR'
  | 'AUCUNE';

/**
 * Liste des 10 mentions obligatoires du document C4 ONEM
 * (AR du 25 novembre 1991 art. 92).
 */
export type C4OnemMention =
  | 'RAISON_SOCIALE_EMPLOYEUR'
  | 'NUMERO_BCE'
  | 'NOM_SALARIE'
  | 'DATE_ENTREE_SERVICE'
  | 'DATE_SORTIE_SERVICE'
  | 'CATEGORIE_ONEM'
  | 'MOTIF_EXPLICITE'
  | 'PERIODE_OCCUPATION_COHERENTE'
  | 'PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE'
  | 'DERNIER_SALAIRE_INDIQUE';

export interface C4OnemExclusionRange {
  minSemaines: number;
  maxSemaines: number;
}

export interface C4OnemChecklistRequest {
  raisonSocialeEmployeur?: string | null;
  /** 10 chiffres BCE si fourni. */
  numeroBce?: string | null;
  /** Requis. */
  nomSalarie: string;
  numeroNationalRegistre?: string | null;
  /** Requis, ISO YYYY-MM-DD. */
  dateEntreeService: string;
  /** Requis, ISO YYYY-MM-DD, ≥ dateEntreeService. */
  dateSortieService: string;
  categorieOnem?: string | null;
  motifExplicite?: string | null;
  /** Requis (boolean). */
  fauteGraveMentionnee: boolean;
  preavisPresteJours?: number | null;
  dernierSalaireMensuelBrut?: number | null;
}

export interface C4OnemChecklistResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  raisonSocialeEmployeur: string | null;
  numeroBce: string | null;
  nomSalarie: string;
  numeroNationalRegistre: string | null;
  dateEntreeService: string;
  dateSortieService: string;
  categorieOnem: string | null;
  motifExplicite: string | null;
  fauteGraveMentionnee: boolean;
  preavisPresteJours: number | null;
  dernierSalaireMensuelBrut: number | null;
  // Outputs calculés
  verdict: C4OnemVerdict;
  mentionsManquantes: C4OnemMention[];
  fauteGraveDetectee: boolean;
  exclusionOnemRange: C4OnemExclusionRange | null;
  lettreRectificativeProposee: string | null;
  baseJuridique: string;
  etapeSuivante: C4OnemEtapeSuivante;
  country: 'BELGIQUE';
}

/**
 * Libellés humains FR des mentions C4 — alignés sur les libellés backend
 * (`C4OnemChecklistCalculator.LIBELLES_MENTIONS`) pour cohérence UI / lettre
 * rectificative générée serveur.
 */
const MENTION_LABELS: Record<C4OnemMention, string> = {
  RAISON_SOCIALE_EMPLOYEUR: 'Raison sociale de l\'employeur',
  NUMERO_BCE: 'Numéro BCE (10 chiffres)',
  NOM_SALARIE: 'Nom du salarié',
  DATE_ENTREE_SERVICE: 'Date d\'entrée en service',
  DATE_SORTIE_SERVICE: 'Date de sortie de service',
  CATEGORIE_ONEM: 'Catégorie ONEM',
  MOTIF_EXPLICITE: 'Motif explicite de la fin de contrat (≥ 5 caractères)',
  PERIODE_OCCUPATION_COHERENTE: 'Cohérence de la période d\'occupation (sortie ≥ entrée)',
  PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE: 'Préavis presté (en jours) — obligatoire en l\'absence de faute grave',
  DERNIER_SALAIRE_INDIQUE: 'Dernier salaire mensuel brut',
};

/** Libellé humain FR pour une mention C4 (utilisé dans la liste UI). */
export function humanizeMention(m: C4OnemMention): string {
  return MENTION_LABELS[m] ?? m;
}
