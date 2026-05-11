import { DivorceDcBeSectionPrefillRules } from './divorce-dc-be-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/**
 * F-243 — Tests du helper partagé `DivorceDcBeSectionPrefillRules`.
 * Garantit la parité runtime/static (`prefill-count-integrity.spec.ts`).
 */
describe('DivorceDcBeSectionPrefillRules', () => {

  describe('computeDateSignatureConvention', () => {
    it('renvoie la date quand aiData.dateAcceptationPV est ISO YYYY-MM-DD valide', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '2025-12-12' };
      expect(DivorceDcBeSectionPrefillRules.computeDateSignatureConvention({ aiData })).toBe('2025-12-12');
    });

    it('renvoie null si aiData absent', () => {
      expect(DivorceDcBeSectionPrefillRules.computeDateSignatureConvention({})).toBeNull();
    });

    it('renvoie null si aiData.dateAcceptationPV absent', () => {
      const aiData: FamilleExtractedData = {};
      expect(DivorceDcBeSectionPrefillRules.computeDateSignatureConvention({ aiData })).toBeNull();
    });

    it('renvoie null si aiData.dateAcceptationPV format non ISO', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '12/12/2025' };
      expect(DivorceDcBeSectionPrefillRules.computeDateSignatureConvention({ aiData })).toBeNull();
    });

    it('renvoie null si aiData.dateAcceptationPV chaîne vide', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '' };
      expect(DivorceDcBeSectionPrefillRules.computeDateSignatureConvention({ aiData })).toBeNull();
    });

    it('renvoie null si aiData.dateAcceptationPV n est pas une string', () => {
      const aiData = { dateAcceptationPV: 20251212 } as unknown as FamilleExtractedData;
      expect(DivorceDcBeSectionPrefillRules.computeDateSignatureConvention({ aiData })).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('renvoie 0 sans aiData', () => {
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({})).toBe(0);
    });

    it('renvoie 0 si dateAcceptationPV absent', () => {
      const aiData: FamilleExtractedData = {};
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(0);
    });

    it('renvoie 1 si dateAcceptationPV ISO valide', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '2025-12-12' };
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(1);
    });

    it('renvoie 0 si dateAcceptationPV format invalide', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '2025' };
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(0);
    });
  });
});
