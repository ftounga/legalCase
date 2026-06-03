import { DecheanceNationalitePrefillRules as Rules } from './decheance-nationalite-section-prefill-rules';

describe('DecheanceNationalitePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        decheanceMotif: 'TERRORISME' as const,
        decheanceBinational: true,
        decheanceMesurePrononcee: true,
        decheanceDateDecret: '2024-01-15',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns motif when in whitelist, rejects others', () => {
    expect(Rules.computeMotif({ aiData: { decheanceMotif: 'TERRORISME' } })).toBe('TERRORISME');
    expect(Rules.computeMotif({ aiData: { decheanceMotif: 'FRAUDE_ACQUISITION' } })).toBe('FRAUDE_ACQUISITION');
    expect(Rules.computeMotif({
      aiData: { decheanceMotif: 'ESPIONNAGE' as unknown as 'AUTRE' },
    })).toBeNull();
  });

  it('returns binational when boolean, rejects non-boolean', () => {
    expect(Rules.computeBinational({ aiData: { decheanceBinational: true } })).toBe(true);
    expect(Rules.computeBinational({ aiData: { decheanceBinational: false } })).toBe(false);
    expect(Rules.computeBinational({
      aiData: { decheanceBinational: 'true' as unknown as boolean },
    })).toBeNull();
  });

  it('returns mesurePrononcee when boolean, rejects non-boolean', () => {
    expect(Rules.computeMesurePrononcee({ aiData: { decheanceMesurePrononcee: true } })).toBe(true);
    expect(Rules.computeMesurePrononcee({ aiData: { decheanceMesurePrononcee: false } })).toBe(false);
    expect(Rules.computeMesurePrononcee({
      aiData: { decheanceMesurePrononcee: 1 as unknown as boolean },
    })).toBeNull();
  });

  it('returns dateDecret when ISO yyyy-MM-dd, rejects malformed', () => {
    expect(Rules.computeDateDecret({ aiData: { decheanceDateDecret: '2024-01-15' } })).toBe('2024-01-15');
    expect(Rules.computeDateDecret({ aiData: { decheanceDateDecret: '15/01/2024' } })).toBeNull();
    expect(Rules.computeDateDecret({ aiData: { decheanceDateDecret: null } })).toBeNull();
  });

  it('returns 1 when only motif is present (partiel)', () => {
    expect(Rules.computePrefillCount({ aiData: { decheanceMotif: 'TERRORISME' } })).toBe(1);
  });

  it('returns 4 when all 4 prefill fields are present (complet)', () => {
    const input = {
      aiData: {
        decheanceMotif: 'TERRORISME' as const,
        decheanceBinational: true,
        decheanceMesurePrononcee: true,
        decheanceDateDecret: '2024-01-15',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});
