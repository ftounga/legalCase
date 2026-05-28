/**
 * SF-219-05b : modèle frontend de l'outil F-219 — Outplacement BE général
 * au titre du régime « préavis ≥ 30 semaines ».
 *
 * <p><b>BE uniquement</b> — Loi du 05/09/2001 art. 11 + AR du 21/10/2007.
 * Aucun équivalent strict en droit français.</p>
 *
 * <p>Distinct de l'outplacement obligatoire 45+ ans (F-207 SF-207-08 —
 * autre régime, qui s'applique quelle que soit la durée du préavis).</p>
 *
 * <p>Contrat figé dans SF-219-05 backend
 * ({@code OutplacementBeGeneral30semRequest} +
 * {@code OutplacementBeGeneral30semResponse}).</p>
 *
 * <p>Analyseur de conformité (7 verdicts hiérarchiques — la première
 * condition non remplie l'emporte) :
 * <ul>
 *   <li>{@code CONFORME} (vert) — toutes les conditions cumulatives sont
 *       remplies.</li>
 *   <li>{@code NON_DU_DEMISSION} (gris info) — démission : pas
 *       d'obligation d'offre d'outplacement.</li>
 *   <li>{@code NON_DU_MOTIF_GRAVE} (gris info) — licenciement pour motif
 *       grave : exclu du régime (art. 11/3 § 1er).</li>
 *   <li>{@code NON_DU_PREAVIS_INSUFFISANT} (gris info) — préavis &lt; 30
 *       semaines : régime « 30 semaines » non applicable.</li>
 *   <li>{@code NON_CONFORME_OFFRE_TARDIVE} (rouge) — offre absente ou
 *       notifiée &gt; 15 jours après fin du contrat.</li>
 *   <li>{@code NON_CONFORME_DUREE_INSUFFISANTE} (rouge) — programme
 *       &lt; 60h ou période &gt; 12 mois (AR 21/10/2007).</li>
 *   <li>{@code NON_CONFORME_FORME} (rouge) — forme non conforme
 *       (non écrite / non individuelle / contenu minimum absent).</li>
 * </ul></p>
 */

/** Verdict de l'outil de conformité outplacement BE général 30 semaines. */
export type OutplacementBeGeneral30semVerdict =
  | 'CONFORME'
  | 'NON_DU_DEMISSION'
  | 'NON_DU_MOTIF_GRAVE'
  | 'NON_DU_PREAVIS_INSUFFISANT'
  | 'NON_CONFORME_OFFRE_TARDIVE'
  | 'NON_CONFORME_DUREE_INSUFFISANTE'
  | 'NON_CONFORME_FORME';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-general-30sem}.
 *
 * <p>Tous les champs sont requis côté backend
 * ({@code @NotNull / @PositiveOrZero}) sauf {@code dateNotificationOffre}
 * (nullable si {@code offreNotifiee = false}).</p>
 */
export interface OutplacementBeGeneral30semRequest {
  /** True si l'employeur licencie (vs démission). */
  licenciementEffectif: boolean;
  /** True si licenciement pour motif grave (art. 35 Loi du 03/07/1978). */
  motifGrave: boolean;
  /** Durée du préavis notifié (ou indemnité de rupture équivalente) en semaines. */
  semainesPreavisOuIndemnite: number;
  /** Date à laquelle le contrat prend fin (ISO yyyy-MM-dd). */
  dateFinContrat: string;
  /** True si l'employeur a offert l'outplacement. */
  offreNotifiee: boolean;
  /** Date de notification de l'offre (ISO yyyy-MM-dd, nullable si pas d'offre). */
  dateNotificationOffre?: string | null;
  /** Nombre d'heures du programme proposé. */
  heuresProgramme: number;
  /** Durée du programme en mois. */
  dureeProgrammeMois: number;
  /** True si l'offre est écrite. */
  offreEcrite: boolean;
  /** True si l'offre est individuelle. */
  offreIndividuelle: boolean;
  /** True si le contenu minimum (bilan/projet/coaching/formation/soutien) est respecté. */
  contenuMinimumRespecte: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface OutplacementBeGeneral30semResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  licenciementEffectif: boolean;
  motifGrave: boolean;
  semainesPreavisOuIndemnite: number;
  dateFinContrat: string;
  offreNotifiee: boolean;
  dateNotificationOffre: string | null;
  heuresProgramme: number;
  dureeProgrammeMois: number;
  offreEcrite: boolean;
  offreIndividuelle: boolean;
  contenuMinimumRespecte: boolean;

  // -- Verdict & conformité --
  verdict: OutplacementBeGeneral30semVerdict;
  /** True si verdict = CONFORME. */
  conforme: boolean;
  /** Licenciement effectif (non démission). */
  conditionLicenciementRemplie: boolean;
  /** Motif non grave. */
  conditionMotifNonGraveRemplie: boolean;
  /** Préavis ≥ 30 semaines. */
  conditionPreavis30semRemplie: boolean;
  /** Offre notifiée dans les 15 jours civils suivant fin contrat. */
  conditionOffreDansLes15joursRemplie: boolean;
  /** Programme ≥ 60h sur ≤ 12 mois. */
  conditionDureeProgrammeRemplie: boolean;
  /** Forme conforme (écrit + individuel + contenu minimum). */
  conditionFormeOffreRemplie: boolean;

  // -- Sanction indicative --
  /** Indemnité forfaitaire due au travailleur si non-conformité (€). */
  indemniteForfaitaireTravailleur: number;

  // -- Synthèse + métadonnées légales --
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
