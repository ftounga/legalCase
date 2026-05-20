/**
 * SF-207-07b : modèle frontend de l'outil F-207 — RCC BE indemnité
 * complémentaire (Belgique uniquement, CCT n°17 art. 5 ; Loi du 3 juillet
 * 1978 ; AR du 3 mai 2007).
 *
 * <p>Calculateur <b>pur</b> sans verdict — la juridiction se limite au
 * calcul de l'indemnité complémentaire RCC due mensuellement par l'employeur
 * (formule CCT 17 art. 5 : moitié de la différence rémunération nette
 * référence / allocation ONEM, plancher 0) et de son montant total jusqu'à
 * l'âge légal de la pension.</p>
 *
 * <p>Contrat figé dans SF-207-07 backend (PR #1155).</p>
 */

export interface RccBeIndemniteComplementaireRequest {
  /** Requis, € ≥ 0 — rémunération nette mensuelle de référence à la rupture. */
  remunerationNetteReference: number;
  /** Requis, € ≥ 0 — allocation ONEM mensuelle de chômage (fournie par l'avocat). */
  allocationOnemMensuelle: number;
  /** Requis, ISO YYYY-MM-DD — date de naissance du salarié. */
  dateNaissanceSalarie: string;
  /** Requis, ISO YYYY-MM-DD — date de début effective du RCC. */
  dateDebutRcc: string;
  /** Optionnel, [60, 75] — défaut 66 côté backend (2026 ; 67 à partir de 2030). */
  ageLegalPension?: number | null;
  /** Optionnel, € ≥ 0 — plancher sectoriel CCT plus favorable. */
  planchersSectoriel?: number | null;
}

export interface RccBeIndemniteComplementaireResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  remunerationNetteReference: number;
  allocationOnemMensuelle: number;
  dateNaissanceSalarie: string;
  dateDebutRcc: string;
  ageLegalPensionApplique: number;
  planchersSectoriel: number | null;
  // Outputs calculés
  /** Indemnité CCT 17 art. 5 brute (€/mois, plancher 0). */
  indemniteMensuelleAvantPlancher: number;
  /** Indemnité finale (€/mois) après application du plancher sectoriel éventuel. */
  indemniteMensuelleEmployeur: number;
  /** true si le plancher sectoriel a modifié l'indemnité (> indemniteAvantPlancher strict). */
  planchersSectorielApplique: boolean;
  /** Date à laquelle le salarié atteint l'âge légal de la pension. */
  dateAgeLegalPension: string;
  /** Nombre de mois entre `dateDebutRcc` et `dateAgeLegalPension` (plancher 0). */
  moisRestantsJusquaPension: number;
  /** indemnité mensuelle × mois restants (€, 2 décimales). */
  montantTotalEmployeur: number;
  baseJuridique: string;
  formuleCalcul: string;
  country: 'BELGIQUE';
}
