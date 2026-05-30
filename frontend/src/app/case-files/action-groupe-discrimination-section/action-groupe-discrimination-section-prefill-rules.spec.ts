import { ActionGroupeDiscriminationPrefillRules as Rules } from './action-groupe-discrimination-section-prefill-rules';

describe('ActionGroupeDiscriminationPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { motifDiscrimination: 'SEXE', dateMiseEnDemeureDiscrimination: '2026-01-10' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeMotifDiscrimination(input)).toBeNull();
    expect(Rules.computeDateMiseEnDemeure(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only motifDiscrimination present', () => {
    const input = { aiData: { motifDiscrimination: 'HANDICAP' } };
    expect(Rules.computeMotifDiscrimination(input)).toBe('HANDICAP');
    expect(Rules.computeDateMiseEnDemeure(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only dateMiseEnDemeure present', () => {
    const input = { aiData: { dateMiseEnDemeureDiscrimination: '2025-11-30' } };
    expect(Rules.computeDateMiseEnDemeure(input)).toBe('2025-11-30');
    expect(Rules.computeMotifDiscrimination(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = { aiData: { motifDiscrimination: 'ORIGINE', dateMiseEnDemeureDiscrimination: '2025-06-01' } };
    expect(Rules.computeMotifDiscrimination(input)).toBe('ORIGINE');
    expect(Rules.computeDateMiseEnDemeure(input)).toBe('2025-06-01');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects an unknown motif value', () => {
    expect(Rules.computeMotifDiscrimination({ aiData: { motifDiscrimination: 'FOO' as never } })).toBeNull();
    expect(Rules.computeMotifDiscrimination({ aiData: { motifDiscrimination: 123 as never } })).toBeNull();
    expect(Rules.computeMotifDiscrimination({ aiData: { motifDiscrimination: null } })).toBeNull();
  });

  it('rejects a non-ISO / non-string date', () => {
    expect(Rules.computeDateMiseEnDemeure({ aiData: { dateMiseEnDemeureDiscrimination: '30/11/2025' } })).toBeNull();
    expect(Rules.computeDateMiseEnDemeure({ aiData: { dateMiseEnDemeureDiscrimination: '2025-13-40' } })).toBe('2025-13-40');
    expect(Rules.computeDateMiseEnDemeure({ aiData: { dateMiseEnDemeureDiscrimination: 20251130 as never } })).toBeNull();
    expect(Rules.computeDateMiseEnDemeure({ aiData: { dateMiseEnDemeureDiscrimination: null } })).toBeNull();
  });

  it('trims surrounding whitespace on the date', () => {
    expect(Rules.computeDateMiseEnDemeure({ aiData: { dateMiseEnDemeureDiscrimination: '  2025-06-01  ' } })).toBe('2025-06-01');
  });

  it('does NOT count actionGroupeDiscriminationEnvisagee flag alone (visibility trigger)', () => {
    const input = { aiData: { actionGroupeDiscriminationEnvisagee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { motifDiscrimination: 'AGE', dateMiseEnDemeureDiscrimination: '2025-07-01' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
