import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  EtatSignalant,
  MotifSignalement,
} from '../../core/models/signalement-sis.model';

/**
 * SF-220-06 — Helper partagé pour {@link SignalementSisSectionComponent}
 * (F-IM-52-signalement-sis-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - signalementConnu : `aiData.signalementSisDetecte` (le pivot de détection vaut
 *    aussi signal métier « signalement connu » — F-246 pré-fill).
 *  - etatSignalant : `aiData.signalementSisEtatSignalant` (whitelist 3 codes).
 *  - motifSignalement : `aiData.signalementSisMotifSignalement` (whitelist 4 codes).
 *  - titreSejourValide : `aiData.signalementSisTitreSejourValide` (booléen).
 *
 * Champs NON pré-remplis (non extraits par le pipeline IA — non factualisables
 * de façon fiable depuis les pièces, saisie avocat) : dateSignalement.
 *
 * Total : 4 champs pre-remplissables.
 */

const ETAT_CODES: ReadonlySet<string> = new Set<string>([
  'FRANCE',
  'AUTRE_ETAT_MEMBRE',
  'INCONNU',
]);

const MOTIF_CODES: ReadonlySet<string> = new Set<string>([
  'IRTF',
  'MESURE_ELOIGNEMENT_ETRANGERE',
  'MENACE_ORDRE_PUBLIC',
  'AUTRE',
]);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const SignalementSisPrefillRules = {

  computeSignalementConnu(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.signalementSisDetecte;
    if (typeof v !== 'boolean') return null;
    // Le flag de détection vaut signal « signalement connu » uniquement quand true.
    return v ? true : null;
  },

  computeEtatSignalant(input: PrefillCountInput): EtatSignalant | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.signalementSisEtatSignalant;
    if (typeof v !== 'string' || !ETAT_CODES.has(v)) return null;
    return v as EtatSignalant;
  },

  computeMotifSignalement(input: PrefillCountInput): MotifSignalement | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.signalementSisMotifSignalement;
    if (typeof v !== 'string' || !MOTIF_CODES.has(v)) return null;
    return v as MotifSignalement;
  },

  computeTitreSejourValide(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.signalementSisTitreSejourValide;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeSignalementConnu(input) !== null) n++;
    if (this.computeEtatSignalant(input) !== null) n++;
    if (this.computeMotifSignalement(input) !== null) n++;
    if (this.computeTitreSejourValide(input) !== null) n++;
    return n;
  },
} as const;
