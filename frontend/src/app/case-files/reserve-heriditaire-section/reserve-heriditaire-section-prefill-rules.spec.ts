/** F-236 SF-236-02 — Tests `ReserveHeriditairePrefillRules`. */
import {
  ReserveHeriditairePrefillRules,
  computePrefillCount,
  computeNombreEnfants,
} from './reserve-heriditaire-section-prefill-rules';

describe('ReserveHeriditairePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('fallback nbDescendants si nombreEnfantsSuccession absent', () => {
    expect(computeNombreEnfants({ aiData: { nbDescendantsDetecte: 3 } } as any)).toBe(3);
  });

  it('cas M — 2/6 (nb + conjoint)', () => {
    expect(
      computePrefillCount({
        aiData: { nbDescendantsDetecte: 2, conjointSurvivantDetected: true },
      } as any),
    ).toBe(2);
  });

  it('rejette montantSuccession 0', () => {
    expect(
      computePrefillCount({ aiData: { montantSuccessionEurDetecte: 0 } } as any),
    ).toBe(0);
  });

  it('cas N — 6/6', () => {
    expect(
      computePrefillCount({
        aiData: {
          nombreEnfantsSuccessionDetecte: 2,
          conjointSurvivantDetected: false,
          montantSuccessionEurDetecte: 500000,
          montantLibsTotalEurDetecte: 100000,
          dateOuvertureSuccessionDetectee: '2025-01-15',
          qualiteDuDemandeurReserveDetecte: 'HERITIER_RESERVATAIRE',
        },
      } as any),
    ).toBe(6);
  });

  it('surface', () => {
    expect(ReserveHeriditairePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
