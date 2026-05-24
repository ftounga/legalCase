/**
 * SF-212-26 — Helper partagé pour l'outil "Protection du lanceur d'alerte"
 * (F-DT-61). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 /
 * F-237 / F-246).
 *
 * Le pipeline IA (SF-212-25 backend) extrait un sous-objet
 * `lanceur_alerte_detail` (4 champs) projeté à plat sur
 * `TravailExtractedData`. FRANCE uniquement (`workspaceCountry === 'FRANCE'`
 * — loi Waserman 2022 spécifique au régime français ; la BE transpose la
 * même directive UE 2019/1937 par sa loi du 28/11/2022 avec un régime
 * d'indemnisation distinct).
 *
 *   natureSignalement          ← aiData.lanceurAlerteNatureSignalement
 *   procedureSignalement       ← aiData.lanceurAlerteProcedure
 *   mesureRepresailleDetectee  ← aiData.lanceurAlerteMesureRepresaille
 *   natureMesureRepresaille    ← aiData.lanceurAlerteNatureMesure
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import {
  LanceurAlerteNatureMesure,
  LanceurAlerteNatureSignalement,
  LanceurAlerteProcedure,
} from '../../core/models/lanceur-alerte-protection.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface LanceurAlerteProtectionPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'lanceurAlerteNatureSignalement'
    | 'lanceurAlerteProcedure'
    | 'lanceurAlerteMesureRepresaille'
    | 'lanceurAlerteNatureMesure'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: LanceurAlerteProtectionPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

const NATURE_SIGNALEMENT_VALUES: ReadonlySet<LanceurAlerteNatureSignalement> = new Set([
  'CRIME_DELIT',
  'VIOLATION_DROIT_UE',
  'MENACE_INTERET_GENERAL',
  'AUTRE',
]);

const PROCEDURE_VALUES: ReadonlySet<LanceurAlerteProcedure> = new Set([
  'INTERNE',
  'EXTERNE',
  'DIVULGATION_PUBLIQUE',
]);

const NATURE_MESURE_VALUES: ReadonlySet<LanceurAlerteNatureMesure> = new Set([
  'LICENCIEMENT',
  'SANCTION',
  'MESURE_DISCRIMINATOIRE',
  'AUTRE',
  'AUCUNE',
]);

/** Enum-validated string — `natureSignalement`. */
export function computeNatureSignalement(
  input: LanceurAlerteProtectionPrefillInput,
): LanceurAlerteNatureSignalement | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.lanceurAlerteNatureSignalement;
  if (typeof v !== 'string') return null;
  return NATURE_SIGNALEMENT_VALUES.has(v as LanceurAlerteNatureSignalement)
    ? (v as LanceurAlerteNatureSignalement)
    : null;
}

/** Enum-validated string — `procedureSignalement`. */
export function computeProcedure(
  input: LanceurAlerteProtectionPrefillInput,
): LanceurAlerteProcedure | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.lanceurAlerteProcedure;
  if (typeof v !== 'string') return null;
  return PROCEDURE_VALUES.has(v as LanceurAlerteProcedure)
    ? (v as LanceurAlerteProcedure)
    : null;
}

/** Booléen direct — `mesureRepresailleDetectee`. */
export function computeMesureRepresaille(
  input: LanceurAlerteProtectionPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.lanceurAlerteMesureRepresaille;
  return typeof v === 'boolean' ? v : null;
}

/** Enum-validated string — `natureMesureRepresaille`. */
export function computeNatureMesure(
  input: LanceurAlerteProtectionPrefillInput,
): LanceurAlerteNatureMesure | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.lanceurAlerteNatureMesure;
  if (typeof v !== 'string') return null;
  return NATURE_MESURE_VALUES.has(v as LanceurAlerteNatureMesure)
    ? (v as LanceurAlerteNatureMesure)
    : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre les 4 champs IA.
 */
export function computePrefillCount(
  input: LanceurAlerteProtectionPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeNatureSignalement(input) !== null) count++;
  if (computeProcedure(input) !== null) count++;
  if (computeMesureRepresaille(input) !== null) count++;
  if (computeNatureMesure(input) !== null) count++;
  return count;
}

export const LanceurAlerteProtectionSectionPrefillRules = {
  computeNatureSignalement,
  computeProcedure,
  computeMesureRepresaille,
  computeNatureMesure,
  computePrefillCount,
};
