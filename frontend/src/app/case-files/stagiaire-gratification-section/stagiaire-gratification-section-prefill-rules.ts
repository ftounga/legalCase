import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-22 — Helper partagé pour {@link StagiaireGratificationSectionComponent}
 * (F-DT-109-stagiaire-gratification-requalification) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateDebutStage : `aiData.dateDebutStage` (ISO YYYY-MM-DD) — date de début
 *    du stage détectée par l'IA dans la convention de stage.
 *  - dateFinStage : `aiData.dateFinStage` (ISO YYYY-MM-DD) — date de fin du
 *    stage détectée par l'IA.
 *
 * Champs NON pré-remplis :
 *  - nombreJoursPresence / gratificationMensuelleVersee / tauxHoraireConventionnel :
 *    montants / décomptes à saisir par l'avocat (non factualisables de façon
 *    fiable depuis les pièces).
 *  - missionsHorsProjetPedagogique / posteTravailPermanent : appréciations
 *    juridiques (saisie avocat).
 *  - `stageDetecte` est un FLAG de visibilité (déclenche l'apparition de l'outil
 *    via DecisionToolVisibilityService) — ce n'est PAS un champ du formulaire,
 *    il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 7 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function isoDateOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  return /^\d{4}-\d{2}-\d{2}/.test(v) ? v.substring(0, 10) : null;
}

export const StagiaireGratificationPrefillRules = {

  computeDateDebutStage(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateDebutStage);
  },

  computeDateFinStage(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateFinStage);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateDebutStage(input) !== null) n++;
    if (this.computeDateFinStage(input) !== null) n++;
    return n;
  },
} as const;
