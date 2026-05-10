import {
  LicenciementSectionPrefillRules,
  computePrefillCount,
  computePrefilledCodes,
} from './licenciement-section-prefill-rules';

describe('LicenciementSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
      expect(computePrefillCount({ aiData: { detections: null } })).toBe(0);
      expect(computePrefillCount({ aiData: { detections: {} } })).toBe(0);
    });

    it('cas M — détections partielles avec INCONNU ignorés', () => {
      expect(
        computePrefillCount({
          aiData: {
            detections: {
              CRIT1: { reponse: 'OUI' },
              CRIT2: { reponse: 'INCONNU' },
              CRIT3: { reponse: 'NON' },
            },
          },
        }),
      ).toBe(2);
    });

    it('cas N — toutes détections OUI/NON', () => {
      expect(
        computePrefillCount({
          aiData: {
            detections: {
              A: { reponse: 'OUI' },
              B: { reponse: 'NON' },
              C: { reponse: 'OUI' },
            },
          },
        }),
      ).toBe(3);
    });
  });

  it('computePrefilledCodes retourne les codes attendus', () => {
    expect(
      computePrefilledCodes({
        aiData: { detections: { A: { reponse: 'OUI' }, B: { reponse: 'INCONNU' } } },
      }),
    ).toEqual(['A']);
  });

  it('expose LicenciementSectionPrefillRules barrel', () => {
    expect(LicenciementSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
