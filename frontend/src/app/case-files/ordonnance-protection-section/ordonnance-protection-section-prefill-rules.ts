/**
 * F-236 SF-236-02 — Helper partagé `OrdonnanceProtectionPrefillRules`.
 *
 * 7 champs : dateRequete, violencesAlleguees (array filtré), preuvesViolences
 * (array filtré), dangerImmediat, presenceEnfants, logementCommun,
 * victimeFinanciairementDependante.
 *
 * Singularités :
 *  - les 4 booléens ne posent provenance IA QUE quand `detected === true`
 *    (le runtime applique aussi la valeur false, mais sans poser provenance) ;
 *    `computePrefillCount` ne compte que les **true** pour rester aligné sur
 *    le nombre de badges visuels effectivement affichés.
 *  - listes `violences` / `preuves` : compte 1 si au moins 1 code filtré
 *    valide après normalisation.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PreuveCode, ViolenceCode } from '../../core/models/ordonnance-protection.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export const VALID_VIOLENCE_CODES: ReadonlySet<string> = new Set<ViolenceCode>([
  'PHYSIQUES',
  'PSYCHOLOGIQUES',
  'SEXUELLES',
  'ECONOMIQUES',
  'MENACES_MORT',
]);

export const VALID_PREUVE_CODES: ReadonlySet<string> = new Set<PreuveCode>([
  'CONSTAT_HUISSIER',
  'MAIN_COURANTE',
  'CERTIFICAT_MEDICAL',
  'TEMOIGNAGES',
  'PHOTOS',
  'PLAINTE_DEPOSEE',
  'JUGEMENT_CORRECTIONNEL',
  'AUTRE',
]);

export interface OrdonnanceProtectionPrefillInput {
  aiData?: (Partial<FamilleExtractedData> & {
    dateRequeteOP?: string | null;
    violencesAllegueesDetectees?: readonly (string | null | undefined)[] | null;
    preuvesViolencesDetectees?: readonly (string | null | undefined)[] | null;
    dangerImmediatDetected?: boolean | null;
    presenceEnfantsDetected?: boolean | null;
    logementCommunDetected?: boolean | null;
    victimeFinanciairementDependanteDetected?: boolean | null;
  }) | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateRequete(input: OrdonnanceProtectionPrefillInput): string | null {
  const v = input.aiData?.dateRequeteOP;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeViolencesAllegueesCodes(
  input: OrdonnanceProtectionPrefillInput,
): ViolenceCode[] {
  const raw = input.aiData?.violencesAllegueesDetectees;
  if (!Array.isArray(raw)) return [];
  return raw
    .map(v => (typeof v === 'string' ? v.toUpperCase() : null))
    .filter((v): v is ViolenceCode => !!v && VALID_VIOLENCE_CODES.has(v));
}

export function computePreuvesViolencesCodes(
  input: OrdonnanceProtectionPrefillInput,
): PreuveCode[] {
  const raw = input.aiData?.preuvesViolencesDetectees;
  if (!Array.isArray(raw)) return [];
  return raw
    .map(p => (typeof p === 'string' ? p.toUpperCase() : null))
    .filter((p): p is PreuveCode => !!p && VALID_PREUVE_CODES.has(p));
}

/** Compte le champ uniquement si `detected === true` (aligné sur la pose de provenance IA). */
export function isDangerImmediatTrue(input: OrdonnanceProtectionPrefillInput): boolean {
  return input.aiData?.dangerImmediatDetected === true;
}
export function isPresenceEnfantsTrue(input: OrdonnanceProtectionPrefillInput): boolean {
  return input.aiData?.presenceEnfantsDetected === true;
}
export function isLogementCommunTrue(input: OrdonnanceProtectionPrefillInput): boolean {
  return input.aiData?.logementCommunDetected === true;
}
export function isVictimeFinanciairementDependanteTrue(
  input: OrdonnanceProtectionPrefillInput,
): boolean {
  return input.aiData?.victimeFinanciairementDependanteDetected === true;
}

export function computePrefillCount(input: OrdonnanceProtectionPrefillInput): number {
  let n = 0;
  if (computeDateRequete(input) !== null) n++;
  if (computeViolencesAllegueesCodes(input).length > 0) n++;
  if (computePreuvesViolencesCodes(input).length > 0) n++;
  if (isDangerImmediatTrue(input)) n++;
  if (isPresenceEnfantsTrue(input)) n++;
  if (isLogementCommunTrue(input)) n++;
  if (isVictimeFinanciairementDependanteTrue(input)) n++;
  return n;
}

export const OrdonnanceProtectionPrefillRules = {
  ISO_DATE_REGEX,
  VALID_VIOLENCE_CODES,
  VALID_PREUVE_CODES,
  computeDateRequete,
  computeViolencesAllegueesCodes,
  computePreuvesViolencesCodes,
  isDangerImmediatTrue,
  isPresenceEnfantsTrue,
  isLogementCommunTrue,
  isVictimeFinanciairementDependanteTrue,
  computePrefillCount,
};
