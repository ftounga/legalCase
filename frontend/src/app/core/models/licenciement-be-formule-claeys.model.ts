/**
 * SF-213-04b : modèle frontend de l'outil F-213 — Préavis Formule Claeys BE
 * (Belgique uniquement, ancien art. 82 §3 Loi 03/07/1978 sur les contrats
 * de travail, applicable aux contrats antérieurs au statut unique
 * 01/01/2014). Combinable avec le préavis statut unique post-2014 via la
 * clause de sauvegarde de la Loi 26/12/2013 art. 67.
 *
 * <p>Contrat figé dans SF-213-04 backend
 * (`LicenciementBeFormuleClaeysResponse` + `LicenciementBeFormuleClaeysRequest`).</p>
 *
 * <p>Régime distinct du FR — pas d'équivalent direct. La France ne connaît
 * pas la dichotomie pré-/post-statut unique (le préavis FR repose sur les
 * conventions collectives + L. 1234-1 C. trav.). Cet outil est BE-only.</p>
 *
 * <p>Référence juridique étendue :
 * <ul>
 *   <li>Ancien art. 82 §3 Loi 03/07/1978 sur les contrats de travail :
 *       formule Claeys originelle (employés "supérieurs", contrat pré-2014).</li>
 *   <li>Cass. 28/02/2011 (RG S.10.0073.F) — consécration jurisprudentielle
 *       de la formule par défaut.</li>
 *   <li>Loi 26/12/2013 portant statut unique, art. 67 — clause de
 *       sauvegarde pour la fraction d'ancienneté pré-2014.</li>
 *   <li>Cass. 14/12/2015 (RG S.15.0024.F) — formule Claeys reste
 *       jurisprudentielle, pas légale stricto sensu.</li>
 * </ul></p>
 */

/**
 * Body POST `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys`.
 *
 * <p>Les 2 derniers champs ({@code ancienneteAnneesPostStatutUnique},
 * {@code salaireHebdomadaireBrut}) sont <b>requis</b> côté backend si
 * {@code appliquerClauseSauvegarde === true} — la cohérence est vérifiée
 * par le service qui renvoie 400 sinon.</p>
 */
export interface LicenciementBeFormuleClaeysRequest {
  /** Années complètes d'ancienneté pré-2014 (0-80). */
  ancienneteAnneesPreStatutUnique: number;
  /** Mois supplémentaires pré-2014 (0-11). Optionnel, défaut 0 côté backend. */
  ancienneteMoisPreStatutUnique?: number | null;
  /**
   * Rémunération annuelle brute au 31/12/2013 exprimée en milliers d'euros
   * (K€). Ex : un salaire annuel de 52 000 € → 52.
   */
  remunerationAnnuelleBruteEnMilliers: number;
  /**
   * Si true (défaut backend si null), cumule le préavis Claeys avec le
   * préavis statut unique post-2014 — clause de sauvegarde loi 26/12/2013
   * art. 67. Si false, le calcul s'arrête au préavis Claeys pur.
   */
  appliquerClauseSauvegarde?: boolean | null;
  /**
   * Années complètes d'ancienneté post-01/01/2014. Requis si
   * {@code appliquerClauseSauvegarde === true}.
   */
  ancienneteAnneesPostStatutUnique?: number | null;
  /**
   * Salaire hebdomadaire brut au moment du licenciement (€ > 0). Requis si
   * {@code appliquerClauseSauvegarde === true} pour calculer l'indemnité
   * compensatoire totale.
   */
  salaireHebdomadaireBrut?: number | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 *
 * <p>{@code preavisClaeysMois} est un BigDecimal côté backend (formule
 * fractionnaire ; ex : 11.52 mois). {@code preavisClaeysSemaines} est la
 * conversion arrondie en semaines. {@code preavisStatutUniquesSemaines}
 * vaut 0 si {@code appliquerClauseSauvegarde === false} ; sinon contient
 * la fraction statut unique calculée par le service.</p>
 */
export interface LicenciementBeFormuleClaeysResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;
  /** Echo ancienneté pré-statut unique (années). */
  ancienneteAnneesPreStatutUnique: number;
  /** Echo ancienneté pré-statut unique (reliquat mois 0-11). */
  ancienneteMoisPreStatutUnique: number;
  /** Echo rémunération annuelle brute au 31/12/2013 en K€. */
  remunerationAnnuelleBruteEnMilliers: number;
  /** Echo flag clause de sauvegarde. */
  appliquerClauseSauvegarde: boolean;
  /** Echo ancienneté post-statut unique (années) — null si pas de sauvegarde. */
  ancienneteAnneesPostStatutUnique: number | null;
  /** Echo salaire hebdo brut — null si pas de sauvegarde. */
  salaireHebdomadaireBrut: number | null;
  /**
   * Préavis Claeys calculé exprimé en MOIS (fractionnaire). Issu de la
   * formule canonique 0.8 × (12 + 0.04 × age_a_remplir) appliquée à l'âge
   * effectif (déduit de l'ancienneté + référence rémunération).
   */
  preavisClaeysMois: number;
  /** Conversion arrondie en semaines (= preavisClaeysMois × 4.333…). */
  preavisClaeysSemaines: number;
  /**
   * Fraction statut unique post-2014 (semaines) — 0 si clause de sauvegarde
   * désactivée. Pré-2014 traité par la formule Claeys ; post-2014 ajouté
   * via loi 26/12/2013 art. 67.
   */
  preavisStatutUniquesSemaines: number;
  /** Somme {@code preavisClaeysSemaines + preavisStatutUniquesSemaines}. */
  preavisTotalSemaines: number;
  /**
   * Indemnité compensatoire correspondant à la fraction Claeys (€) —
   * basée sur la rémunération annuelle au 31/12/2013.
   */
  indemniteClaeysBrute: number;
  /**
   * Indemnité compensatoire totale (€) — fraction Claeys + fraction
   * statut unique si clause de sauvegarde. Null si clause de sauvegarde
   * désactivée (seule la fraction Claeys est calculée).
   */
  indemniteTotaleBrute: number | null;
  /** Formule détaillée du calcul (affichage JetBrains Mono). */
  formuleClaeys: string;
  /** Base juridique humanisée (Cass. 28/02/2011 + ancien art. 82). */
  baseJuridique: string;
  /**
   * Avertissement systématique sur le caractère jurisprudentiel de la
   * formule Claeys (pas d'ancrage légal stricto sensu — Cass. 14/12/2015).
   * Toujours présent (le backend le renvoie systématiquement).
   */
  avertissement: string;
}
