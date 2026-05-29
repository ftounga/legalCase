import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-44 — Helper partagé pour
 * {@link AutorisationTravailEmployeurSectionComponent}
 * (F-IM-46-autorisation-travail-employeur-fr) — FRANCE mono-pays.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - nationaliteCandidat : `aiData.nationalite` (texte libre, ex. "Algérienne") —
 *    nationalité du candidat à embaucher, qui détermine si une autorisation de
 *    travail est requise (dispense pour les ressortissants UE/EEE/Suisse).
 *
 * Champs NON pré-remplis :
 *  - typeContrat : choix de l'employeur, non factualisable de façon fiable.
 *  - posteProposes : intitulé du poste, propre au recrutement de l'employeur.
 *  - dureeContratMois : durée du contrat envisagé (optionnel).
 *  - refusAutorisation / dateRefusAutorisation : éléments procéduraux postérieurs.
 *
 * Total : 1 champ pre-remplissable (sur 6 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AutorisationTravailEmployeurPrefillRules = {

  computeNationaliteCandidat(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.nationalite;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    return trimmed.length > 0 ? trimmed : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeNationaliteCandidat(input) !== null) n++;
    return n;
  },
} as const;
