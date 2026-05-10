import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ImmigrationWorkRightSectionComponent` (F-IM-07-droit-au-travail).
 *
 * Pattern gating pays clean : pré-fill activé uniquement quand le code IA
 * (`typeTitreSejourCode`) est compatible avec `workspaceCountry`.
 *
 * 1 champ pré-rempli : `titreType` (depuis `aiData.typeTitreSejourCode`).
 * Le champ `country` n'est pas compté — c'est juste un alignement de
 * cohérence interne avec `workspaceCountry`.
 */
export const FR_TITRE_CODES = new Set([
  'VLS_TS_ETUDIANT', 'VLS_TS_SALARIE', 'CST_SALARIE', 'CARTE_PLURIANNUELLE',
  // SF-IM-07-04 : sous-types explicites de la carte pluriannuelle.
  'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE', 'CARTE_PLURIANNUELLE_SALARIE',
  'CARTE_PLURIANNUELLE_PASSEPORT_TALENT', 'CARTE_PLURIANNUELLE_VPF',
  'CARTE_RESIDENT', 'APS', 'CST_VPF', 'CST_VPF_CONJOINT_FR', 'RECEPISSE_ASILE',
]);

export const BE_TITRE_CODES = new Set([
  'CARTE_A_TRAVAIL', 'CARTE_A_ETUDES', 'CARTE_A_FAMILLE', 'CARTE_B', 'CARTE_C',
  'PERMIS_UNIQUE', 'ANNEXE_15', 'ATTESTATION_IMMATRICULATION',
]);

export const ALL_TITRE_CODES = new Set([...FR_TITRE_CODES, ...BE_TITRE_CODES]);

export const ImmigrationWorkRightPrefillRules = {
  FR_TITRE_CODES,
  BE_TITRE_CODES,
  ALL_TITRE_CODES,

  /**
   * `titreType` : posé si `aiData.typeTitreSejourCode` est un code connu ET
   * compatible avec le pays du workspace (FR_TITRE_CODES si FRANCE,
   * BE_TITRE_CODES si BELGIQUE).
   */
  computeTitreType(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const code = typeof ai.typeTitreSejourCode === 'string'
      ? ai.typeTitreSejourCode.toUpperCase()
      : null;
    if (!code) return null;
    const isFR = FR_TITRE_CODES.has(code);
    const isBE = BE_TITRE_CODES.has(code);
    if (!isFR && !isBE) return null;
    const country = input.workspaceCountry ?? 'FRANCE';
    if ((country === 'FRANCE' && isFR) || (country === 'BELGIQUE' && isBE)) {
      return code;
    }
    return null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    return this.computeTitreType(input) !== null ? 1 : 0;
  },
} as const;
