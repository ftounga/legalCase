/**
 * SF-218-02 — Helper partagé pour l'outil "Appel CPH devant la Cour d'appel"
 * (F-DT-86). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * <p>Le pipeline IA (SF-218-01 backend) extrait `dateNotificationJugement`
 * projeté à plat sur `TravailExtractedData` via Jackson `@JsonUnwrapped`.
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — voie d'appel social
 * strictement française ; la BE dispose d'un régime distinct, Code judiciaire
 * belge).</p>
 *
 *   dateNotificationJugement ← aiData.dateNotificationJugement (ISO YYYY-MM-DD)
 *
 * <p>F-218a — Procédure CPH avancée (P3 Travail FR).</p>
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface AppelCphPrefillInput {
  aiData?: Pick<TravailExtractedData, 'dateNotificationJugement'> | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: AppelCphPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/**
 * Date de notification du jugement CPH (ISO `YYYY-MM-DD`). Rejette toute
 * chaîne qui n'est pas une date ISO valide (format strict + valeur parsable).
 * Hors France → null.
 */
export function computeDateNotificationJugement(
  input: AppelCphPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateNotificationJugement;
  if (typeof v !== 'string') return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  const ts = Date.parse(v);
  if (Number.isNaN(ts)) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre le seul champ IA
 * `dateNotificationJugement`.
 */
export function computePrefillCount(input: AppelCphPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDateNotificationJugement(input) !== null) count++;
  return count;
}

export const AppelCphSectionPrefillRules = {
  computeDateNotificationJugement,
  computePrefillCount,
};
