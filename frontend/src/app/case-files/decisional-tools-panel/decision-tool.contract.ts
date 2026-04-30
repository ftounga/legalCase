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
