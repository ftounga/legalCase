import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-24 — Helper partagé pour {@link CarteResidentSectionComponent}
 * (F-IM-36-carte-resident-l4261-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dureeSejourRegulierAnnees : `aiData.aesDureePresenceMois` (mois) converti
 *    en années entières (÷ 12, arrondi inférieur).
 *  - ressourcesMensuellesNettes : `aiData.carteResidentRessources` (number).
 *
 * Champs NON pré-remplis :
 *  - niveauIntegration / condamnationsPenalesGraves / typesTitresAnterieurs :
 *    pas de champ IA dédié fiable, saisis par l'avocat.
 *
 * Total : 2 champs pre-remplissables.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const CarteResidentPrefillRules = {

  computeDureeSejourRegulierAnnees(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const mois = (ai as { aesDureePresenceMois?: unknown }).aesDureePresenceMois;
    if (typeof mois !== 'number' || !Number.isFinite(mois) || mois < 0) return null;
    return Math.floor(mois / 12);
  },

  computeRessourcesMensuellesNettes(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const r = (ai as { carteResidentRessources?: unknown }).carteResidentRessources;
    if (typeof r !== 'number' || !Number.isFinite(r) || r < 0) return null;
    return r;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDureeSejourRegulierAnnees(input) !== null) n++;
    if (this.computeRessourcesMensuellesNettes(input) !== null) n++;
    return n;
  },
} as const;
