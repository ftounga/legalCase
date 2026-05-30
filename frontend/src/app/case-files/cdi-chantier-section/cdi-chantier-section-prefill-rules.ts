import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { SecteurChantier } from '../../core/models/cdi-chantier.model';

/**
 * SF-218-26 — Helper partagé pour {@link CdiChantierSectionComponent}
 * (F-DT-37-licenciement-cdi-chantier) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateEntree : `aiData.dateEntree` (ISO YYYY-MM-DD) — début du contrat de
 *    chantier détecté par l'IA.
 *  - dateRupture : `aiData.dateLicenciement` ou, à défaut, `aiData.dateRuptureContrat`
 *    (ISO YYYY-MM-DD) — date de notification du licenciement détectée par l'IA.
 *  - secteur : `aiData.cdiChantierSecteur` (enum) — secteur détecté par l'IA.
 *
 * Champs NON pré-remplis :
 *  - fondementRecours / chantierAcheve / salaireMensuelMoyen /
 *    reclassementAutreChantierPropose : appréciations / saisie avocat.
 *  - `cdiChantierDetecte` est un FLAG de visibilité (déclenche l'apparition de
 *    l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 3 champs pre-remplissables (sur 7 saisissables).
 */

const SECTEURS_VALIDES: ReadonlySet<SecteurChantier> = new Set<SecteurChantier>([
  'BTP',
  'INGENIERIE',
  'AUTRE',
]);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function isoDateOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  return /^\d{4}-\d{2}-\d{2}/.test(v) ? v.substring(0, 10) : null;
}

export const CdiChantierPrefillRules = {

  computeDateEntree(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateEntree);
  },

  computeDateRupture(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateLicenciement) ?? isoDateOrNull(ai.dateRuptureContrat);
  },

  computeSecteur(input: PrefillCountInput): SecteurChantier | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const raw = ai.cdiChantierSecteur;
    return typeof raw === 'string' && SECTEURS_VALIDES.has(raw as SecteurChantier)
      ? (raw as SecteurChantier)
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntree(input) !== null) n++;
    if (this.computeDateRupture(input) !== null) n++;
    if (this.computeSecteur(input) !== null) n++;
    return n;
  },
} as const;
