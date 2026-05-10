/**
 * F-236 SF-236-02 — Helper partagé `MajeursProtegesPrefillRules`.
 * 5 champs : regimeProtectionDemande, altertationFacultesMentales (bool),
 * altertationFacultesPhysiques (bool), certificatMedicalCirconstancie (bool),
 * dateCertificatMedical (ISO).
 *
 * Singularités runtime :
 * - les 3 booléens posent IA uniquement si IA detected = true (mais set la
 *   valeur dans tous les cas si la condition d'écrasement est remplie).
 *   Pour le compteur, ces 3 sont comptés UNIQUEMENT si true (parité avec
 *   la pose de provenance / badge visible).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import {
  ActeEnvisage,
  DemandeurFamilial,
  FormeMandatProtection,
  RegimeProtection,
} from '../../core/models/majeurs-proteges.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeProtection>([
  'SAUVEGARDE_JUSTICE', 'HABILITATION_FAMILIALE', 'CURATELLE_SIMPLE',
  'CURATELLE_RENFORCEE', 'TUTELLE', 'MANDAT_PROTECTION_FUTURE',
]);
export const VALID_DEMANDEURS: ReadonlySet<string> = new Set<DemandeurFamilial>([
  'CONJOINT', 'ENFANT_MAJEUR', 'PARENT', 'FRERE_SOEUR', 'TIERS_PROCHE', 'MINISTERE_PUBLIC',
]);
export const VALID_ACTES: ReadonlySet<string> = new Set<ActeEnvisage>([
  'GESTION_PATRIMOINE', 'DECISIONS_LOGEMENT', 'DECISIONS_SANTE',
  'DECISIONS_FAMILIALES', 'ACTES_ETAT_CIVIL', 'AUTRE',
]);
export const VALID_FORMES_MANDAT: ReadonlySet<string> = new Set<FormeMandatProtection>([
  'NOTARIE', 'SOUS_SEING_PRIVE',
]);

type Ai = Partial<FamilleExtractedData> & {
  regimeProtectionDemande?: string | null;
  altertationFacultesMentales?: boolean | null;
  altertationFacultesPhysiques?: boolean | null;
  certificatMedicalCirconstancieDetected?: boolean | null;
  dateCertificatMedicalDetected?: string | null;
  consentementPersonneAProtegerDetected?: boolean | null;
  demandeurFamilialDetected?: string | null;
  actesEnvisagesDetected?: (string | null)[] | null;
  incapaciteGestionQuotidienneDetected?: boolean | null;
  altertationGraveDetected?: boolean | null;
  mandatPrealableSigneDetected?: boolean | null;
  formeMandatProtectionDetected?: string | null;
};

export interface MajeursProtegesPrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeRegimeDemande(input: MajeursProtegesPrefillInput): RegimeProtection | null {
  const raw = input.aiData?.regimeProtectionDemande;
  if (typeof raw !== 'string' || !raw) return null;
  const upper = raw.toUpperCase();
  return VALID_REGIMES.has(upper) ? (upper as RegimeProtection) : null;
}

export function isAlterationMentalesTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.altertationFacultesMentales === true;
}
export function isAlterationPhysiquesTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.altertationFacultesPhysiques === true;
}
export function isCertificatMedicalTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.certificatMedicalCirconstancieDetected === true;
}

export function computeDateCertificat(input: MajeursProtegesPrefillInput): string | null {
  const v = input.aiData?.dateCertificatMedicalDetected;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function isConsentementPersonneTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.consentementPersonneAProtegerDetected === true;
}

export function computeDemandeurFamilial(input: MajeursProtegesPrefillInput): DemandeurFamilial | null {
  const raw = input.aiData?.demandeurFamilialDetected;
  if (typeof raw !== 'string' || !raw) return null;
  const u = raw.toUpperCase();
  return VALID_DEMANDEURS.has(u) ? (u as DemandeurFamilial) : null;
}

export function computeActesEnvisages(input: MajeursProtegesPrefillInput): ActeEnvisage[] {
  const raw = input.aiData?.actesEnvisagesDetected;
  if (!Array.isArray(raw)) return [];
  return raw
    .map(a => (typeof a === 'string' ? a.toUpperCase() : null))
    .filter((a): a is ActeEnvisage => !!a && VALID_ACTES.has(a));
}

export function isIncapaciteGestionTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.incapaciteGestionQuotidienneDetected === true;
}
export function isAltertationGraveTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.altertationGraveDetected === true;
}
export function isMandatPrealableSigneTrue(input: MajeursProtegesPrefillInput): boolean {
  return input.aiData?.mandatPrealableSigneDetected === true;
}

export function computeFormeMandat(input: MajeursProtegesPrefillInput): FormeMandatProtection | null {
  const raw = input.aiData?.formeMandatProtectionDetected;
  if (typeof raw !== 'string' || !raw) return null;
  const u = raw.toUpperCase();
  return VALID_FORMES_MANDAT.has(u) ? (u as FormeMandatProtection) : null;
}

export function computePrefillCount(input: MajeursProtegesPrefillInput): number {
  let n = 0;
  if (computeRegimeDemande(input) !== null) n++;
  if (isAlterationMentalesTrue(input)) n++;
  if (isAlterationPhysiquesTrue(input)) n++;
  if (isCertificatMedicalTrue(input)) n++;
  if (computeDateCertificat(input) !== null) n++;
  if (isConsentementPersonneTrue(input)) n++;
  if (computeDemandeurFamilial(input) !== null) n++;
  if (computeActesEnvisages(input).length > 0) n++;
  if (isIncapaciteGestionTrue(input)) n++;
  if (isAltertationGraveTrue(input)) n++;
  if (isMandatPrealableSigneTrue(input)) n++;
  if (computeFormeMandat(input) !== null) n++;
  return n;
}

export const MajeursProtegesPrefillRules = {
  ISO_DATE_REGEX,
  VALID_REGIMES,
  VALID_DEMANDEURS,
  VALID_ACTES,
  VALID_FORMES_MANDAT,
  computeRegimeDemande,
  isAlterationMentalesTrue,
  isAlterationPhysiquesTrue,
  isCertificatMedicalTrue,
  computeDateCertificat,
  isConsentementPersonneTrue,
  computeDemandeurFamilial,
  computeActesEnvisages,
  isIncapaciteGestionTrue,
  isAltertationGraveTrue,
  isMandatPrealableSigneTrue,
  computeFormeMandat,
  computePrefillCount,
};
