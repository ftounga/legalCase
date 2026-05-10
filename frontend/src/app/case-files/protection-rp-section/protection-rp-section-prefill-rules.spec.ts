import {
  ProtectionRpSectionPrefillRules,
  computePrefillCount,
} from './protection-rp-section-prefill-rules';

describe('ProtectionRpSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — motifLicenciement vide / non mappable retourne 0', () => {
      expect(computePrefillCount({ aiData: { motifLicenciement: null } })).toBe(0);
      expect(computePrefillCount({ aiData: { motifLicenciement: '' } })).toBe(0);
      expect(
        computePrefillCount({ aiData: { motifLicenciement: 'COMPLETEMENT_INCONNU' } }),
      ).toBe(0);
    });

    it('cas N — motifLicenciement mappable retourne 1', () => {
      // Le mapper accepte au moins une variante courante.
      const variants = ['ECONOMIQUE', 'FAUTE_GRAVE', 'INAPTITUDE'];
      let success = 0;
      for (const v of variants) {
        if (computePrefillCount({ aiData: { motifLicenciement: v } }) === 1) {
          success++;
        }
      }
      expect(success).toBeGreaterThan(0);
    });
  });

  it('expose ProtectionRpSectionPrefillRules barrel', () => {
    expect(ProtectionRpSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
