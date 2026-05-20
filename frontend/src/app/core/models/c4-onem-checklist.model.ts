/**
 * SF-207-04 (frontend) : modèle de l'outil F-207 — C4 ONEM checklist conformité
 * (Belgique uniquement, AR 25/11/1991 portant réglementation du chômage
 * art. 92 — 10 mentions obligatoires ; art. 144 — exclusion ONEM 4-52 sem.
 * en cas de mention « faute grave »).
 * Contrat figé dans SF-207-02-backend (PR #1123).
 */

/**
 * 10 mentions obligatoires vérifiées par {@code C4OnemChecklistCalculator}.
 * Le NRN (numéro national de registre) n'est PAS vérifié (peut être masqué RGPD).
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

/**
 * Verdict global de conformité du C4 ONEM.
 * Priorité : RISQUE_EXCLUSION_FAUTE_GRAVE > NON_CONFORME > CONFORME.
 */
export type C4OnemVerdict =
  | 'CONFORME'
  | 'NON_CONFORME'
  | 'RISQUE_EXCLUSION_FAUTE_GRAVE';

/** Orientation procédurale recommandée selon le verdict. */
export type C4OnemEtapeSuivante =
  | 'CONTESTATION_C4'
  | 'RECTIFICATION_AUPRES_EMPLOYEUR'
  | 'AUCUNE';

/**
 * Fourchette d'exclusion ONEM (art. 144 AR 25/11/1991) — 4 à 52 semaines
 * de privation des allocations en cas de licenciement pour faute grave
 * reconnu par l'ONEM.
 */
export interface C4OnemExclusionRange {
  minSemaines: number;
  maxSemaines: number;
}

export interface C4OnemChecklistRequest {
  raisonSocialeEmployeur?: string | null;
  numeroBce?: string | null;
  nomSalarie: string;
  numeroNationalRegistre?: string | null;
  dateEntreeService: string; // ISO YYYY-MM-DD
  dateSortieService: string; // ISO YYYY-MM-DD
  categorieOnem?: string | null;
  motifExplicite?: string | null;
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
 * Libellés FR avocat des 10 mentions (affichés en checkbox + listes
 * « mentions manquantes »). Référence : AR 25/11/1991 art. 92.
 */
export const C4_ONEM_MENTION_LABELS: Record<C4OnemMention, string> = {
  RAISON_SOCIALE_EMPLOYEUR: 'Raison sociale de l\'employeur',
  NUMERO_BCE: 'Numéro BCE (10 chiffres)',
  NOM_SALARIE: 'Nom du salarié',
  DATE_ENTREE_SERVICE: 'Date d\'entrée en service',
  DATE_SORTIE_SERVICE: 'Date de sortie de service',
  CATEGORIE_ONEM: 'Catégorie / code ONEM',
  MOTIF_EXPLICITE: 'Motif explicite de la fin de contrat',
  PERIODE_OCCUPATION_COHERENTE: 'Période d\'occupation cohérente',
  PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE:
    'Préavis presté précisé (si pas faute grave)',
  DERNIER_SALAIRE_INDIQUE: 'Dernier salaire mensuel brut indiqué',
};
