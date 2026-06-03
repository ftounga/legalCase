import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-50 — Helper partagé pour {@link RttAcquisitionSectionComponent}
 * (F-DT-80-rtt-acquisition) — Travail FR mono-pays.
 *
 * Champ pré-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - horaireHebdomadaireCollectif : `aiData['horaire_hebdomadaire_collectif']` —
 *    horaire hebdomadaire collectif détecté par l'IA. Retenu seulement s'il est
 *    cohérent avec le calcul JRTT (> 35 et ≤ 48). Toute valeur hors borne ou non
 *    numérique est ignorée.
 *
 * Champs NON pré-remplis :
 *  - accordRttPresent : présence d'un accord d'aménagement du temps de travail —
 *    qualification juridique laissée à l'avocat (non factualisable de façon
 *    fiable depuis le sous-record consolidé).
 *  - semainesTravailleesAn : nombre de semaines travaillées — défaut métier 47
 *    appliqué côté backend (non factualisable).
 *  - `rtt_acquisition_detectee` est un FLAG de visibilité (déclenche l'apparition
 *    de l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pré-remplissable.
 */

const HORAIRE_MIN = 35;
const HORAIRE_MAX = 48;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Normalise une valeur brute en horaire collectif cohérent (> 35 et ≤ 48), ou null. */
function normalizeHoraire(raw: unknown): number | null {
  const n = typeof raw === 'number' ? raw : typeof raw === 'string' ? Number(raw) : NaN;
  if (!Number.isFinite(n)) return null;
  if (n <= HORAIRE_MIN || n > HORAIRE_MAX) return null;
  return n;
}

export const RttAcquisitionPrefillRules = {

  computeHoraireHebdomadaireCollectif(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { horaire_hebdomadaire_collectif?: unknown } | null | undefined;
    if (!ai) return null;
    return normalizeHoraire(ai.horaire_hebdomadaire_collectif);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeHoraireHebdomadaireCollectif(input) !== null) n++;
    return n;
  },
} as const;
