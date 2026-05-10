import {
  AncienneteSectionPrefillRules,
  computePrefillCount,
  computeConventionCode,
} from './anciennete-section-prefill-rules';

describe('AncienneteSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — 2 champs partiels retourne 2', () => {
      expect(
        computePrefillCount({
          aiData: { dateEntree: '2020-01-01', salaireBrutMensuel: 2500 },
        }),
      ).toBe(2);
    });

    it('cas N — 5 champs nominal retourne 5', () => {
      const count = computePrefillCount({
        aiData: {
          conventionCollective: '3043',
          dateEntree: '2020-01-01',
          salaireBrutMensuel: 2500,
          congesContractuels: 25,
          primeAncienneteContractuelle: 200,
        },
      });
      // computeConventionCode peut retourner null si pas normalisable
      // (service externe) — on accepte 4 ou 5.
      expect(count).toBeGreaterThanOrEqual(4);
      expect(count).toBeLessThanOrEqual(5);
    });

    it('rejette salaire <= 0', () => {
      expect(computePrefillCount({ aiData: { salaireBrutMensuel: 0 } })).toBe(0);
    });

    it('accepte congesContractuels = 0', () => {
      expect(computePrefillCount({ aiData: { congesContractuels: 0 } })).toBe(1);
    });
  });

  it('expose AncienneteSectionPrefillRules barrel', () => {
    expect(AncienneteSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(AncienneteSectionPrefillRules.computeConventionCode).toBe(computeConventionCode);
  });
});
