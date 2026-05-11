import { VictimeViolencesL4256PrefillRules as Rules } from './victime-violences-l4256-section-prefill-rules';

describe('VictimeViolencesL4256PrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when BELGIQUE (mono-pays FR)', () => {
    expect(Rules.computePrefillCount({
      aiData: {},
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 today (no IA signal for ordonnance JAF in ImmigrationExtractedData V1)', () => {
    expect(Rules.computeDateOrdonnanceProtection({})).toBeNull();
    expect(Rules.computeDateOrdonnanceProtection({ aiData: { dateNotificationOqtf: '2026-04-01' } })).toBeNull();
  });
});
