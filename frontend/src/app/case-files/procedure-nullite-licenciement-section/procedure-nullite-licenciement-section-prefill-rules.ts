/**
 * SF-246-01 — Helper partagé pour l'outil "Nullité de procédure de
 * licenciement" (F-DT-36). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA extrait désormais le sous-objet `procedure_licenciement_detection`
 * (cf. `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`). 8 champs du formulaire
 * sont pré-remplissables, FRANCE uniquement (`workspaceCountry === 'FRANCE'`) :
 *
 *   convocationEnvoyee            ← aiData.convocationEntretienDetectee (booléen)
 *   dateConvocationPresentee      ← aiData.dateConvocationEntretienDetectee (ISO)
 *   dateEntretienPrealable        ← aiData.dateEntretienPrealableDetectee (ISO)
 *   entretienTenu                 ← aiData.entretienPrealableTenuDetected (OUI/NON)
 *   dateNotificationLicenciement  ← aiData.dateLicenciement (ISO, déjà extrait)
 *   lettreLicenciementEcrite      ← aiData.lettreLicenciementEcriteDetectee (booléen)
 *   lettreMotivee                 ← aiData.lettreLicenciementMotiveeDetected (OUI/NON)
 *   motivationSuffisante          ← aiData.motivationLettreSuffisanteDetected (OUI/NON)
 *
 * Les `DetectedAnswer` sont traduits : `OUI` → true, `NON` → false,
 * `INCONNU` / absent → non pré-rempli.
 */

import { DetectedAnswer, TravailExtractedData } from '../../core/models/case-analysis.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface ProcedureNulliteLicenciementPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'convocationEntretienDetectee'
    | 'dateConvocationEntretienDetectee'
    | 'dateEntretienPrealableDetectee'
    | 'entretienPrealableTenuDetected'
    | 'dateLicenciement'
    | 'lettreLicenciementEcriteDetectee'
    | 'lettreLicenciementMotiveeDetected'
    | 'motivationLettreSuffisanteDetected'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: ProcedureNulliteLicenciementPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Traduit un `DetectedAnswer` en booléen : OUI → true, NON → false, sinon null. */
function detectedAnswerToBoolean(answer: DetectedAnswer | null | undefined): boolean | null {
  if (!answer || typeof answer.reponse !== 'string') return null;
  if (answer.reponse === 'OUI') return true;
  if (answer.reponse === 'NON') return false;
  return null;
}

/** `convocationEnvoyee` — booléen factuel direct. */
export function computeConvocationEnvoyee(
  input: ProcedureNulliteLicenciementPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.convocationEntretienDetectee;
  return typeof v === 'boolean' ? v : null;
}

/** `dateConvocationPresentee` — date ISO de présentation de la convocation. */
export function computeDateConvocation(
  input: ProcedureNulliteLicenciementPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateConvocationEntretienDetectee;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `dateEntretienPrealable` — date ISO de l'entretien préalable. */
export function computeDateEntretien(
  input: ProcedureNulliteLicenciementPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateEntretienPrealableDetectee;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `entretienTenu` — appréciation OUI/NON de la tenue de l'entretien. */
export function computeEntretienTenu(
  input: ProcedureNulliteLicenciementPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return detectedAnswerToBoolean(input.aiData?.entretienPrealableTenuDetected);
}

/** `dateNotificationLicenciement` — date ISO de notification (déjà extraite). */
export function computeDateNotification(
  input: ProcedureNulliteLicenciementPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `lettreLicenciementEcrite` — booléen factuel direct. */
export function computeLettreEcrite(
  input: ProcedureNulliteLicenciementPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.lettreLicenciementEcriteDetectee;
  return typeof v === 'boolean' ? v : null;
}

/** `lettreMotivee` — appréciation OUI/NON de la présence de motifs. */
export function computeLettreMotivee(
  input: ProcedureNulliteLicenciementPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return detectedAnswerToBoolean(input.aiData?.lettreLicenciementMotiveeDetected);
}

/** `motivationSuffisante` — appréciation OUI/NON du caractère suffisant. */
export function computeMotivationSuffisante(
  input: ProcedureNulliteLicenciementPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return detectedAnswerToBoolean(input.aiData?.motivationLettreSuffisanteDetected);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 */
export function computePrefillCount(
  input: ProcedureNulliteLicenciementPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeConvocationEnvoyee(input) !== null) count++;
  if (computeDateConvocation(input) !== null) count++;
  if (computeDateEntretien(input) !== null) count++;
  if (computeEntretienTenu(input) !== null) count++;
  if (computeDateNotification(input) !== null) count++;
  if (computeLettreEcrite(input) !== null) count++;
  if (computeLettreMotivee(input) !== null) count++;
  if (computeMotivationSuffisante(input) !== null) count++;
  return count;
}

export const ProcedureNulliteLicenciementSectionPrefillRules = {
  computeConvocationEnvoyee,
  computeDateConvocation,
  computeDateEntretien,
  computeEntretienTenu,
  computeDateNotification,
  computeLettreEcrite,
  computeLettreMotivee,
  computeMotivationSuffisante,
  computePrefillCount,
};
