/**
 * SF-219-04b : modèle frontend de l'outil F-219 — Cumul RCC BE + allocations
 * chômage / pension (CCT n° 17 du 19/12/1974 + AR du 25/11/1991 ONEM +
 * AR du 03/05/2007 art. 22 et suiv. — disponibilité ajustée selon l'âge).
 *
 * <p><b>BE uniquement</b> — aucun équivalent strict en droit français
 * (pré-retraite supprimée 2010 ; dispositifs amiante / C2P pénibilité
 * distincts).</p>
 *
 * <p>Contrat figé dans SF-219-04 backend ({@code CumulRccAllocationsRequest}
 * + {@code CumulRccAllocationsResponse}).</p>
 *
 * <p>Analyseur transversal de cumul (4 verdicts) :
 * <ul>
 *   <li>{@code CUMUL_CONFORME} (vert) — total ≤ rémunération nette de
 *       référence + disponibilité OK.</li>
 *   <li>{@code CUMUL_PLAFOND_DEPASSE} (rouge) — total &gt; rémunération
 *       nette de référence : l'indemnité complémentaire doit être réduite
 *       (CCT 17 — plancher interdit, l'employeur ne peut faire mieux que
 *       la dernière rémunération nette).</li>
 *   <li>{@code BASCULE_PENSION_LEGALE} (info) — âge légal pension atteint :
 *       le RCC s'éteint, bascule pension légale.</li>
 *   <li>{@code INCOMPATIBLE_ACTIVITE_NON_AUTORISEE} (rouge) — reprise
 *       d'activité non déclarée : sortie du régime + récupération ONEM.</li>
 * </ul></p>
 */

/** Verdict de l'outil de cumul RCC + allocations. */
export type CumulRccAllocationsVerdict =
  | 'CUMUL_CONFORME'
  | 'CUMUL_PLAFOND_DEPASSE'
  | 'BASCULE_PENSION_LEGALE'
  | 'INCOMPATIBLE_ACTIVITE_NON_AUTORISEE';

/**
 * Types d'activités complémentaires autorisés en cumul RCC (ONEM).
 *
 * <p>L'ONEM permet certaines activités en complément du RCC sous conditions
 * strictes (déclaration préalable, plafonds de revenus accessoires). Toute
 * reprise non listée ou non déclarée entraîne la sortie du régime.</p>
 */
export type CumulRccAllocationsActiviteAutorisee =
  | 'AUCUNE'
  | 'ACTIVITE_INDEPENDANT_ACCESSOIRE'
  | 'ACTIVITE_BENEVOLE_DECLAREE'
  | 'ACTIVITE_NON_DECLAREE';

/**
 * Régime de disponibilité applicable au bénéficiaire RCC (AR 03/05/2007
 * art. 22 et suiv.) — modulé selon l'âge à la prise de cours.
 */
export type CumulRccAllocationsDisponibiliteRegime =
  | 'DISPONIBILITE_ADAPTEE'
  | 'DISPONIBILITE_PASSIVE'
  | 'DISPENSE_RECHERCHE_EMPLOI';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/cumul-rcc-allocations}.
 *
 * <p>Tous les champs sont requis côté backend
 * ({@code @NotNull / @PositiveOrZero}). {@code ageLegalPension} est
 * optionnel (par défaut 65 ans côté backend — permet de tester la
 * progression vers 66 ans en 2027 / 67 ans en 2030).</p>
 */
export interface CumulRccAllocationsRequest {
  /** Date de naissance — détermine l'âge à la date considérée. */
  dateNaissance: string;
  /** Date de prise d'effet du RCC. */
  dateDebutRcc: string;
  /** Montant mensuel des allocations chômage ONEM (€). */
  allocationChomageMensuelle: number;
  /** Indemnité complémentaire CCT 17 mensuelle à charge employeur (€). */
  indemniteComplementaireMensuelle: number;
  /** Dernière rémunération nette mensuelle de référence (€). */
  remunerationNetteReference: number;
  /** Carrière totale en années (ETP). */
  anneesCarriereTotale: number;
  /** Nature de l'activité complémentaire éventuelle. */
  activiteComplementaire: CumulRccAllocationsActiviteAutorisee;
  /** Âge légal pension (par défaut 65 si omis). */
  ageLegalPension?: number | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface CumulRccAllocationsResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateNaissance: string;
  dateDebutRcc: string;
  allocationChomageMensuelle: number;
  indemniteComplementaireMensuelle: number;
  remunerationNetteReference: number;
  anneesCarriereTotale: number;
  activiteComplementaire: CumulRccAllocationsActiviteAutorisee;
  ageLegalPension: number | null;

  // -- Outputs métier --
  verdict: CumulRccAllocationsVerdict;
  /** True si verdict = CUMUL_CONFORME. */
  conforme: boolean;
  /** Âge à la date d'entrée RCC. */
  ageADateDebutRcc: number;
  /** Cumul mensuel total (allocations + indemnité complémentaire). */
  cumulMensuelTotal: number;
  /** True si cumul ≤ rémunération nette de référence. */
  plafondRespecte: boolean;
  /** Montant à déduire de l'indemnité complémentaire (0 si plafond OK). */
  montantReductionIndemnite: number;
  /** Régime de disponibilité applicable selon l'âge. */
  regimeDisponibilite: CumulRccAllocationsDisponibiliteRegime;
  /** True si âge légal pension déjà atteint. */
  ageLegalPensionAtteint: boolean;

  // -- Synthèse + métadonnées légales --
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
