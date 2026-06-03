import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-46 — Helper partagé pour {@link CongeParentalEducationSectionComponent}
 * (F-DT-78-conge-parental-education) — Travail FR mono-pays.
 *
 * Champ pré-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - dateNaissanceOuAdoption : `aiData['date_naissance_ou_adoption']` (ISO
 *    yyyy-MM-dd) — date de naissance / d'arrivée de l'enfant détectée par l'IA.
 *
 * Champs NON pré-remplis :
 *  - ancienneteMois : ancienneté du salarié à la date de naissance / adoption —
 *    saisie avocat (aucune clé d'ancienneté en mois dans le sous-record consolidé
 *    `Sf218dDetail` ; non factualisable de façon fiable depuis ce record).
 *  - modalite : temps plein / temps partiel — choix du salarié (non factualisable).
 *  - nombreEnfants : nombre d'enfants concernés — saisie avocat (défaut 1).
 *  - `conge_parental_detecte` est un FLAG de visibilité (déclenche l'apparition
 *    de l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pré-remplissable.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Valide un format date ISO yyyy-MM-dd. Retourne la chaîne ou null. */
function normalizeDate(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const v = raw.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  return v;
}

export const CongeParentalEducationPrefillRules = {

  computeDateNaissanceOuAdoption(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { date_naissance_ou_adoption?: unknown } | null | undefined;
    if (!ai) return null;
    return normalizeDate(ai.date_naissance_ou_adoption);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNaissanceOuAdoption(input) !== null) n++;
    return n;
  },
} as const;
