import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeTrajet } from '../../core/models/temps-trajet-deplacement.model';

/**
 * SF-218-52 — Helper partagé pour {@link TempsTrajetDeplacementSectionComponent}
 * (F-DT-81-temps-trajet-deplacement) — Travail FR mono-pays.
 *
 * Champs pré-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - typeTrajet : `aiData['type_trajet']` — type de trajet détecté par l'IA.
 *    Retenu seulement s'il appartient à l'enum {@code TypeTrajet}.
 *  - tempsTrajetQuotidienMinutes : `aiData['temps_trajet_quotidien_minutes']` —
 *    temps de trajet quotidien détecté (minutes ≥ 0). Toute valeur négative ou
 *    non numérique est ignorée.
 *
 * Champs NON pré-remplis :
 *  - tempsTrajetNormalMinutes : temps de trajet « normal » de référence — relève
 *    d'une appréciation (distance habituelle / usage local), non factualisable
 *    de façon fiable depuis le sous-record consolidé.
 *  - contrepartiePrevueAccord : présence d'une contrepartie déjà prévue par
 *    accord — qualification juridique laissée à l'avocat (non factualisable).
 *  - `temps_trajet_detecte` est un FLAG de visibilité (déclenche l'apparition de
 *    l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pré-remplissables.
 */

const TYPE_TRAJET_VALUES: readonly TypeTrajet[] = [
  'DOMICILE_TRAVAIL_HABITUEL',
  'DOMICILE_CLIENT_DEPASSEMENT',
  'ITINERANT_SANS_LIEU_FIXE',
];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Normalise une valeur brute en type de trajet connu, ou null. */
function normalizeTypeTrajet(raw: unknown): TypeTrajet | null {
  if (typeof raw !== 'string') return null;
  const v = raw.trim().toUpperCase();
  return (TYPE_TRAJET_VALUES as readonly string[]).includes(v) ? (v as TypeTrajet) : null;
}

/** Normalise une valeur brute en minutes cohérentes (≥ 0), ou null. */
function normalizeMinutes(raw: unknown): number | null {
  const n = typeof raw === 'number' ? raw : typeof raw === 'string' ? Number(raw) : NaN;
  if (!Number.isFinite(n)) return null;
  if (n < 0) return null;
  return Math.round(n);
}

export const TempsTrajetDeplacementPrefillRules = {

  computeTypeTrajet(input: PrefillCountInput): TypeTrajet | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { type_trajet?: unknown } | null | undefined;
    if (!ai) return null;
    return normalizeTypeTrajet(ai.type_trajet);
  },

  computeTempsTrajetQuotidienMinutes(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { temps_trajet_quotidien_minutes?: unknown } | null | undefined;
    if (!ai) return null;
    return normalizeMinutes(ai.temps_trajet_quotidien_minutes);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeTypeTrajet(input) !== null) n++;
    if (this.computeTempsTrajetQuotidienMinutes(input) !== null) n++;
    return n;
  },
} as const;
