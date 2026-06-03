import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-38 — Helper partagé pour {@link RttMonetisationSectionComponent}
 * (F-DT-51-rtt-monetisation) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - nombreJoursRttRenonces : `aiData['nombre_jours_rtt_renonces']` (entier > 0)
 *    — nombre de jours de RTT renoncés détecté par l'IA.
 *  - salaireJournalierBrut : `aiData['salaire_journalier_brut']` (> 0) — salaire
 *    journalier brut de référence détecté par l'IA.
 *
 * Champs NON pré-remplis :
 *  - tauxMajorationConventionnel : taux conventionnel issu de la convention
 *    collective / accord d'entreprise — non factualisable de façon fiable depuis
 *    les pièces (saisie avocat, défaut 25 % côté backend).
 *  - dateRenonciation : date de la demande de renonciation — saisie avocat
 *    (conditionne l'appartenance à la fenêtre 01/01/2022 → 31/12/2026).
 *  - `rtt_monetisation_detectee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function positiveIntOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return Math.trunc(v);
}

function positiveNumberOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return v;
}

export const RttMonetisationPrefillRules = {

  computeNombreJoursRttRenonces(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { nombre_jours_rtt_renonces?: unknown } | null | undefined;
    if (!ai) return null;
    return positiveIntOrNull(ai.nombre_jours_rtt_renonces);
  },

  computeSalaireJournalierBrut(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { salaire_journalier_brut?: unknown } | null | undefined;
    if (!ai) return null;
    return positiveNumberOrNull(ai.salaire_journalier_brut);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeNombreJoursRttRenonces(input) !== null) n++;
    if (this.computeSalaireJournalierBrut(input) !== null) n++;
    return n;
  },
} as const;
