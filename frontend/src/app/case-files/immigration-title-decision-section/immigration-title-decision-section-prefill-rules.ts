import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ImmigrationTitleDecisionSectionComponent` (F-IM-05-arbre-decisionnel-titre).
 *
 * Référence canonique du contrat (cf. `docs/features/F-236/contract-prefill-rules.md`).
 *
 * 3 champs pré-remplis :
 *   1. `nationaliteUe` (boolean) — depuis `aiData.nationaliteUe`.
 *   2. `motif` (enum 'TRAVAIL'|'ETUDES'|'FAMILLE'|'ASILE'|'AUTRE') —
 *      cascade triggerEvents[0] → typeTitreSejourCode → heuristique texte.
 *   3. `situationFamiliale` ('MARIE'|'PACS_COHABITATION'|'CELIBATAIRE') —
 *      uniquement si un triggerEvent l'impose explicitement.
 */

const CODE_TO_MOTIF: Record<string, string> = {
  VLS_TS_ETUDIANT: 'ETUDES',
  CARTE_A_ETUDES: 'ETUDES',
  CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE: 'ETUDES',
  VLS_TS_SALARIE: 'TRAVAIL',
  CST_SALARIE: 'TRAVAIL',
  CARTE_PLURIANNUELLE: 'TRAVAIL',
  CARTE_PLURIANNUELLE_SALARIE: 'TRAVAIL',
  CARTE_PLURIANNUELLE_PASSEPORT_TALENT: 'TRAVAIL',
  APS: 'TRAVAIL',
  CARTE_A_TRAVAIL: 'TRAVAIL',
  PERMIS_UNIQUE: 'TRAVAIL',
  CST_VPF: 'FAMILLE',
  CST_VPF_CONJOINT_FR: 'FAMILLE',
  CARTE_PLURIANNUELLE_VPF: 'FAMILLE',
  CARTE_A_FAMILLE: 'FAMILLE',
  RECEPISSE_ASILE: 'ASILE',
  ATTESTATION_IMMATRICULATION: 'ASILE',
  ANNEXE_15: 'ASILE',
};

/**
 * SF-IM-05-04 : mapping trigger_event → (motif, situationFamiliale).
 * Prioritaire sur CODE_TO_MOTIF car décrit la voie JURIDIQUE CIBLE.
 */
const TRIGGER_TO_CRITERIA: Record<string, { motif: string; situationFamiliale?: string }> = {
  MARIAGE_RESSORTISSANT_FR: { motif: 'FAMILLE', situationFamiliale: 'MARIE' },
  PACS_RESSORTISSANT_FR: { motif: 'FAMILLE', situationFamiliale: 'PACS_COHABITATION' },
  NAISSANCE_ENFANT_FR: { motif: 'FAMILLE' },
  REGROUPEMENT_FAMILIAL_AUTORISE: { motif: 'FAMILLE' },
  VIOLENCES_CONJUGALES_CONSTATEES: { motif: 'FAMILLE' },
  CDI_OBTENU_SALARIE: { motif: 'TRAVAIL' },
  DOCTORAT_OBTENU: { motif: 'TRAVAIL' },
  DEMANDE_ASILE_ACCORDEE_OFPRA: { motif: 'ASILE' },
  ENFANT_NE_FR_13ANS_PRESENCE: { motif: 'FAMILLE' },
};

const TEXT_KEYWORDS: ReadonlyArray<{ kw: readonly string[]; motif: string }> = [
  { kw: ['ETUDIANT', 'STUDENT'], motif: 'ETUDES' },
  { kw: ['SALARIE', 'TRAVAIL'], motif: 'TRAVAIL' },
  { kw: ['FAMILLE', 'VPF'], motif: 'FAMILLE' },
  { kw: ['ASILE', 'REFUGIE'], motif: 'ASILE' },
];

function normalize(s: string): string {
  return s.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function firstTriggerCode(input: PrefillCountInput): string | null {
  const triggers = Array.isArray(input.triggerEvents) ? input.triggerEvents : [];
  const t = triggers[0];
  return t?.eventCode ?? t?.event_code ?? null;
}

export const ImmigrationTitleDecisionPrefillRules = {
  CODE_TO_MOTIF,
  TRIGGER_TO_CRITERIA,
  TEXT_KEYWORDS,

  /** Renvoie le boolean ou null si pas pré-remplissable. */
  computeNationaliteUe(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai || typeof ai.nationaliteUe !== 'boolean') return null;
    return ai.nationaliteUe;
  },

  /** Cascade triggerEvents → typeTitreSejourCode → heuristique texte. */
  computeMotif(input: PrefillCountInput): string | null {
    const code = firstTriggerCode(input);
    if (code && TRIGGER_TO_CRITERIA[code]) {
      return TRIGGER_TO_CRITERIA[code].motif;
    }
    const ai = input.aiData;
    if (!ai) return null;
    const titreCode = typeof ai.typeTitreSejourCode === 'string'
      ? ai.typeTitreSejourCode.toUpperCase()
      : null;
    if (titreCode && CODE_TO_MOTIF[titreCode]) {
      return CODE_TO_MOTIF[titreCode];
    }
    if (typeof ai.typeTitreSejour === 'string' && ai.typeTitreSejour.length > 0) {
      const t = normalize(ai.typeTitreSejour);
      const match = TEXT_KEYWORDS.find(({ kw }) => kw.some(k => t.includes(k)));
      if (match) return match.motif;
    }
    return null;
  },

  /** Posée seulement si un triggerEvent l'impose explicitement. */
  computeSituationFamiliale(input: PrefillCountInput): string | null {
    const code = firstTriggerCode(input);
    if (code && TRIGGER_TO_CRITERIA[code]?.situationFamiliale) {
      return TRIGGER_TO_CRITERIA[code].situationFamiliale!;
    }
    return null;
  },

  /** Maître : compte les champs non-null. */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeNationaliteUe(input) !== null) n++;
    if (this.computeMotif(input) !== null) n++;
    if (this.computeSituationFamiliale(input) !== null) n++;
    return n;
  },
} as const;
