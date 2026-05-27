/**
 * SF-213-05b : modèle frontend de l'outil F-213 — Protection contre le
 * licenciement pendant la grossesse / maternité en droit belge.
 *
 * <p>Belgique uniquement — Loi du 16 mars 1971 sur la protection de la
 * maternité, art. 40. Pas d'équivalent direct en droit français : la France
 * a une protection similaire (L.1225-4 C. trav.) mais le calcul de
 * l'indemnité et la durée de la fenêtre protégée diffèrent — un outil FR
 * dédié reste hors scope SF-213-05.</p>
 *
 * <p>Contrat figé dans SF-213-05 backend
 * ({@code LicenciementBeProtectionGrossesseResponse} +
 * {@code LicenciementBeProtectionGrossesseRequest}).</p>
 *
 * <p>Référence juridique :
 * <ul>
 *   <li>Loi du 16/03/1971 sur la protection de la maternité, art. 40 :
 *       interdiction de licenciement pendant la période protégée.</li>
 *   <li>Période protégée : début grossesse → fin congé maternité + 1 mois
 *       (ou accouchement + 3 mois si pas de congé maternité documenté).</li>
 *   <li>Présomption d'inversion de charge si notification écrite ET
 *       licenciement ≤ 10 sem post début grossesse — l'employeur doit
 *       prouver le motif étranger à la grossesse.</li>
 *   <li>Indemnité forfaitaire = 6 × rémunération mensuelle brute
 *       (art. 40 al. 3), cumulable avec dommages prouvés.</li>
 * </ul></p>
 */

/**
 * Verdict de l'analyseur (4 états — miroir enum backend
 * {@code LicenciementBeProtectionGrossesseVerdict}).
 *
 * <ul>
 *   <li>{@code HORS_PERIODE_PROTECTION} — licenciement hors fenêtre.
 *       Pas d'indemnité forfaitaire (badge vert).</li>
 *   <li>{@code PROTECTION_APPLICABLE_NON_NOTIFIEE} — période protégée mais
 *       notification écrite absente. Protection applicable, pas
 *       d'inversion (badge ambre).</li>
 *   <li>{@code PROTECTION_APPLICABLE} — période protégée + notification
 *       écrite (hors fenêtre 10 sem). Charge de preuve normale (badge rouge).</li>
 *   <li>{@code PROTECTION_PRESUMEE} — notification écrite + licenciement
 *       ≤ 10 sem post début grossesse. Présomption irréfragable, charge
 *       de preuve renversée — l'employeur doit prouver (badge rouge foncé).</li>
 * </ul>
 */
export type LicenciementBeProtectionGrossesseVerdict =
  | 'HORS_PERIODE_PROTECTION'
  | 'PROTECTION_APPLICABLE_NON_NOTIFIEE'
  | 'PROTECTION_APPLICABLE'
  | 'PROTECTION_PRESUMEE';

/**
 * Body POST `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse`.
 *
 * <p>{@code dateDebutGrossesse}, {@code dateLicenciement},
 * {@code remunerationMensuelleBrute} sont <b>requis</b>. Les autres champs
 * sont optionnels — le backend les défaute (null pour dates, false pour
 * notification, null pour motif).</p>
 */
export interface LicenciementBeProtectionGrossesseRequest {
  /** Date de début de grossesse (ISO YYYY-MM-DD) — point de départ de la fenêtre protégée. */
  dateDebutGrossesse: string;
  /** Date prévue / réelle d'accouchement (ISO) — précise la fin de protection si congé maternité absent. */
  dateAccouchement?: string | null;
  /** Date de début du congé maternité (ISO) — optionnel, traçabilité. */
  dateCongeMaterniteDebut?: string | null;
  /** Date de fin du congé maternité (ISO) — si présente, fin protection = +1 mois. */
  dateCongeMaterniteFinale?: string | null;
  /** Date de notification du licenciement (ISO YYYY-MM-DD). */
  dateLicenciement: string;
  /** Grossesse notifiée par écrit à l'employeur — défaut false côté backend. */
  grossesseNotifieeParEcrit?: boolean | null;
  /** Rémunération mensuelle brute (€ > 0) — base de calcul de l'indemnité forfaitaire (×6). */
  remunerationMensuelleBrute: number;
  /** Motif allégué par l'employeur (texte libre) — documenté pour traçabilité. */
  motifInvoqueParEmployeur?: string | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 *
 * <p>L'indemnité forfaitaire vaut {@code 6 × remunerationMensuelleBrute}
 * (art. 40 al. 3) — calculée backend ; 0 si verdict HORS_PERIODE_PROTECTION.
 * Le {@code chargePreuveEmployeur} est {@code true} si verdict
 * PROTECTION_PRESUMEE (notification écrite + licenciement dans les 10 sem
 * post début grossesse).</p>
 */
export interface LicenciementBeProtectionGrossesseResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;
  /** Echo date début grossesse. */
  dateDebutGrossesse: string;
  /** Echo date accouchement (peut être null). */
  dateAccouchement: string | null;
  /** Echo date début congé maternité (peut être null). */
  dateCongeMaterniteDebut: string | null;
  /** Echo date fin congé maternité (peut être null). */
  dateCongeMaterniteFinale: string | null;
  /** Echo date du licenciement. */
  dateLicenciement: string;
  /** Echo notification écrite. */
  grossesseNotifieeParEcrit: boolean;
  /** Echo rémunération mensuelle brute. */
  remunerationMensuelleBrute: number;
  /** Echo motif employeur (peut être null). */
  motifInvoqueParEmployeur: string | null;
  /** Verdict 4 états. */
  verdict: LicenciementBeProtectionGrossesseVerdict;
  /** Drapeau dérivé : licenciement dans la fenêtre protégée. */
  licenciementDansLaPeriodeProtegee: boolean;
  /** Début de la période protégée (= dateDebutGrossesse en pratique). */
  dateDebutProtection: string | null;
  /**
   * Fin de la période protégée : congé maternité fin + 1 mois si renseigné,
   * sinon accouchement + 3 mois si renseigné, sinon null + bannière
   * avertissement.
   */
  dateFinProtection: string | null;
  /** Indemnité forfaitaire = 6 × rémunération mensuelle brute (€). 0 si hors période. */
  indemniteForfaitaire: number;
  /** True si verdict PROTECTION_PRESUMEE (charge de preuve renversée). */
  chargePreuveEmployeur: boolean;
  /** Base juridique humanisée (Loi 16/03/1971 art. 40). */
  baseJuridique: string;
  /** Formule détaillée du calcul (affichage JetBrains Mono). */
  formuleCalcul: string;
  /** Avertissement éventuel — bannière ambre (ex : « Date fin protection indéterminée »). */
  avertissement: string | null;
}
