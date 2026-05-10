import { Type } from '@angular/core';

/**
 * F-177 SF-177-03 — Contrat statique exposé par chaque composant outil décisionnel
 * pour que le panel et le dashboard agrégé puissent afficher leur titre + icône
 * dans la card sans instancier le composant.
 *
 * Convention :
 * - `TOOL_LABEL` : intitulé MAJUSCULES affiché dans le header de la card (et du modal)
 * - `TOOL_ICON` : nom Material Icon affiché à gauche du titre
 *
 * Les composants outils doivent ajouter ces 2 statics ; le panel les lit via
 * `getToolMetadata(component)`. Si un composant ne les expose pas, `getToolMetadata`
 * retourne `null` et le panel applique un fallback.
 */
export interface DecisionToolStatic {
  readonly TOOL_LABEL: string;
  readonly TOOL_ICON: string;
}

export interface DecisionToolMetadata {
  label: string;
  icon: string;
}

/**
 * Lit `TOOL_LABEL` + `TOOL_ICON` exposés en statics par un composant outil.
 * Retourne `null` si l'un des deux manque.
 */
export function getToolMetadata(component: Type<unknown>): DecisionToolMetadata | null {
  const candidate = component as unknown as Partial<DecisionToolStatic>;
  if (typeof candidate.TOOL_LABEL === 'string' && typeof candidate.TOOL_ICON === 'string') {
    return { label: candidate.TOOL_LABEL, icon: candidate.TOOL_ICON };
  }
  return null;
}

/**
 * F-177 SF-177-12 — Input du static `getPrefillCount` exposé par les composants
 * outils décisionnels. Pattern miroir des `inputs:` de TOOL_REGISTRY (couplage
 * faible — chaque composant cast vers son type fort en interne).
 *
 * `triggerEvents` et `workspaceCountry` sont ajoutés en top-level pour refléter
 * fidèlement les `@Input` réels des composants pilotes (ex. immigration-title-decision
 * lit `triggerEvents` séparément, immigration-work-right gate sur `workspaceCountry`).
 */
export interface PrefillCountInput {
  aiData?: any;
  procedureChecks?: any[];
  aiQuestions?: any[];
  piecesManquantes?: any[];
  triggerEvents?: any[];
  workspaceCountry?: string;
  /**
   * F-177 SF-177-12+ : synthèse complète du dossier — utile pour les
   * composants qui dépendent d'estimations dérivées (ex : F-FA-06
   * `pensionAlimentaireEstimate.modeGardeDetaille`) plutôt que de
   * `aiData` direct. Optionnel ; si absent, le composant doit dégrader
   * gracieusement (retourne 0).
   */
  synthesis?: any;
}

/**
 * F-177 SF-177-12 — Static optionnel exposé par les composants outils :
 * retourne le nombre de champs du formulaire que `prefillFromAi()` poserait
 * si le composant était instancié maintenant avec ces inputs.
 *
 * - Stricte parité avec la logique de `prefillFromAi()` (mêmes guards).
 * - 0 si rien à pré-remplir (aiData absent, champs vides…).
 * - Permet à la card du panel d'afficher le badge `auto_awesome` AVANT
 *   l'instanciation effective du composant outil.
 */
export interface DecisionToolPrefillStatic {
  getPrefillCount(input: PrefillCountInput): number;
}

let prefillWarnings = new Set<string>();

/**
 * F-177 SF-177-12 — Lit le static `getPrefillCount` du composant outil.
 * Retourne `null` si :
 *   - le static n'est pas exposé (composant pas encore instrumenté), OU
 *   - le static throw à l'exécution (log `console.warn` 1x par composant).
 *
 * Le panel passe `null` à `<app-decision-tool-card [prefillCount]>` ce qui
 * masque silencieusement le badge (fallback safe).
 */
export function getToolPrefillCount(
  component: Type<unknown>,
  input: PrefillCountInput,
): number | null {
  const candidate = component as unknown as Partial<DecisionToolPrefillStatic>;
  if (typeof candidate.getPrefillCount !== 'function') {
    return null;
  }
  try {
    const value = candidate.getPrefillCount(input);
    return typeof value === 'number' && value >= 0 ? value : null;
  } catch (err) {
    const name = (component as { name?: string }).name ?? '<anonymous>';
    if (!prefillWarnings.has(name)) {
      prefillWarnings.add(name);
      // eslint-disable-next-line no-console
      console.warn(`[decision-tool] getPrefillCount threw on ${name}:`, err);
    }
    return null;
  }
}

/** Test helper — reset l'idempotence du `console.warn`. */
export function _resetPrefillWarningsForTests(): void {
  prefillWarnings = new Set<string>();
}
