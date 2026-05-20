/**
 * SF-246-28 — Helper partagé pour l'outil "Autorité parentale (Belgique)"
 * (`autorite-parentale-be`). Module pur — runtime et static appellent les
 * mêmes fonctions (contrat F-236 SF-236-01 / F-237).
 *
 * SF-246-28 : levée de `PREFILL_COUNT_ALWAYS_ZERO`. Le pipeline IA extrait
 * désormais `modeHebergementPrincipalBeDetecte` (whitelist 3 codes) depuis le
 * sous-objet `famille_be_detection_v2`. Un champ formulaire est pré-rempli si
 * ce champ est non null.
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Whitelist des codes de mode d'hébergement acceptés par le formulaire. */
const MODE_HEBERGEMENT_BE_CODES = new Set([
  'HEBERGEMENT_EGALITAIRE',
  'HEBERGEMENT_PRINCIPAL_UN_PARENT',
  'HEBERGEMENT_NON_FIXE',
]);

export interface AutoriteParentaleBePrefillInput {
  aiData?: FamilleExtractedData | null | unknown;
}

/**
 * Retourne le mode d'hébergement principal détecté par l'IA si le code est
 * dans la whitelist, null sinon.
 */
export function computeModeHebergement(
  input: AutoriteParentaleBePrefillInput,
): string | null {
  const ai = input.aiData as FamilleExtractedData | null | undefined;
  if (!ai) return null;
  const code = ai.modeHebergementPrincipalBeDetecte;
  if (!code || !MODE_HEBERGEMENT_BE_CODES.has(code)) return null;
  return code;
}

/**
 * Compte le nombre de champs pré-remplis par l'IA pour le badge du panneau.
 * SF-246-28 : 1 champ maximum (`modeHebergementPrincipal`).
 */
export function computePrefillCount(
  input: AutoriteParentaleBePrefillInput,
): number {
  return computeModeHebergement(input) != null ? 1 : 0;
}

export const AutoriteParentaleBeSectionPrefillRules = {
  computePrefillCount,
  computeModeHebergement,
};
