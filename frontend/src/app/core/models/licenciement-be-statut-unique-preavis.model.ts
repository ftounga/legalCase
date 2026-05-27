/**
 * SF-213-03b : modèle frontend de l'outil F-213 — Préavis "statut unique"
 * du licenciement BE (Belgique uniquement, Loi du 26 décembre 2013 portant
 * introduction d'un statut unique entre ouvriers et employés — art. 37/1
 * et suivants de la Loi 03/07/1978 sur les contrats de travail).
 *
 * <p>Contrat figé dans SF-213-03 backend (record `LicenciementBeStatutUnique
 * PreavisResponse` + Bean Validation `LicenciementBeStatutUniquePreavisRequest`).</p>
 *
 * <p>Régime distinct du FR : le préavis FR repose sur les conventions
 * collectives + barème C. trav. L. 1234-1 (1 à 2 mois selon ancienneté) ;
 * le BE applique un barème légal articulé en SEMAINES suivant des paliers
 * d'ancienneté précis (Loi 26/12/2013 art. 37/1) avec, pour les contrats
 * antérieurs au 01/01/2014, une fraction "Claeys" pré-statut unique calculée
 * séparément. Ce simulateur ne traite QUE la partie statut unique
 * (post-2014) — la partie pré-2014 est traitée par un outil séparé
 * (SF-213-04, formule Claeys) à venir.</p>
 *
 * <p>Référence juridique étendue :
 * <ul>
 *   <li>Loi 03/07/1978 sur les contrats de travail, art. 37/2, §1er,
 *       al. 1 — barème statut unique par paliers d'ancienneté.</li>
 *   <li>CCT du Conseil National du Travail n° 109 du 12 février 2014 —
 *       motivation du licenciement.</li>
 *   <li>Loi 26/12/2013 portant introduction d'un statut unique entre
 *       ouvriers et employés (entrée en vigueur 01/01/2014).</li>
 * </ul></p>
 */

export interface LicenciementBeStatutUniquePreavisRequest {
  /** Ancienneté en années complètes au moment du licenciement (0-80). */
  ancienneteAnnees: number;
  /**
   * Mois supplémentaires au-delà des années complètes (0-11). Optionnel —
   * défaut 0 côté backend. Permet d'exprimer une ancienneté fine (ex : 9 ans 6 mois).
   */
  ancienneteMoisSupplementaires?: number | null;
  /** Salaire hebdomadaire brut de référence (€ > 0). */
  salaireHebdomadaireBrut: number;
  /** Date de notification du licenciement (ISO YYYY-MM-DD). */
  dateNotificationLicenciement: string;
  /**
   * True si l'ancienneté correspond intégralement à la période post-2014
   * (statut unique). False signale un contrat antérieur au 01/01/2014 dont
   * la partie pré-2014 doit être traitée séparément via l'outil Formule
   * Claeys (SF-213-04). Défaut true côté service si null.
   */
  partieStatutUniqueSeulement?: boolean | null;
}

export interface LicenciementBeStatutUniquePreavisResponse {
  /** UUID du dossier de référence (mirroring backend). */
  caseFileId: string;
  /** Echo de l'ancienneté en années saisie. */
  ancienneteAnnees: number;
  /** Echo des mois supplémentaires (peut être 0). */
  ancienneteMoisSupplementaires: number;
  /** Echo du salaire hebdomadaire brut. */
  salaireHebdomadaireBrut: number;
  /** Echo de la date de notification (ISO YYYY-MM-DD). */
  dateNotificationLicenciement: string;
  /** Echo du flag partieStatutUniqueSeulement. */
  partieStatutUniqueSeulement: boolean;
  /** Durée de préavis calculée en semaines (≥ 0). */
  dureePreavisEnSemaines: number;
  /** Date de fin du préavis (ISO YYYY-MM-DD). */
  dateFinPreavis: string;
  /**
   * Indemnité compensatoire de préavis (€) — calculée si l'employeur
   * choisit la rupture immédiate plutôt que de prester le préavis.
   * Formule : `dureePreavisEnSemaines × salaireHebdomadaireBrut`.
   */
  indemniteCompensatoire: number;
  /** Formule détaillée du calcul (affichage JetBrains Mono). */
  formuleCalcul: string;
  /** Base juridique humanisée (Loi 03/07/1978 art. 37/2 §1er). */
  baseJuridique: string;
  /**
   * Avertissement (texte) si le contrat est mixte (pré + post 2014) et que
   * la partie Claeys doit être calculée séparément. Null si statut unique pur.
   */
  avertissement?: string | null;
}

/**
 * Statut UX dérivé de `partieStatutUniqueSeulement` :
 * - `STATUT_UNIQUE_PUR` (vert) : contrat post-2014 simple, pas de Claeys.
 * - `MIXTE_CLAEYS_PLUS_STATUT_UNIQUE` (ambre) : contrat enjambant le 01/01/2014,
 *   le résultat actuel ne couvre QUE la partie statut unique — la partie
 *   Claeys est à calculer via l'outil SF-213-04.
 */
export type LicenciementBeStatutUniqueBadge =
  | 'STATUT_UNIQUE_PUR'
  | 'MIXTE_CLAEYS_PLUS_STATUT_UNIQUE';

/** Humanise le badge pour le header. */
export function humanizeStatutUniqueBadge(b: LicenciementBeStatutUniqueBadge): string {
  switch (b) {
    case 'STATUT_UNIQUE_PUR': return 'Statut unique (post-2014)';
    case 'MIXTE_CLAEYS_PLUS_STATUT_UNIQUE': return 'Contrat mixte (Claeys + statut unique)';
  }
}

/** Dérive le badge depuis la réponse. */
export function badgeFromResponse(
  r: LicenciementBeStatutUniquePreavisResponse,
): LicenciementBeStatutUniqueBadge {
  return r.partieStatutUniqueSeulement
    ? 'STATUT_UNIQUE_PUR'
    : 'MIXTE_CLAEYS_PLUS_STATUT_UNIQUE';
}
