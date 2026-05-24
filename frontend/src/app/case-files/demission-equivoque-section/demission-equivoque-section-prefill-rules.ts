/**
 * SF-212-22 — Helper partagé pour l'outil « Démission — validité et
 * caractère équivoque » (F-DT-41). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat
 * F-236 / F-237 / F-246).
 *
 * **Note SF-212-21 (backend)** : le sous-objet IA `demission_equivoque_detail`
 * n'a PAS été ajouté au record `TravailExtractedData` (limite JVM 255 slots
 * du constructeur canonical saturée par SF-212-23 — pattern aligné sur
 * SF-212-17 F-DT-43). Le pré-fill IA fera l'objet d'une SF de rattrapage
 * ultérieure (refactor du record TravailExtractedData en plusieurs sous-records
 * ou consommation du JSON brut `analysis_result`).
 *
 * En attendant, ce helper :
 *  - expose la même surface API que les autres helpers (parité contrat F-236)
 *  - renvoie systématiquement 0 pré-rempli et null pour chaque champ
 *  - reste prêt à être branché dès que le sous-objet IA sera disponible
 *
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — exigence prétorienne
 * Cass. soc. 09/05/2007 propre au droit français).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface DemissionEquivoquePrefillInput {
  aiData?: TravailExtractedData | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: DemissionEquivoquePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/**
 * SF-212-22 — le pré-fill IA est désactivé tant que SF-212-21 n'a pas pu
 * ajouter le sous-objet `demissionEquivoqueDetail` au record
 * `TravailExtractedData`. Toujours null en attendant la SF de rattrapage.
 */
export function computePrefillCount(input: DemissionEquivoquePrefillInput): number {
  if (!isFrance(input)) return 0;
  // Aucun champ IA actuellement projetable — sous-objet backend en attente.
  return 0;
}

export const DemissionEquivoqueSectionPrefillRules = {
  computePrefillCount,
};
