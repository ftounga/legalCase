import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-221-03 — Helper partagé pour {@link ResidenceLongueDureeUeBeSectionComponent}
 * (F-IM-55-residence-longue-duree-ue-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateDebutSejourLegal         : `aiData.rlueDateDebutSejour` (ISO yyyy-MM-dd)
 *  - ressourcesStablesSuffisantes : `aiData.rlueRessourcesSuffisantes` (booléen)
 *  - assuranceMaladie             : `aiData.rlueAssuranceMaladie` (booléen)
 *  - conditionIntegrationRemplie  : `aiData.rlueIntegrationRemplie` (booléen)
 *
 * Total : 4 champs pré-remplissables. Les 2 champs `sejourLegalIninterrompu`
 * et `absencesHorsUeExcessives` sont ASPIRATIONNELS — appréciations de continuité
 * non factualisées de façon fiable par l'IA (saisie / contrôle avocat F-246) — et
 * ne comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function normalizeIsoDate(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  // Validité calendaire stricte (ex. rejette 2026-02-30).
  const d = new Date(`${trimmed}T00:00:00Z`);
  if (isNaN(d.getTime())) return null;
  const iso = d.toISOString().slice(0, 10);
  return iso === trimmed ? trimmed : null;
}

function normalizeBoolean(v: unknown): boolean | null {
  if (typeof v === 'boolean') return v;
  return null;
}

export const ResidenceLongueDureeUeBePrefillRules = {
  computeDateDebut(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.rlueDateDebutSejour);
  },

  computeRessources(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.rlueRessourcesSuffisantes);
  },

  computeAssurance(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.rlueAssuranceMaladie);
  },

  computeIntegration(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.rlueIntegrationRemplie);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateDebut(input) !== null) n++;
    if (this.computeRessources(input) !== null) n++;
    if (this.computeAssurance(input) !== null) n++;
    if (this.computeIntegration(input) !== null) n++;
    return n;
  },
} as const;
