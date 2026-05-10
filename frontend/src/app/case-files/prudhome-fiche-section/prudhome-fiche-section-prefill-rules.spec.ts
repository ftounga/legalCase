import {
  PrudhomeFicheSectionPrefillRules,
  computePrefillCount,
} from './prudhome-fiche-section-prefill-rules';

describe('PrudhomeFicheSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — poste vide retourne 0', () => {
      expect(computePrefillCount({ aiData: { poste: null } })).toBe(0);
      expect(computePrefillCount({ aiData: { poste: '' } })).toBe(0);
    });

    it('cas N — poste renseigné retourne 1', () => {
      expect(computePrefillCount({ aiData: { poste: 'Développeur' } })).toBe(1);
    });
  });

  it('expose PrudhomeFicheSectionPrefillRules barrel', () => {
    expect(PrudhomeFicheSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
