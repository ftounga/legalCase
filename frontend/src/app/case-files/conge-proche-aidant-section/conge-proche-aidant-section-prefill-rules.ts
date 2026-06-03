import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CongeProcheAidantLien } from '../../core/models/conge-proche-aidant.model';

/**
 * SF-218-48 — Helper partagé pour {@link CongeProcheAidantSectionComponent}
 * (F-DT-79-conge-proche-aidant) — Travail FR mono-pays.
 *
 * Champ pré-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - lienPersonneAidee : `aiData['lien_personne_aidee']` — lien avec la personne
 *    aidée détecté par l'IA. Valeur normalisée contre l'enum
 *    {@link CongeProcheAidantLien} ; toute valeur inconnue est ignorée.
 *
 * Champs NON pré-remplis :
 *  - personneAideeResideFrance : information de résidence — saisie avocat (non
 *    factualisable de façon fiable depuis le sous-record consolidé).
 *  - dureeSouhaiteeMois : durée souhaitée — choix du salarié (non factualisable).
 *  - ajpaDemandee : demande d'AJPA auprès de la CAF — choix du salarié.
 *  - `conge_proche_aidant_detecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pré-remplissable.
 */

const LIENS: ReadonlyArray<CongeProcheAidantLien> = [
  'CONJOINT',
  'ASCENDANT',
  'DESCENDANT',
  'COLLATERAL',
  'SANS_LIEN_RESIDENCE_COMMUNE',
];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Normalise une valeur brute en lien connu, ou null. */
function normalizeLien(raw: unknown): CongeProcheAidantLien | null {
  if (typeof raw !== 'string') return null;
  const v = raw.trim().toUpperCase();
  return LIENS.includes(v as CongeProcheAidantLien) ? (v as CongeProcheAidantLien) : null;
}

export const CongeProcheAidantPrefillRules = {

  computeLienPersonneAidee(input: PrefillCountInput): CongeProcheAidantLien | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { lien_personne_aidee?: unknown } | null | undefined;
    if (!ai) return null;
    return normalizeLien(ai.lien_personne_aidee);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeLienPersonneAidee(input) !== null) n++;
    return n;
  },
} as const;
