/**
 * F-236 SF-236-02 — Tests `DivorceChecklistPrefillRules`.
 */
import {
  DivorceChecklistPrefillRules,
  computeDateAcceptationPV,
  computePrefillCount,
  isIsoDate,
  SIGNATURE_STEP_CODES,
} from './divorce-checklist-section-prefill-rules';

describe('DivorceChecklistPrefillRules', () => {
  describe('cas 0', () => {
    it('retourne 0 quand input vide', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('retourne 0 quand aiData absent', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('retourne 0 quand dateAcceptationPV absent', () => {
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });
  });

  describe('cas M — partiels (date invalide)', () => {
    it('retourne 0 pour date non-ISO', () => {
      expect(computePrefillCount({ aiData: { dateAcceptationPV: '15/03/2026' } })).toBe(0);
    });

    it('retourne 0 pour type non-string', () => {
      expect(
        computePrefillCount({ aiData: { dateAcceptationPV: 123 as unknown as string } }),
      ).toBe(0);
    });

    it('retourne 0 pour chaîne vide', () => {
      expect(computePrefillCount({ aiData: { dateAcceptationPV: '' } })).toBe(0);
    });
  });

  describe('cas N — nominal', () => {
    it('retourne 1 pour date ISO YYYY-MM-DD', () => {
      expect(computePrefillCount({ aiData: { dateAcceptationPV: '2026-03-15' } })).toBe(1);
    });

    it('computeDateAcceptationPV renvoie la date validée', () => {
      expect(
        computeDateAcceptationPV({ aiData: { dateAcceptationPV: '2025-12-31' } }),
      ).toBe('2025-12-31');
    });
  });

  describe('isIsoDate', () => {
    it('valide YYYY-MM-DD', () => {
      expect(isIsoDate('2026-05-10')).toBe(true);
    });

    it('rejette format français', () => {
      expect(isIsoDate('10/05/2026')).toBe(false);
    });
  });

  describe('surface', () => {
    it('expose SIGNATURE_STEP_CODES', () => {
      expect(SIGNATURE_STEP_CODES).toContain('FR_SIGNATURE_CONVENTION');
      expect(SIGNATURE_STEP_CODES).toContain('BE_REDACTION_CONVENTION');
      expect(DivorceChecklistPrefillRules.SIGNATURE_STEP_CODES).toBe(SIGNATURE_STEP_CODES);
    });
  });
});
