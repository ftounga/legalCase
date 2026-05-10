/** F-236 SF-236-02 — Tests `ReconnaissancePaternellePrefillRules`. */
import {
  ReconnaissancePaternellePrefillRules,
  computePrefillCount,
} from './reconnaissance-paternelle-section-prefill-rules';

describe('ReconnaissancePaternellePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/5', () => {
    expect(
      computePrefillCount({
        aiData: { consentementLibreDuPereDetected: true, paterniteVraisemblableDetected: false },
      } as any),
    ).toBe(2);
  });

  it('cas N — 5/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          consentementLibreDuPereDetected: true,
          paterniteVraisemblableDetected: true,
          enfantNonReconnuParAutrePereDetected: true,
          procedureRespecteeReconnaissanceDetected: true,
          dateNaissanceEnfantDetectee: '2020-05-10',
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(ReconnaissancePaternellePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
