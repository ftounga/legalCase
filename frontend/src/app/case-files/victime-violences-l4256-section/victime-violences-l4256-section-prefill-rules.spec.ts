import { VictimeViolencesL4256PrefillRules as Rules } from './victime-violences-l4256-section-prefill-rules';

describe('VictimeViolencesL4256PrefillRules', () => {
  describe('computeDateOrdonnanceProtection', () => {
    it('returns null when no aiData', () => {
      expect(Rules.computeDateOrdonnanceProtection({})).toBeNull();
    });

    it('returns null when aiData has no dateOrdonnanceProtectionJaf', () => {
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateNotificationOqtf: '2026-04-01' },
      })).toBeNull();
    });

    it('returns the date when dateOrdonnanceProtectionJaf is a valid ISO date', () => {
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateOrdonnanceProtectionJaf: '2026-01-15' },
      })).toBe('2026-01-15');
    });

    it('returns null when dateOrdonnanceProtectionJaf is not ISO (ambiguous LLM format)', () => {
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateOrdonnanceProtectionJaf: '15 janvier 2026' },
      })).toBeNull();
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateOrdonnanceProtectionJaf: '15/01/2026' },
      })).toBeNull();
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateOrdonnanceProtectionJaf: '2026-1-5' },
      })).toBeNull();
    });

    it('returns null when dateOrdonnanceProtectionJaf is null or empty', () => {
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateOrdonnanceProtectionJaf: null },
      })).toBeNull();
      expect(Rules.computeDateOrdonnanceProtection({
        aiData: { dateOrdonnanceProtectionJaf: '' },
      })).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('returns 0 when no aiData (empty input)', () => {
      expect(Rules.computePrefillCount({})).toBe(0);
      expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('returns 0 when aiData date is not ISO', () => {
      expect(Rules.computePrefillCount({
        aiData: { dateOrdonnanceProtectionJaf: '15/01/2026' },
      })).toBe(0);
    });

    it('returns 0 when BELGIQUE even with a valid date (mono-pays FR)', () => {
      expect(Rules.computePrefillCount({
        aiData: { dateOrdonnanceProtectionJaf: '2026-01-15' },
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('returns 1 when FRANCE and a valid ISO date is detected', () => {
      expect(Rules.computePrefillCount({
        aiData: { dateOrdonnanceProtectionJaf: '2026-01-15' },
        workspaceCountry: 'FRANCE',
      })).toBe(1);
    });

    it('returns 1 when workspaceCountry is omitted (defaults to FRANCE)', () => {
      expect(Rules.computePrefillCount({
        aiData: { dateOrdonnanceProtectionJaf: '2026-01-15' },
      })).toBe(1);
    });
  });
});
