/**
 * SF-213-01b : modèle frontend de l'outil F-213 — Clause de non-concurrence BE
 * (Belgique uniquement, Loi 03/07/1978 art. 65 + CCT n°13 du 24/02/1971).
 *
 * <p>Contrat figé dans SF-213-01 backend.</p>
 *
 * <p>Régime distinct du FR (F-DT-24 — montant libre) : BE impose une
 * indemnité légale obligatoire = ½ rémunération brute × durée de la clause.</p>
 */

/** Zone géographique de la clause — 3 valeurs strictes (miroir backend enum). */
export type ClauseNonConcurrenceBeZone =
  | 'BELGIQUE_UNIQUEMENT'
  | 'BELGIQUE_ET_ETRANGER'
  | 'NON_SPECIFIEE';

/**
 * Verdict de validité de la clause selon la Loi 03/07/1978 art. 65.
 *  - `VALIDE` : clause opposable, indemnité légale due.
 *  - `NULLE` : clause nulle de plein droit (rémunération insuffisante ou durée > 12 mois).
 *  - `PARTIELLEMENT_NULLE` : zone internationale non justifiée — nulle pour la partie étrangère.
 */
export type ClauseNonConcurrenceBeVerdict =
  | 'VALIDE'
  | 'NULLE'
  | 'PARTIELLEMENT_NULLE';

/**
 * Raison de nullité — émise uniquement pour les verdicts `NULLE` ou
 * `PARTIELLEMENT_NULLE`. `null` quand verdict = VALIDE.
 */
export type ClauseNonConcurrenceBeRaisonNullite =
  | 'REMUNERATION_INSUFFISANTE'
  | 'DUREE_EXCESSIVE'
  | 'ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE';

export interface ClauseNonConcurrenceBeRequest {
  /** Rémunération annuelle brute du salarié (€ > 0). */
  remunerationAnnuelleBrute: number;
  /** Durée de la clause en mois — entier 1-12. */
  dureeMois: number;
  /** Zone géographique de la clause. */
  zoneGeographique: ClauseNonConcurrenceBeZone;
  /** Optionnel : true si activité internationale prouvée (atténue zone étrangère). Défaut false. */
  activiteInternationaleProuvee?: boolean | null;
  /** Optionnel : seuil de salaire en vigueur (défaut backend = 73 571 €/2024). */
  salaireAnnuelSeuil?: number | null;
}

export interface ClauseNonConcurrenceBeResponse {
  /** Verdict de validité. */
  verdict: ClauseNonConcurrenceBeVerdict;
  /** Raison de nullité (null si VALIDE). */
  raisonNullite: ClauseNonConcurrenceBeRaisonNullite | null;
  /** Indemnité légale calculée (€, arrondi 2 décimales). */
  indemniteLegale: number;
  /** Formule détaillée (JetBrains Mono côté UI). */
  indemniteLegaleFormule: string;
  /** Base juridique en texte (JetBrains Mono italique côté UI). */
  baseJuridique: string;
  /** Avertissement (ex. seuil indexé annuellement). */
  avertissement: string;
}

/** Humanise une zone géographique pour l'affichage avocat. */
export function humanizeZoneGeographique(z: ClauseNonConcurrenceBeZone): string {
  switch (z) {
    case 'BELGIQUE_UNIQUEMENT': return 'Belgique uniquement';
    case 'BELGIQUE_ET_ETRANGER': return 'Belgique + étranger';
    case 'NON_SPECIFIEE': return 'Non spécifiée';
  }
}

/** Humanise une raison de nullité pour l'affichage avocat. */
export function humanizeRaisonNullite(
  r: ClauseNonConcurrenceBeRaisonNullite | null | undefined,
): string {
  switch (r) {
    case 'REMUNERATION_INSUFFISANTE':
      return 'Rémunération annuelle brute inférieure au seuil légal';
    case 'DUREE_EXCESSIVE':
      return 'Durée de la clause supérieure à 12 mois';
    case 'ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE':
      return 'Zone internationale sans activité internationale prouvée';
    default:
      return '';
  }
}

/** Humanise un verdict pour le badge du header. */
export function humanizeVerdict(v: ClauseNonConcurrenceBeVerdict): string {
  switch (v) {
    case 'VALIDE': return 'Clause valide';
    case 'NULLE': return 'Clause nulle';
    case 'PARTIELLEMENT_NULLE': return 'Partiellement nulle';
  }
}
