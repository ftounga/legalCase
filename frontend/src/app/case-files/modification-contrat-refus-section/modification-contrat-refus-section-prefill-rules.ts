/**
 * SF-212-12 — Helper partagé pour l'outil "Modification du contrat — refus
 * du salarié" (F-DT-70). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-11 backend) extrait un sous-objet
 * `modification_contrat_detail` (4 champs) projeté à plat sur
 * `TravailExtractedData`. FRANCE uniquement (`workspaceCountry === 'FRANCE'`
 * — Cass. soc. distinction modification du contrat / changement des conditions
 * de travail ; en BE, régime de l'acte équipollent à rupture distinct).
 *
 *   elementModifie                      ← aiData.modifContratElementModifie
 *   elementExplicitementContractualise  ← aiData.modifContratContractualise
 *   motifEconomique                     ← aiData.modifContratMotifEco
 *   notificationEcriteL1222_6           ← aiData.modifContratNotifEcrite
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ModificationContratElementModifie } from '../../core/models/modification-contrat-refus.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface ModificationContratRefusPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'modifContratElementModifie'
    | 'modifContratContractualise'
    | 'modifContratMotifEco'
    | 'modifContratNotifEcrite'
  > | null;
  workspaceCountry?: string;
}

/** Valeurs admises pour `elementModifie`. */
const ELEMENT_MODIFIE_VALUES: ReadonlySet<ModificationContratElementModifie> = new Set([
  'REMUNERATION',
  'QUALIFICATION',
  'DUREE_TRAVAIL',
  'LIEU_TRAVAIL',
  'HORAIRES',
  'TACHES',
  'AUTRE',
]);

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: ModificationContratRefusPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Élément modifié — chaîne contrainte aux 7 valeurs admises. */
export function computeElementModifie(
  input: ModificationContratRefusPrefillInput,
): ModificationContratElementModifie | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.modifContratElementModifie;
  if (typeof v !== 'string') return null;
  const upper = v.toUpperCase() as ModificationContratElementModifie;
  return ELEMENT_MODIFIE_VALUES.has(upper) ? upper : null;
}

/** Booléen direct — `elementExplicitementContractualise`. */
export function computeContractualise(
  input: ModificationContratRefusPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.modifContratContractualise;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `motifEconomique`. */
export function computeMotifEco(
  input: ModificationContratRefusPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.modifContratMotifEco;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `notificationEcriteL1222_6`. */
export function computeNotifEcrite(
  input: ModificationContratRefusPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.modifContratNotifEcrite;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 4 champs IA.
 */
export function computePrefillCount(input: ModificationContratRefusPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeElementModifie(input) !== null) count++;
  if (computeContractualise(input) !== null) count++;
  if (computeMotifEco(input) !== null) count++;
  if (computeNotifEcrite(input) !== null) count++;
  return count;
}

export const ModificationContratRefusSectionPrefillRules = {
  computeElementModifie,
  computeContractualise,
  computeMotifEco,
  computeNotifEcrite,
  computePrefillCount,
};
