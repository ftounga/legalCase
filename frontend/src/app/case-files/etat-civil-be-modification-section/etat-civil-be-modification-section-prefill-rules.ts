/**
 * SF-223-09 — Helper partagé pour l'outil "Modification de l'état civil
 * (Belgique)" (`etat-civil-be-modification`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Trois champs sont factualisables depuis
 * le sous-objet IA `etat_civil_modification_be_detection` (consolidé dans
 * `Sf223Detail`, @JsonUnwrapped → clés camelCase plates sur
 * `familleExtractedData`) :
 *
 *  - `etatCivilModificationTypeDetecte` (CHANGEMENT_NOM / CHANGEMENT_PRENOM /
 *    CHANGEMENT_SEXE) → champ `typeModification` ;
 *  - `etatCivilModificationMajeurDetecte` (boolean) → champ `personneMajeure` ;
 *  - `etatCivilModificationNationaliteResidentDetectee` (boolean) → champ
 *    `nationaliteBelgeOuResident`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0 à
 * 3). Les booleans de fond propres à chaque branche (motif sérieux du nom,
 * gratuité de la 1re demande de prénom, seconde déclaration de sexe, consentement
 * des représentants d'un mineur) ne sont pas pré-remplis (appréciation
 * juridique / situation à confirmer). Le static `getPrefillCount` du composant
 * délègue à ce helper (parité runtime/static — garde-fou `prefill-count-integrity`).
 */

interface EtatCivilModificationAiShape {
  etatCivilModificationTypeDetecte?: string | null;
  etatCivilModificationMajeurDetecte?: boolean | null;
  etatCivilModificationNationaliteResidentDetectee?: boolean | null;
}

export interface EtatCivilBeModificationPrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const TYPE_VALUES = new Set(['CHANGEMENT_NOM', 'CHANGEMENT_PRENOM', 'CHANGEMENT_SEXE']);

/**
 * Nombre de champs pré-remplis par l'IA (0 à 3) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: EtatCivilBeModificationPrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as EtatCivilModificationAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.etatCivilModificationTypeDetecte === 'string'
      && TYPE_VALUES.has(ai.etatCivilModificationTypeDetecte)) {
    count += 1;
  }
  if (typeof ai.etatCivilModificationMajeurDetecte === 'boolean') {
    count += 1;
  }
  if (typeof ai.etatCivilModificationNationaliteResidentDetectee === 'boolean') {
    count += 1;
  }
  return count;
}

export const EtatCivilBeModificationSectionPrefillRules = {
  computePrefillCount,
};
