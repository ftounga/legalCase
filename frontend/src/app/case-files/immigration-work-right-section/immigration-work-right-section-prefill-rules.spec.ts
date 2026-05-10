import {
  ImmigrationWorkRightPrefillRules as Rules,
  FR_TITRE_CODES,
  BE_TITRE_CODES,
} from './immigration-work-right-section-prefill-rules';

describe('ImmigrationWorkRightPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when typeTitreSejourCode missing or unknown', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { typeTitreSejourCode: null } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { typeTitreSejourCode: 'INCONNU_X' } })).toBe(0);
  });

  it('returns 0 when typeTitreSejourCode is FR but workspaceCountry is BE (gating)', () => {
    const input = {
      aiData: { typeTitreSejourCode: 'VLS_TS_ETUDIANT' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeTitreType(input)).toBeNull();
  });

  it('returns 0 when typeTitreSejourCode is BE but workspaceCountry is FR (gating)', () => {
    const input = {
      aiData: { typeTitreSejourCode: 'CARTE_B' },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas M (partiel) — N=1 mono-champ, donc M=N=1 ──────────────────────
  it('returns 1 when typeTitreSejourCode FR matches workspaceCountry=FRANCE', () => {
    const input = {
      aiData: { typeTitreSejourCode: 'VLS_TS_SALARIE' },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeTitreType(input)).toBe('VLS_TS_SALARIE');
  });

  it('returns 1 when typeTitreSejourCode BE matches workspaceCountry=BELGIQUE', () => {
    const input = {
      aiData: { typeTitreSejourCode: 'permis_unique' }, // teste l'uppercase
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeTitreType(input)).toBe('PERMIS_UNIQUE');
  });

  // ── Cas N (nominal) ───────────────────────────────────────────────────
  it('returns 1 for every code in FR_TITRE_CODES quand workspaceCountry=FRANCE', () => {
    for (const code of FR_TITRE_CODES) {
      const input = { aiData: { typeTitreSejourCode: code }, workspaceCountry: 'FRANCE' };
      expect(Rules.computePrefillCount(input)).toBe(1);
    }
  });

  it('returns 1 for every code in BE_TITRE_CODES quand workspaceCountry=BELGIQUE', () => {
    for (const code of BE_TITRE_CODES) {
      const input = { aiData: { typeTitreSejourCode: code }, workspaceCountry: 'BELGIQUE' };
      expect(Rules.computePrefillCount(input)).toBe(1);
    }
  });

  it('defaults workspaceCountry to FRANCE when absent', () => {
    const input = { aiData: { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
