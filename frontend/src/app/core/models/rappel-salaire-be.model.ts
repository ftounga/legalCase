/**
 * SF-213-02b : modèle frontend de l'outil F-213 — Rappel de salaire BE
 * (Belgique uniquement, Loi 12/04/1965 art. 10 + Loi 03/07/1978 art. 15).
 *
 * <p>Contrat figé dans SF-213-02 backend.</p>
 *
 * <p>Régime distinct du FR :
 *  - Prescription 5 ans pendant contrat (vs 3 ans FR),
 *  - 1 an post-rupture (Loi 03/07/1978 art. 15 al. 1),
 *  - Intérêts moratoires légaux de plein droit à 10 % (Loi 12/04/1965 art. 10).</p>
 */

/** Régime applicable à la créance d'arriéré — détermine le délai de prescription. */
export type RappelSalaireBeTypeArriere =
  | 'PENDANT_CONTRAT'
  | 'POST_RUPTURE'
  | 'MIXTE';

/**
 * Statut de prescription :
 *  - `PRESCRIT` : action forclose.
 *  - `IMMINENT` : ≤ 30 jours avant prescription — alerte rouge UX.
 *  - `NON_PRESCRIT` : action recevable.
 */
export type RappelSalaireBeStatutPrescription =
  | 'PRESCRIT'
  | 'IMMINENT'
  | 'NON_PRESCRIT';

export interface RappelSalaireBeRequest {
  /** Montant brut réclamé (€ > 0). */
  montantBrut: number;
  /** Début de la période d'arriéré (ISO YYYY-MM-DD). */
  dateDebutPeriode: string;
  /** Fin de la période d'arriéré — date d'exigibilité principale (ISO YYYY-MM-DD). */
  dateFinPeriode: string;
  /** Optionnel — date de rupture du contrat (régime 1 an post-rupture). */
  dateRupture?: string | null;
  /** Optionnel — date d'action envisagée (défaut backend = today). */
  dateActionEnvisagee?: string | null;
  /** Régime applicable. */
  typeArriereEnum: RappelSalaireBeTypeArriere;
}

export interface RappelSalaireBeResponse {
  /** Montant brut réclamé. */
  montantBrut: number;
  /** Intérêts moratoires courus à `dateActionEnvisagee`. */
  interetsCourus: number;
  /** Taux moratoire humanisé (ex. "10% (Loi 12/04/1965 art. 10)"). */
  tauxMoratoire: string;
  /** Total réclamable (montantBrut + interetsCourus). */
  totalReclame: number;
  /** Date limite de prescription calculée (ISO YYYY-MM-DD). */
  dateLimitePrescription: string;
  /** Jours restants avant prescription (peut être négatif si prescrit). */
  joursRestantsAvantPrescription: number;
  /** Statut de prescription. */
  statutPrescription: RappelSalaireBeStatutPrescription;
  /** Base juridique humanisée (texte court). */
  baseJuridique: string;
  /** Formule détaillée du calcul (affichage JetBrains Mono). */
  formuleCalcul: string;
}

/** Humanise un type d'arriéré pour le mat-select. */
export function humanizeTypeArriere(t: RappelSalaireBeTypeArriere): string {
  switch (t) {
    case 'PENDANT_CONTRAT': return 'Pendant contrat (prescription 5 ans)';
    case 'POST_RUPTURE': return 'Post-rupture (prescription 1 an)';
    case 'MIXTE': return 'Mixte (avant et après rupture)';
  }
}

/** Humanise un statut de prescription pour le badge. */
export function humanizeStatutPrescription(s: RappelSalaireBeStatutPrescription): string {
  switch (s) {
    case 'PRESCRIT': return 'Prescrit';
    case 'IMMINENT': return 'Prescription imminente';
    case 'NON_PRESCRIT': return 'Action recevable';
  }
}
