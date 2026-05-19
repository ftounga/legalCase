import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AesEtudiantNiveauEtudes } from '../../core/models/aes-etudiant.model';

/**
 * SF-236-02 / SF-246-18 — Helper partagé pour `AesEtudiantSectionComponent`
 * (F-IM-09-aes-etudiant) — FR mono-pays.
 *
 * SF-236-02 : 2 champs — dateEntreeFrance + dateDepotDemande.
 * SF-246-18 : 3 nouveaux champs — dureePresenceMois + anneesScolariteConsecutives
 *   + niveauEtudesActuel. Total : 5 champs pré-remplissables.
 *
 * Sources backend (ImmigrationExtractedData) :
 *   - dateEntreeFrance ← `aesDateEntreeFrance`
 *   - dureePresenceMois ← `aesDureePresenceMois` (calculé backend)
 *   - anneesScolariteConsecutives ← `aesAnneesScolariteConsecutives`
 *   - niveauEtudesActuel ← `aesNiveauEtudes` (whitelist 4 codes)
 *   - dateDepotDemande ← `dateDepotProcedure`
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const NIVEAUX_ETUDES_SET = new Set<string>([
  'LYCEE', 'BAC_PLUS_1_2', 'BAC_PLUS_3_4', 'BAC_PLUS_5_PLUS',
]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AesEtudiantPrefillRules = {
  ISO_DATE_RE,

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    // SF-246-18 : lecture sur champ typé `aesDateEntreeFrance`.
    const v = ai.aesDateEntreeFrance;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  /** SF-246-18 : durée de présence en mois (calculée backend, dérivée de aesDateEntreeFrance). */
  computeDureePresenceMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDureePresenceMois;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0) return null;
    return v;
  },

  /** SF-246-18 : années de scolarité consécutives en France (entier ≥ 0). */
  computeAnneesScolariteConsecutives(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesAnneesScolariteConsecutives;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > 20) return null;
    return v;
  },

  /** SF-246-18 : niveau d'études normalisé (whitelist 4 codes). */
  computeNiveauEtudes(input: PrefillCountInput): AesEtudiantNiveauEtudes | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesNiveauEtudes;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    return NIVEAUX_ETUDES_SET.has(upper) ? (upper as AesEtudiantNiveauEtudes) : null;
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dateDepotProcedure;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    // SF-246-18 : 3 nouveaux champs
    if (this.computeDureePresenceMois(input) !== null) n++;
    if (this.computeAnneesScolariteConsecutives(input) !== null) n++;
    if (this.computeNiveauEtudes(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
