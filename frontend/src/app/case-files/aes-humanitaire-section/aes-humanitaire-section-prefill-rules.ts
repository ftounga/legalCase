import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifHumanitaire } from '../../core/models/aes-humanitaire.model';

/**
 * SF-236-02 / SF-246-18 — Helper partagé pour `AesHumanitaireSectionComponent`
 * (F-IM-09-aes-humanitaire) — FR mono-pays.
 *
 * SF-236-02 : 2 champs — dateEntreeFrance + dateDepotDemande.
 * SF-246-18 : 1 nouveau champ — motifHumanitaireDominant.
 *   Total : 3 champs pré-remplissables.
 *
 * Sources backend (ImmigrationExtractedData) :
 *   - dateEntreeFrance ← `aesDateEntreeFrance` (champ typé, SF-246-18)
 *   - dateDepotDemande ← `dateDepotProcedure` (existant)
 *   - motifHumanitaire ← `aesMotifHumanitaire` (whitelist 6 codes, SF-246-18)
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const MOTIFS_HUMANITAIRES_SET = new Set<string>([
  'RISQUES_AU_RETOUR', 'ISOLEMENT_TOTAL', 'VICTIME_VIOLENCES',
  'VICTIME_TRAITE', 'SITUATION_MEDICALE_PRECAIRE_HORS_L425_9', 'AUTRE_HUMANITAIRE',
]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AesHumanitairePrefillRules = {
  ISO_DATE_RE,

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    // SF-246-18 : lecture sur champ typé `aesDateEntreeFrance`.
    const v = ai.aesDateEntreeFrance;
    if (typeof v !== 'string' || v.length < 10) return null;
    return v.substring(0, 10);
  },

  /**
   * dateDepotDemande : posée si dépôt ISO non futur ET (pas de
   * dateEntreeFrance OU depot >= dateEntreeFrance).
   */
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const depot = ai.dateDepotProcedure;
    if (typeof depot !== 'string' || !ISO_DATE_RE.test(depot)) return null;
    if (depot > todayIso()) return null;
    const entree = this.computeDateEntreeFrance(input);
    if (entree !== null && depot < entree) return null;
    return depot;
  },

  /** SF-246-18 : motif humanitaire dominant (whitelist 6 codes). */
  computeMotifHumanitaire(input: PrefillCountInput): MotifHumanitaire | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesMotifHumanitaire;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    return MOTIFS_HUMANITAIRES_SET.has(upper) ? (upper as MotifHumanitaire) : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    // SF-246-18 : 1 nouveau champ
    if (this.computeMotifHumanitaire(input) !== null) n++;
    return n;
  },
} as const;
