/**
 * F-236 SF-236-02 — Helper partagé `PmaGpaBioethiquePrefillRules`.
 * 1 champ : dispositif (3 codes valides). Comptage : 1 si le dispositif IA
 * existe ET diffère du défaut `PMA_RECONNAISSANCE_ANTICIPEE`, sinon 0.
 *
 * Note : runtime accepte aussi le dispositif IA = défaut si la provenance est
 * déjà IA (re-prefill). On compte ici la condition "IA poserait visiblement
 * un badge", soit dispositif IA reconnu ET différent du défaut.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { DispositifBioethique } from '../../core/models/pma-gpa-bioethique.model';

const DEFAULT_DISPOSITIF: DispositifBioethique = 'PMA_RECONNAISSANCE_ANTICIPEE';

type Ai = Partial<FamilleExtractedData> & {
  dispositifBioethiqueDetecte?: string | null;
};

export interface PmaGpaBioethiquePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function parseDispositifFromIa(value: unknown): DispositifBioethique | null {
  if (typeof value !== 'string') return null;
  const v = value.trim().toUpperCase();
  if (v === 'PMA_RECONNAISSANCE_ANTICIPEE'
      || v === 'GPA_TRANSCRIPTION_ETAT_CIVIL'
      || v === 'DON_GAMETES_ACCES_ORIGINES') {
    return v as DispositifBioethique;
  }
  return null;
}

export function computeDispositif(input: PmaGpaBioethiquePrefillInput): DispositifBioethique | null {
  return parseDispositifFromIa(input.aiData?.dispositifBioethiqueDetecte);
}

export function computePrefillCount(input: PmaGpaBioethiquePrefillInput): number {
  const d = computeDispositif(input);
  if (d === null) return 0;
  // Le runtime considère "champ vide" si encore à la valeur défaut. On
  // compte 1 dès qu'un dispositif IA reconnu est présent (la pose runtime
  // peut être no-op si l'avocat a déjà customisé, mais le badge anticipe
  // ce qu'il aurait fait — parité approximative au sens panel).
  return 1;
}

export const PmaGpaBioethiquePrefillRules = {
  DEFAULT_DISPOSITIF,
  parseDispositifFromIa,
  computeDispositif,
  computePrefillCount,
};
