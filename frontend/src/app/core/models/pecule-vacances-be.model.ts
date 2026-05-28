/**
 * SF-219-20b : modèle frontend de l'outil F-219 — pécule de vacances
 * BE (Lois coordonnées du 28/06/1971 + AR du 30/03/1967).
 *
 * <p><b>BE uniquement</b> — l'indemnité compensatrice de congés payés
 * française (C. trav. art. L. 3141-24 et s. — 10 % de la rémunération
 * brute totale ou maintien du salaire) constitue un régime distinct
 * (pas de double pécule, pas de débiteur tiers de type ONVA). Aucun
 * mapping mécanique. Le gate {@code workspaceCountry} est porté côté
 * composant ; le backend renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-20 backend
 * ({@code PeculeVacancesBeRequest} + {@code PeculeVacancesBeResponse}).</p>
 */

/**
 * Statut du travailleur — aiguille le débiteur du pécule.
 *
 * <ul>
 *   <li>{@code EMPLOYE} — pécule payé directement par l'employeur sur
 *       la rémunération de l'exercice (art. 38 et art. 46 AR
 *       30/03/1967).</li>
 *   <li>{@code OUVRIER} — pécule payé par l'ONVA / Caisse de vacances
 *       sectorielle sur la base des cotisations versées par l'employeur
 *       (art. 19 AR 30/03/1967) ; pas d'action directe contre
 *       l'employeur hors défaut de versement de cotisations.</li>
 *   <li>{@code JEUNE_TRAVAILLEUR} — pécule vacances jeunes 1re année
 *       (art. 9 et 9bis AR 30/03/1967) calculé sur l'allocation de
 *       chômage / revenu d'intégration de l'année précédente.</li>
 *   <li>{@code INDETERMINE} — statut à qualifier (analyse cas par
 *       cas).</li>
 * </ul>
 */
export type PeculeVacancesBeStatut =
  | 'EMPLOYE'
  | 'OUVRIER'
  | 'JEUNE_TRAVAILLEUR'
  | 'INDETERMINE';

/**
 * Type de calcul demandé.
 *
 * <ul>
 *   <li>{@code PECULE_SIMPLE} — rémunération normale maintenue pendant
 *       les jours de congé pris (art. 38 AR 30/03/1967, employés) ;
 *       pour les ouvriers, fraction du pécule ONVA via cotisations
 *       (art. 19 AR 30/03/1967).</li>
 *   <li>{@code DOUBLE_PECULE} — supplément payé au moment des vacances
 *       principales : 85 % du salaire mensuel brut (employés, art. 38
 *       AR 30/03/1967) ; intégré au pécule ONVA pour les ouvriers.</li>
 *   <li>{@code PECULE_DEPART} — pécule de sortie versé au départ de
 *       l'employé(e) : 15,34 % de la rémunération brute de l'exercice
 *       en cours + 15,34 % de la rémunération brute de l'exercice
 *       précédent non encore liquidée (art. 46 AR 30/03/1967). Pour
 *       les ouvriers, intégré au pécule ONVA de l'année suivante.</li>
 * </ul>
 */
export type PeculeVacancesBeTypeCalcul =
  | 'PECULE_SIMPLE'
  | 'DOUBLE_PECULE'
  | 'PECULE_DEPART';

/**
 * Verdict hiérarchisé 8 états — ordre de priorité (court-circuits
 * backend {@code PeculeVacancesBeCalculator}) :
 *
 * <ol>
 *   <li>{@code A_ANALYSER} — statut indéterminé.</li>
 *   <li>{@code PRESCRIT} — créance prescrite (art. 15 Loi 03/07/1978
 *       : 1 an depuis la fin du contrat / 5 ans depuis le fait
 *       générateur).</li>
 *   <li>{@code NON_DU_DEJA_PAYE} — pécule déjà liquidé (action sans
 *       objet).</li>
 *   <li>{@code NON_DU_OUVRIER_VIA_ONVA} — l'ouvrier doit s'adresser
 *       à l'ONVA / Caisse de vacances et non à l'employeur (art. 19
 *       AR 30/03/1967).</li>
 *   <li>{@code NON_DU_JOURS_INSUFFISANTS} — durée d'occupation
 *       insuffisante pour ouvrir le droit (vacances jeunes — art. 9
 *       AR 30/03/1967).</li>
 *   <li>{@code DU_PECULE_SIMPLE_CALCULE} — pécule simple liquidé,
 *       créance due par l'employeur.</li>
 *   <li>{@code DU_DOUBLE_PECULE_CALCULE} — double pécule liquidé,
 *       créance due (vacances principales).</li>
 *   <li>{@code DU_PECULE_DEPART_CALCULE} — pécule de sortie liquidé
 *       à la sortie de l'employé(e), créance due par l'employeur.</li>
 * </ol>
 */
export type PeculeVacancesBeVerdict =
  | 'DU_PECULE_SIMPLE_CALCULE'
  | 'DU_DOUBLE_PECULE_CALCULE'
  | 'DU_PECULE_DEPART_CALCULE'
  | 'NON_DU_OUVRIER_VIA_ONVA'
  | 'NON_DU_DEJA_PAYE'
  | 'NON_DU_JOURS_INSUFFISANTS'
  | 'PRESCRIT'
  | 'A_ANALYSER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/pecule-vacances-be}.
 *
 * <p>Tous les champs marqués requis sont {@code @NotNull} côté backend
 * (cf. {@code PeculeVacancesBeRequest}). {@code dateSortie} et
 * {@code dateFinContrat} sont optionnels — {@code dateSortie} n'est
 * pertinent que si {@code typeCalcul === 'PECULE_DEPART'},
 * {@code dateFinContrat} sert au calcul de la prescription post-
 * contrat (art. 15 Loi 03/07/1978).</p>
 */
export interface PeculeVacancesBeRequest {
  /** Statut du travailleur — aiguille le débiteur (employeur vs ONVA). */
  statut: PeculeVacancesBeStatut;
  /** Type de calcul demandé (simple / double / départ). */
  typeCalcul: PeculeVacancesBeTypeCalcul;
  /** Rémunération brute annuelle de l'exercice de vacances (€). */
  remunerationBruteAnnuelleExerciceEur: number;
  /** Rémunération mensuelle brute (€). */
  remunerationMensuelleBruteEur: number;
  /** Jours de congé pris (sur 24 jours en régime 6j, art. 3 Loi 28/06/1971). */
  joursCongesPris: number;
  /** Le pécule a déjà été payé (short-circuit du calcul). */
  peculeDejaPaye: boolean;
  /** Rémunération brute annuelle de l'exercice précédent (€) — pécule de départ. */
  remunerationBruteAnnuelleExercicePrecedentEur: number;
  /** Date de sortie — uniquement si typeCalcul = PECULE_DEPART. */
  dateSortie: string | null;
  /** Date fin du contrat — sert au calcul de la prescription. */
  dateFinContrat: string | null;
  /** Date de la réclamation — sert au calcul de la prescription. */
  dateReclamation: string;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + montants
 * calculés + verdict + métadonnées légales.
 */
export interface PeculeVacancesBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  statut: PeculeVacancesBeStatut;
  typeCalcul: PeculeVacancesBeTypeCalcul;
  remunerationBruteAnnuelleExerciceEur: number;
  remunerationMensuelleBruteEur: number;
  joursCongesPris: number;
  peculeDejaPaye: boolean;
  remunerationBruteAnnuelleExercicePrecedentEur: number;
  dateSortie: string | null;
  dateFinContrat: string | null;
  dateReclamation: string;

  // -- Outputs calculés --
  /** Montant pécule simple (€). */
  montantPeculeSimpleEur: number;
  /** Montant double pécule (€). */
  montantDoublePeculeEur: number;
  /** Montant pécule de départ (€). */
  montantPeculeDepartEur: number;
  /** Montant total dû (€). */
  montantTotalDuEur: number;
  /** Fraction légale appliquée (0..1) — 0,1534 si pécule départ employé. */
  fractionLegaleAppliquee: number;
  /** Créance prescrite (art. 15 Loi 03/07/1978). */
  prescrit: boolean;
  /** Jours écoulés depuis le fait générateur (fin contrat ou exercice). */
  joursDepuisFaitGenerateur: number;

  // -- Verdict + métadonnées légales --
  verdict: PeculeVacancesBeVerdict;
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement (ONVA débiteur, exercice de vacances vs exercice de prise, etc.). */
  avertissement: string;
}
