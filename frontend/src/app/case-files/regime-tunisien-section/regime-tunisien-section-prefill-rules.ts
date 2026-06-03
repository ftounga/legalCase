import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CategorieRegimeTunisien } from '../../core/models/regime-tunisien.model';

/**
 * SF-220-01 — Helper partagé pour {@link RegimeTunisienSectionComponent}
 * (F-IM-47-regime-tunisien-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - categorie : `aiData.regimeTunisienCategorie` (whitelist 5 codes).
 *  - dureeSejourEnvisageeMois : `aiData.regimeTunisienDureeSejour` (entier ≥ 0).
 *  - titreEnCours : `aiData.regimeTunisienTitreEnCours` (booléen).
 *
 * Champ NON pré-rempli :
 *  - dejaResident : pas de champ IA dédié, saisi par l'avocat.
 *
 * Total : 3 champs pre-remplissables (sur 4 saisissables).
 */

export const CATEGORIE_REGIME_TUNISIEN_SET: ReadonlySet<CategorieRegimeTunisien> =
  new Set<CategorieRegimeTunisien>(['ETUDIANT', 'COMMERCANT', 'SALARIE', 'FAMILIAL', 'AUTRE']);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RegimeTunisienPrefillRules = {
  CATEGORIE_REGIME_TUNISIEN_SET,

  computeCategorie(input: PrefillCountInput): CategorieRegimeTunisien | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.regimeTunisienCategorie;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!CATEGORIE_REGIME_TUNISIEN_SET.has(upper as CategorieRegimeTunisien)) return null;
    return upper as CategorieRegimeTunisien;
  },

  computeDureeSejourEnvisageeMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.regimeTunisienDureeSejour;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return Math.trunc(v);
  },

  computeTitreEnCours(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.regimeTunisienTitreEnCours;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeCategorie(input) !== null) n++;
    if (this.computeDureeSejourEnvisageeMois(input) !== null) n++;
    if (this.computeTitreEnCours(input) !== null) n++;
    return n;
  },
} as const;
