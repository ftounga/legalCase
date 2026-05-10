/** F-236 SF-236-02 — Tests `AcceptationRenonciationPrefillRules`. */
import {
  AcceptationRenonciationPrefillRules,
  computePrefillCount,
} from './acceptation-renonciation-section-prefill-rules';

describe('AcceptationRenonciationPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/6', () => {
    expect(
      computePrefillCount({
        aiData: { actifBrutSuccessionEurDetecte: 0, dettesIncertainesDetected: false },
      } as any),
    ).toBe(2);
  });

  it('rejette qualité inconnue', () => {
    expect(
      computePrefillCount({ aiData: { qualiteHeritierDetectee: 'TIERS' } } as any),
    ).toBe(0);
  });

  it('cas N — 6/6', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateOuvertureSuccessionDetectee: '2024-05-15',
          actifBrutSuccessionEurDetecte: 100000,
          passifSuccessionEurDetecte: 20000,
          qualiteHeritierDetectee: 'PREMIER_RANG',
          actesEquivalentAcceptationDejaPosesDetected: true,
          dettesIncertainesDetected: true,
        },
      } as any),
    ).toBe(6);
  });

  it('surface', () => {
    expect(AcceptationRenonciationPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
