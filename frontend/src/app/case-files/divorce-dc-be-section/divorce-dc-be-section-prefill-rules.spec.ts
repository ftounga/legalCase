import { DivorceDcBeSectionPrefillRules, computeDateAudienceHomologation } from './divorce-dc-be-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/**
 * F-243 / SF-246-27 — Tests du helper partagé `DivorceDcBeSectionPrefillRules`.
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

  // SF-246-27 : computeDateAudienceHomologation (BELGIQUE UNIQUEMENT)
  describe('SF-246-27 : computeDateAudienceHomologation', () => {
    it('retourne la date ISO valide', () => {
      const aiData: FamilleExtractedData = { dateAudienceHomologationDcBe: '2025-02-18' };
      expect(computeDateAudienceHomologation({ aiData })).toBe('2025-02-18');
    });

    it('retourne null si absente', () => {
      expect(computeDateAudienceHomologation({})).toBeNull();
      expect(computeDateAudienceHomologation({ aiData: {} })).toBeNull();
    });

    it('retourne null si format non ISO', () => {
      const aiData: FamilleExtractedData = { dateAudienceHomologationDcBe: '18/02/2025' };
      expect(computeDateAudienceHomologation({ aiData })).toBeNull();
    });

    it('retourne null si chaîne vide', () => {
      const aiData: FamilleExtractedData = { dateAudienceHomologationDcBe: '' };
      expect(computeDateAudienceHomologation({ aiData })).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('renvoie 0 sans aiData', () => {
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({})).toBe(0);
    });

    it('renvoie 0 si les deux dates sont absentes', () => {
      const aiData: FamilleExtractedData = {};
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(0);
    });

    it('renvoie 1 si dateAcceptationPV ISO valide uniquement', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '2025-12-12' };
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(1);
    });

    it('renvoie 1 si dateAudienceHomologationDcBe ISO valide uniquement', () => {
      const aiData: FamilleExtractedData = { dateAudienceHomologationDcBe: '2025-02-18' };
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(1);
    });

    it('SF-246-27 : renvoie 2 si les deux dates ISO valides', () => {
      const aiData: FamilleExtractedData = {
        dateAcceptationPV: '2025-12-12',
        dateAudienceHomologationDcBe: '2025-02-18',
      };
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(2);
    });

    it('renvoie 0 si dateAcceptationPV format invalide', () => {
      const aiData: FamilleExtractedData = { dateAcceptationPV: '2025' };
      expect(DivorceDcBeSectionPrefillRules.computePrefillCount({ aiData })).toBe(0);
    });
  });
});
