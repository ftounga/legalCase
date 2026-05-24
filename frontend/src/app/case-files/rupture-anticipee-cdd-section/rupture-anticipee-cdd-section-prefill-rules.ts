/**
 * SF-212-18 — Helper partagé pour l'outil "Rupture anticipée du CDD"
 * (F-DT-43). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Note importante : le sous-objet IA `rupture_anticipee_cdd_detail` n'est pas
 * actuellement projeté à plat sur `TravailExtractedData` côté backend (limite
 * JVM 255 slots du constructeur canonical du record, saturé par les vagues
 * F-212 antérieures + SF-212-23). Le pré-fill IA pour F-DT-43 sera traité par
 * une SF de rattrapage ultérieure (refactor de TravailExtractedData en
 * plusieurs records ou consommation de l'analysis_result raw JSON).
 *
 * `computePrefillCount()` retourne donc 0 sur les fixtures normales —
 * l'avocat saisit manuellement les 6 champs du formulaire. Le déclenchement
 * F-IA-04 de l'outil reste fonctionnel via le flag top-level
 * `rupture_anticipee_cdd_detectee` (DecisionToolVisibilityService).
 *
 * Cette signature est conservée pour parité d'interface avec les autres
 * helpers de pré-fill (contrat F-236).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface RuptureAnticipeeCddPrefillInput {
  aiData?: TravailExtractedData | null;
  workspaceCountry?: string;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 actuellement (limite JVM
 * du backend — voir commentaire d'en-tête du module).
 */
export function computePrefillCount(_input: RuptureAnticipeeCddPrefillInput): number {
  // Pas de pré-fill IA tant que le sous-objet `ruptureAnticipeeCddDetail`
  // n'est pas projeté côté backend. SF de rattrapage à prévoir post-refactor
  // de TravailExtractedData.
  return 0;
}

export const RuptureAnticipeeCddSectionPrefillRules = {
  computePrefillCount,
};
