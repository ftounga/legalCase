/**
 * SF-207-08b : modèle frontend de l'outil F-207 — Outplacement BE
 * obligatoire 45+ (Belgique uniquement, CCT n°82 ; CCT n°82 bis ; Loi du
 * 5 septembre 2001 art. 13 ; AR du 30 mai 2018 ; AR du 25 novembre 1991
 * art. 154).
 *
 * <p>Contrat figé dans SF-207-08 backend (PR #1160).</p>
 */

/** Motifs de rupture conditionnant l'obligation d'outplacement employeur. */
export type OutplacementBeMotifLicenciement =
  | 'LICENCIEMENT_ECONOMIQUE'
  | 'LICENCIEMENT_AUTRE'
  | 'FAUTE_GRAVE'
  | 'DEMISSION';

/**
 * 5 verdicts conditionnels (priorité dans l'ordre indiqué) :
 *   1. {@link OUTPLACEMENT_NON_DU} — exemption (âge, ancienneté, faute grave,
 *      démission). Information uniquement (vert).
 *   2. {@link OFFRE_NON_FAITE_SANCTION_EMPLOYEUR} — obligation applicable
 *      mais aucune offre reçue (sanction employeur 1 800 € — rouge).
 *   3. {@link OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR} — offre reçue mais non
 *      conforme CCT 82 ou hors délai 15 j (sanction employeur 1 800 € — rouge).
 *   4. {@link OFFRE_REFUSEE_SANCTION_SALARIE} — offre conforme refusée par
 *      le salarié (sanction salarié 4-52 sem. — ambre).
 *   5. {@link OFFRE_CONFORME_RESPECTEE} — offre conforme acceptée ou
 *      indécise (conforme — vert).
 */
export type OutplacementBeVerdict =
  | 'OUTPLACEMENT_NON_DU'
  | 'OFFRE_CONFORME_RESPECTEE'
  | 'OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR'
  | 'OFFRE_NON_FAITE_SANCTION_EMPLOYEUR'
  | 'OFFRE_REFUSEE_SANCTION_SALARIE';

/**
 * Raison d'absence d'obligation employeur — émise UNIQUEMENT pour le verdict
 * {@link OutplacementBeVerdict#OUTPLACEMENT_NON_DU}.
 */
export type OutplacementBeRaisonNonObligation =
  | 'AGE_INFERIEUR_45'
  | 'ANCIENNETE_INFERIEURE_1AN'
  | 'FAUTE_GRAVE'
  | 'DEMISSION';

/** Orientation procédurale recommandée à l'avocat selon le verdict. */
export type OutplacementBeEtapeSuivante =
  | 'PLAINTE_INSPECTION_LOIS_SOCIALES'
  | 'PRESENTATION_C4_ONEM'
  | 'AUCUNE';

/**
 * Fourchette d'exclusion ONEM pour le salarié refusant une offre
 * d'outplacement conforme — 4 à 52 semaines (AR 25/11/1991 art. 154).
 */
export interface OutplacementBeSanctionSalarieRange {
  minSemaines: number;
  maxSemaines: number;
}

export interface OutplacementBeRequest {
  /** Requis, ISO YYYY-MM-DD. */
  dateLicenciement: string;
  /** Requis, ISO YYYY-MM-DD. */
  dateNaissanceSalarie: string;
  /** Requis, ≥ 0 — ancienneté en années avec décimales. */
  ancienneteAnnees: number;
  /** Requis — enum 4 valeurs. */
  motifLicenciement: OutplacementBeMotifLicenciement;
  /** Requis — true = CCT 82 ; false = CCT 82 bis. */
  contratTempsPlein: boolean;
  /** Requis. */
  offreOutplacementRecue: boolean;
  /** Requis SI offreOutplacementRecue=true. ISO YYYY-MM-DD. */
  dateOffreOutplacement?: string | null;
  /** Requis SI offreOutplacementRecue=true. */
  offreConformeCCT82?: boolean | null;
  /** Optionnel — null = pas encore décidé (verdict OFFRE_CONFORME_RESPECTEE). */
  salarieAcceptantOffre?: boolean | null;
}

export interface OutplacementBeResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  dateLicenciement: string;
  dateNaissanceSalarie: string;
  ancienneteAnnees: number;
  motifLicenciement: OutplacementBeMotifLicenciement;
  contratTempsPlein: boolean;
  offreOutplacementRecue: boolean;
  dateOffreOutplacement: string | null;
  offreConformeCCT82: boolean | null;
  salarieAcceptantOffre: boolean | null;
  // Outputs calculés
  verdict: OutplacementBeVerdict;
  ageALaDateLicenciement: number;
  obligationEmployeurApplicable: boolean;
  raisonNonObligation: OutplacementBeRaisonNonObligation | null;
  /** Sanction employeur (1 800 € si verdict d'infraction employeur, sinon 0 €). */
  sanctionEmployeurEuros: number;
  /** Fourchette d'exclusion ONEM si refus offre conforme par salarié. */
  sanctionSalarieRange: OutplacementBeSanctionSalarieRange | null;
  /** dateLicenciement + 15 jours calendaires (fuseau Europe/Brussels). */
  dateLimiteOffre: string | null;
  /** true si dateOffre ≤ dateLimiteOffre, null si aucune offre. */
  delaiOffreRespecte: boolean | null;
  etapeSuivante: OutplacementBeEtapeSuivante;
  baseJuridique: string;
  formuleCalcul: string;
  country: 'BELGIQUE';
}

/**
 * Humanise une raison d'absence d'obligation pour l'affichage avocat.
 * Alignée avec le tableau de la mini-spec SF-207-08b.
 */
export function humanizeRaisonNonObligation(
  r: OutplacementBeRaisonNonObligation | null | undefined,
): string {
  switch (r) {
    case 'AGE_INFERIEUR_45': return 'âge inférieur à 45 ans';
    case 'ANCIENNETE_INFERIEURE_1AN': return 'ancienneté inférieure à 1 an';
    case 'FAUTE_GRAVE': return 'faute grave (exemption employeur)';
    case 'DEMISSION': return 'démission';
    default: return '';
  }
}

/** Humanise un motif de licenciement (sélecteur du formulaire). */
export function humanizeMotifLicenciement(
  m: OutplacementBeMotifLicenciement,
): string {
  switch (m) {
    case 'LICENCIEMENT_ECONOMIQUE': return 'Licenciement économique';
    case 'LICENCIEMENT_AUTRE': return 'Autre licenciement';
    case 'FAUTE_GRAVE': return 'Faute grave';
    case 'DEMISSION': return 'Démission';
  }
}
