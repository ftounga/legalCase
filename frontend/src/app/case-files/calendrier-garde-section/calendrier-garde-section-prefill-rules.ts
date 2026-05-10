/**
 * F-236 SF-236-02 — Helper partagé `CalendrierGardePrefillRules` (module pur).
 *
 * Singularité : F-FA-06 lit le mode de garde depuis
 * `synthesis.pensionAlimentaireEstimate.modeGardeDetaille` (et pas depuis
 * `aiData` direct), via l'`@Input() aiModeGardeDetaille`.
 *
 * Gate `workspaceCountry` : si le mode IA appartient à l'autre pays que le
 * workspace courant, **pas de pré-fill** (note informative seulement).
 */

export const MODES_FR: ReadonlySet<string> = new Set(['ALTERNEE_FR', 'DVH_CLASSIQUE_FR', 'DVH_ELARGI_FR']);
export const MODES_BE: ReadonlySet<string> = new Set(['ALTERNEE_BE', 'SECONDAIRE_BE', 'SECONDAIRE_ELARGI_BE']);
export const ALL_MODES: ReadonlySet<string> = new Set([...MODES_FR, ...MODES_BE]);

export interface CalendrierGardePrefillInput {
  /** Mode de garde IA, soit direct (@Input aiModeGardeDetaille) soit via synthesis. */
  aiModeGardeDetaille?: string | null;
  synthesis?: {
    pensionAlimentaireEstimate?: { modeGardeDetaille?: string | null } | null;
  } | null;
  workspaceCountry?: string;
  // Champs ignorés (contrat unifié).
  aiData?: unknown;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
}

/**
 * Résolution du mode IA : priorité au `@Input()` direct, fallback `synthesis`.
 * Normalisé en majuscules.
 */
export function resolveModeIa(input: CalendrierGardePrefillInput): string | null {
  const direct = input.aiModeGardeDetaille;
  const fromSynth = input.synthesis?.pensionAlimentaireEstimate?.modeGardeDetaille;
  const raw = (typeof direct === 'string' && direct)
    ? direct
    : (typeof fromSynth === 'string' && fromSynth)
      ? fromSynth
      : null;
  return raw ? raw.toUpperCase() : null;
}

/**
 * Détermine si le mode IA est compatible avec le pays du workspace.
 * Retourne le mode normalisé compatible, sinon null.
 */
export function computeGardeCode(input: CalendrierGardePrefillInput): string | null {
  const ai = resolveModeIa(input);
  if (!ai) return null;
  const wsFR = (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
  const isFR = MODES_FR.has(ai);
  const isBE = MODES_BE.has(ai);
  if (!isFR && !isBE) return null; // mode inconnu
  if ((wsFR && isFR) || (!wsFR && isBE)) return ai;
  return null; // mode de l'autre pays → pas de pré-fill
}

/**
 * Décide si une note informative "mode de l'autre pays" doit être affichée
 * (utile au runtime). Retourne la chaîne ou null.
 */
export function computeModeDetailleNote(input: CalendrierGardePrefillInput): string | null {
  const ai = resolveModeIa(input);
  if (!ai) return null;
  const wsFR = (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
  const isFR = MODES_FR.has(ai);
  const isBE = MODES_BE.has(ai);
  if (!isFR && !isBE) return null;
  if ((wsFR && isFR) || (!wsFR && isBE)) return null;
  return `Mode détecté : "${ai}" (autre pays). Vérifier que ce dossier est adapté.`;
}

export function computePrefillCount(input: CalendrierGardePrefillInput): number {
  return computeGardeCode(input) !== null ? 1 : 0;
}

export const CalendrierGardePrefillRules = {
  MODES_FR,
  MODES_BE,
  ALL_MODES,
  resolveModeIa,
  computeGardeCode,
  computeModeDetailleNote,
  computePrefillCount,
};
