/**
 * SF-DT-29-02 : types frontend pour l'outil décisionnel "Crédit-temps /
 * interruption de carrière BE" (F-DT-29). Régimes CCT 103 (avec / sans
 * motif) + AR 29/10/1997 (fin de carrière). BE uniquement.
 *
 * Contrat figé dans SF-DT-29-01 (backend, autre worktree).
 */

/** Régime de crédit-temps (3 régimes V1). */
export type CreditTempsRegime = 'AVEC_MOTIF' | 'SANS_MOTIF' | 'FIN_CARRIERE';

/**
 * Motif requis si régime = AVEC_MOTIF (CCT 103). 4 motifs canoniques.
 * Optionnel si régime = SANS_MOTIF ou FIN_CARRIERE.
 */
export type CreditTempsMotif =
  | 'SOINS_ENFANT_LT_8_ANS'
  | 'SOINS_PARENT_MALADE'
  | 'FORMATION'
  | 'AUTRE';

/** Type de réduction du temps de travail (CCT 103). */
export type DureeReductionType = 'CINQUIEME' | 'MOITIE' | 'TEMPS_PLEIN';

/** Verdict d'éligibilité (scoring niveau 5). */
export type CreditTempsVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellé humain pour mat-select régime. */
export const REGIME_LABELS: ReadonlyArray<{ code: CreditTempsRegime; label: string }> = [
  { code: 'AVEC_MOTIF', label: 'Avec motif (CCT 103 — 4 motifs reconnus)' },
  { code: 'SANS_MOTIF', label: 'Sans motif (CCT 103 — quota employeur)' },
  { code: 'FIN_CARRIERE', label: 'Fin de carrière (AR 29/10/1997)' },
];

/** Libellé humain pour mat-select motif (régime AVEC_MOTIF). */
export const MOTIF_LABELS: ReadonlyArray<{ code: CreditTempsMotif; label: string }> = [
  { code: 'SOINS_ENFANT_LT_8_ANS', label: 'Soins à un enfant < 8 ans' },
  { code: 'SOINS_PARENT_MALADE', label: 'Soins à un parent gravement malade' },
  { code: 'FORMATION', label: 'Formation reconnue' },
  { code: 'AUTRE', label: 'Autre motif (à justifier)' },
];

/** Libellé humain pour mat-select durée de réduction. */
export const DUREE_REDUCTION_LABELS: ReadonlyArray<{ code: DureeReductionType; label: string }> = [
  { code: 'CINQUIEME', label: '1/5 temps (réduction 1 jour/semaine)' },
  { code: 'MOITIE', label: 'Mi-temps (50 %)' },
  { code: 'TEMPS_PLEIN', label: 'Temps plein (100 % suspension)' },
];

export interface CreditTempsBeRequest {
  regime: CreditTempsRegime;
  motif?: CreditTempsMotif;
  /** Ancienneté dans l'entreprise (mois entiers ≥ 0). */
  ancienneteEntrepriseMois: number;
  /** Taille de l'entreprise en équivalents temps plein (≥ 0). */
  tailleEntrepriseEtp: number;
  dureeReductionType: DureeReductionType;
  /** Âge du demandeur en années entières (0..75). */
  ageDemandeurAnnees: number;
  /** Date de la demande (YYYY-MM-DD). */
  dateDemande: string;
}

export interface CreditTempsBeResponse {
  caseFileId: string;
  regime: CreditTempsRegime;
  eligible: boolean;
  /** Score global 0..100. */
  scoreGlobal: number;
  verdictEligibilite: CreditTempsVerdict;
  /** Liste des critères non remplis (chaînes textuelles). */
  criteresNonRemplis: string[];
  /** Indemnité mensuelle ONEM (€). */
  indemniteOnemMensuelle: number;
  /** Durée maximale autorisée (mois). */
  dureeMaximaleMois: number;
  /** Base juridique (texte JetBrains Mono italique). */
  baseJuridique: string;
  /** Formule (texte JetBrains Mono). */
  formule: string;
  /** Messages explicatifs (rendus via LegalCitationsPipe). */
  messages: string[];
}
