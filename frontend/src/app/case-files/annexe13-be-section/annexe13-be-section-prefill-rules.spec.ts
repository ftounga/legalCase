import { Annexe13BePrefillRules as Rules, MOTIFS_OQT_WHITELIST } from './annexe13-be-section-prefill-rules';

describe('Annexe13BePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when delai is negative or non-integer', () => {
    expect(Rules.computePrefillCount({ aiData: { delaiDepartImposeJours: -1 } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { delaiDepartImposeJours: 3.5 } })).toBe(0);
  });

  it('returns 0 when motif is unknown', () => {
    expect(Rules.computePrefillCount({ aiData: { motifOqtCodeBe: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when only date is set', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationAnnexe13: '2026-04-01' } })).toBe(1);
  });

  it('returns 1 when only delai jours is set (>=0)', () => {
    expect(Rules.computePrefillCount({ aiData: { delaiDepartImposeJours: 0 } })).toBe(1);
    expect(Rules.computePrefillCount({ aiData: { delaiDepartImposeJours: 30 } })).toBe(1);
  });

  it('returns 1 when only transfertImminent boolean is set', () => {
    expect(Rules.computePrefillCount({ aiData: { transfertImminentDetected: false } })).toBe(1);
  });

  it('returns 1 when only motif valid (case sensitive)', () => {
    const motif = [...MOTIFS_OQT_WHITELIST][0];
    expect(Rules.computePrefillCount({ aiData: { motifOqtCodeBe: motif } })).toBe(1);
  });

  it('returns N=4 when all four sources alimente', () => {
    const motif = [...MOTIFS_OQT_WHITELIST][0];
    const input = {
      aiData: {
        dateNotificationAnnexe13: '2026-04-01',
        delaiDepartImposeJours: 7,
        motifOqtCodeBe: motif,
        transfertImminentDetected: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});
