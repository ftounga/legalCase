import { ImmigrationTitleDecisionPrefillRules as Rules } from './immigration-title-decision-section-prefill-rules';

describe('ImmigrationTitleDecisionPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData and no triggerEvents', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null, triggerEvents: [] })).toBe(0);
  });

  it('returns 0 when aiData is empty object', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  // ── Cas M (partiel) ───────────────────────────────────────────────────
  it('returns 1 when only nationaliteUe is set', () => {
    const input = { aiData: { nationaliteUe: true } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeNationaliteUe(input)).toBe(true);
    expect(Rules.computeMotif(input)).toBeNull();
    expect(Rules.computeSituationFamiliale(input)).toBeNull();
  });

  it('returns 1 when only typeTitreSejourCode is set (CODE_TO_MOTIF cascade)', () => {
    const input = { aiData: { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeMotif(input)).toBe('ETUDES');
  });

  it('returns 1 when only typeTitreSejour string heuristic matches', () => {
    const input = { aiData: { typeTitreSejour: 'Visa long séjour SALARIE' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeMotif(input)).toBe('TRAVAIL');
  });

  it('triggerEvents prend priorité sur typeTitreSejourCode', () => {
    const input = {
      aiData: { typeTitreSejourCode: 'VLS_TS_SALARIE' },
      triggerEvents: [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' }],
    };
    // trigger motif=FAMILLE + situationFamiliale=MARIE = 2 fields
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeMotif(input)).toBe('FAMILLE');
    expect(Rules.computeSituationFamiliale(input)).toBe('MARIE');
  });

  it('event_code legacy snake_case fonctionne aussi', () => {
    const input = {
      aiData: {},
      triggerEvents: [{ event_code: 'PACS_RESSORTISSANT_FR' }],
    };
    expect(Rules.computeMotif(input)).toBe('FAMILLE');
    expect(Rules.computeSituationFamiliale(input)).toBe('PACS_COHABITATION');
  });

  // ── Cas N (nominal) ───────────────────────────────────────────────────
  it('returns N=3 (max théo.) when triggerEvent + nationaliteUe', () => {
    const input = {
      aiData: { nationaliteUe: false, typeTitreSejourCode: 'VLS_TS_SALARIE' },
      triggerEvents: [{ eventCode: 'MARIAGE_RESSORTISSANT_FR' }],
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
    expect(Rules.computeNationaliteUe(input)).toBe(false);
    expect(Rules.computeMotif(input)).toBe('FAMILLE');
    expect(Rules.computeSituationFamiliale(input)).toBe('MARIE');
  });

  it('returns 2 when nationaliteUe + motif from typeTitreSejourCode (pas de situationFamiliale)', () => {
    const input = {
      aiData: { nationaliteUe: true, typeTitreSejourCode: 'CARTE_PLURIANNUELLE_PASSEPORT_TALENT' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeMotif(input)).toBe('TRAVAIL');
    expect(Rules.computeSituationFamiliale(input)).toBeNull();
  });
});
