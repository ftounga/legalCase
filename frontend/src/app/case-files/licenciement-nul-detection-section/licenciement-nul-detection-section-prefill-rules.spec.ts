import {
  LicenciementNulDetectionSectionPrefillRules,
  computePrefillCount,
  computeDateNotification,
  computeProtectionFlag,
} from './licenciement-nul-detection-section-prefill-rules';

describe('LicenciementNulDetectionSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas M — 2 champs partiels', () => {
      expect(
        computePrefillCount({
          aiData: { salaireBrutMensuel: 2500, dateLicenciement: '2024-05-01' },
        }),
      ).toBe(2);
    });

    it('cas N — 3 champs nominal', () => {
      expect(
        computePrefillCount({
          aiData: {
            salaireBrutMensuel: 2500,
            dateLicenciement: '2024-05-01',
            motifNullitePressenti: 'DISCRIMINATION',
          },
        }),
      ).toBe(3);
    });

    it('rejette date hors format ISO', () => {
      expect(computeDateNotification({ aiData: { dateLicenciement: '01/05/2024' } })).toBeNull();
    });

    it('rejette motif non mappable', () => {
      expect(
        computeProtectionFlag({ aiData: { motifNullitePressenti: 'INCONNU_XYZ' } }),
      ).toBeNull();
    });
  });

  it('expose LicenciementNulDetectionSectionPrefillRules barrel', () => {
    expect(LicenciementNulDetectionSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
