import {
  RuptureConvSectionPrefillRules,
  computePrefillCount,
} from './rupture-conv-section-prefill-rules';

describe('RuptureConvSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: { detections: {} } })).toBe(0);
    });

    it('cas M — 2 codes OUI/NON, INCONNU ignoré, code hors RC_CODES ignoré', () => {
      expect(
        computePrefillCount({
          aiData: {
            detections: {
              RC_CONSENTEMENT: { reponse: 'OUI' },
              RC_HOMOLOGATION: { reponse: 'NON' },
              RC_DELAI_RETRACTATION: { reponse: 'INCONNU' },
              FOO: { reponse: 'OUI' }, // hors RC_CODES
            },
          },
        }),
      ).toBe(2);
    });

    it('cas N — 6 codes RC_CODES OUI', () => {
      const detections: Record<string, { reponse: 'OUI' | 'NON' | 'INCONNU' }> = {};
      for (const code of RuptureConvSectionPrefillRules.RC_CODES) {
        detections[code] = { reponse: 'OUI' };
      }
      expect(computePrefillCount({ aiData: { detections } })).toBe(6);
    });
  });

  it('expose RuptureConvSectionPrefillRules barrel', () => {
    expect(RuptureConvSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
