/**
 * F-236 SF-236-02 — Helper partagé `CalendrierGardePrefillRules` (module pur).
 *
 * Singularité : F-FA-06 lit le mode de garde depuis
 * `synthesis.pensionAlimentaireEstimate.modeGardeDetaille` (et pas depuis
 * `aiData` direct), via l'`@Input() aiModeGardeDetaille`.
 *
 * Gate `workspaceCountry` : si le mode IA appartient à l'autre pays que le
 * workspace courant, **pas de pré-fill** (note informative seulement).
 *
 * SF-246-10 : ajout du pré-fill `agesEnfantsDetectes` + `dateDebutCalendrierDetectee`
 * + `dateFinCalendrierDetectee` depuis `aiData` (FamilleExtractedData).
 * Le composant est branché sur `aiData` depuis cette SF — avant il l'ignorait.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

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
  /** SF-246-10 : données IA famille pour pré-fill âges + dates calendrier. */
  aiData?: Partial<FamilleExtractedData> | null;
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

/** SF-246-10 : âges des enfants depuis le champ réel `agesEnfantsDetectes`. */
export function computeAgesEnfants(input: CalendrierGardePrefillInput): number[] {
  const ages = input.aiData?.agesEnfantsDetectes;
  if (!Array.isArray(ages)) return [];
  return ages.filter(
    (n): n is number => typeof n === 'number' && Number.isInteger(n) && n >= 0 && n <= 25,
  );
}

/** SF-246-10 : date de début de la période du calendrier. */
export function computeDateDebutCalendrier(input: CalendrierGardePrefillInput): string | null {
  const v = input.aiData?.dateDebutCalendrierDetectee;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/** SF-246-10 : date de fin de la période du calendrier. */
export function computeDateFinCalendrier(input: CalendrierGardePrefillInput): string | null {
  const v = input.aiData?.dateFinCalendrierDetectee;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computePrefillCount(input: CalendrierGardePrefillInput): number {
  let n = 0;
  if (computeGardeCode(input) !== null) n++;
  if (computeAgesEnfants(input).length > 0) n++;
  if (computeDateDebutCalendrier(input) !== null) n++;
  if (computeDateFinCalendrier(input) !== null) n++;
  return n;
}

export const CalendrierGardePrefillRules = {
  MODES_FR,
  MODES_BE,
  ALL_MODES,
  resolveModeIa,
  computeGardeCode,
  computeModeDetailleNote,
  computeAgesEnfants,
  computeDateDebutCalendrier,
  computeDateFinCalendrier,
  computePrefillCount,
};
