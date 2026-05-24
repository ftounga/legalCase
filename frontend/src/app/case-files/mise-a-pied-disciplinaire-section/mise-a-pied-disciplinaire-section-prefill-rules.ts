/**
 * SF-212-20 — Helper partagé pour l'outil "Mise à pied disciplinaire —
 * régularité" (F-DT-48). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-19 backend) extrait un sous-objet
 * `mise_a_pied_detail` (7 champs) projeté à plat sur `TravailExtractedData`.
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — la procédure
 * disciplinaire FR L. 1332-1 à L. 1332-4 CT n'a pas d'équivalent direct
 * côté BE).
 *
 *   natureMiseAPied               ← aiData.mapDisciplinaireNature
 *   procedureEntretienSuivie      ← aiData.mapDisciplinaireProcedureSuivie
 *   prescriptionFauteVerifiee     ← aiData.mapDisciplinairePrescriptionFaute
 *   dureeDefiniedansRI            ← aiData.mapDisciplinaireDureeRi
 *   dureeJours                    ← aiData.mapDisciplinaireDureeJours
 *   salaireSuspendu               ← aiData.mapDisciplinaireSalaireSuspendu
 *   sancionsAnterieuresMemesFaits ← aiData.mapDisciplinaireSanctionsAnterieures
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { MapNatureMiseAPied } from '../../core/models/mise-a-pied-disciplinaire.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface MiseAPiedDisciplinairePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'mapDisciplinaireNature'
    | 'mapDisciplinaireProcedureSuivie'
    | 'mapDisciplinairePrescriptionFaute'
    | 'mapDisciplinaireDureeRi'
    | 'mapDisciplinaireDureeJours'
    | 'mapDisciplinaireSalaireSuspendu'
    | 'mapDisciplinaireSanctionsAnterieures'
  > | null;
  workspaceCountry?: string;
}

/** Borne supérieure raisonnable de la durée en jours. */
const DUREE_MAX_JOURS = 365;

/** Valeurs autorisées pour la nature de la mise à pied. */
const NATURE_VALEURS: ReadonlySet<MapNatureMiseAPied> = new Set([
  'DISCIPLINAIRE',
  'CONSERVATOIRE',
  'INCONNUE',
]);

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: MiseAPiedDisciplinairePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Enum — `natureMiseAPied`. */
export function computeNature(
  input: MiseAPiedDisciplinairePrefillInput,
): MapNatureMiseAPied | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinaireNature;
  if (typeof v !== 'string') return null;
  return NATURE_VALEURS.has(v as MapNatureMiseAPied)
    ? (v as MapNatureMiseAPied)
    : null;
}

/** Booléen direct — `procedureEntretienSuivie`. */
export function computeProcedureSuivie(
  input: MiseAPiedDisciplinairePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinaireProcedureSuivie;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `prescriptionFauteVerifiee`. */
export function computePrescription(
  input: MiseAPiedDisciplinairePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinairePrescriptionFaute;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `dureeDefiniedansRI`. */
export function computeDureeRi(
  input: MiseAPiedDisciplinairePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinaireDureeRi;
  return typeof v === 'boolean' ? v : null;
}

/** Entier — `dureeJours`. Borné [0, 365] et integer fini. */
export function computeDureeJours(
  input: MiseAPiedDisciplinairePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinaireDureeJours;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > DUREE_MAX_JOURS) return null;
  return Math.trunc(v);
}

/** Booléen direct — `salaireSuspendu`. */
export function computeSalaireSuspendu(
  input: MiseAPiedDisciplinairePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinaireSalaireSuspendu;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `sancionsAnterieuresMemesFaits`. */
export function computeSanctionsAnterieures(
  input: MiseAPiedDisciplinairePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.mapDisciplinaireSanctionsAnterieures;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 7 champs IA.
 */
export function computePrefillCount(input: MiseAPiedDisciplinairePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeNature(input) !== null) count++;
  if (computeProcedureSuivie(input) !== null) count++;
  if (computePrescription(input) !== null) count++;
  if (computeDureeRi(input) !== null) count++;
  if (computeDureeJours(input) !== null) count++;
  if (computeSalaireSuspendu(input) !== null) count++;
  if (computeSanctionsAnterieures(input) !== null) count++;
  return count;
}

export const MiseAPiedDisciplinaireSectionPrefillRules = {
  computeNature,
  computeProcedureSuivie,
  computePrescription,
  computeDureeRi,
  computeDureeJours,
  computeSalaireSuspendu,
  computeSanctionsAnterieures,
  computePrefillCount,
};
