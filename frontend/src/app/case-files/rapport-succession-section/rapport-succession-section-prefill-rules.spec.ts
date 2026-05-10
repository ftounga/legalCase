/** F-236 SF-236-02 — Tests `RapportSuccessionPrefillRules`. */
import {
  RapportSuccessionPrefillRules,
  computePrefillCount,
} from './rapport-succession-section-prefill-rules';

describe('RapportSuccessionPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/6', () => {
    expect(
      computePrefillCount({
        aiData: {
          qualiteHeritierRapportDetectee: 'DESCENDANT',
          dateDonationDetectee: '2020-05-01',
        },
      } as any),
    ).toBe(2);
  });

  it('rejette qualité inconnue', () => {
    expect(
      computePrefillCount({
        aiData: { qualiteHeritierRapportDetectee: 'TIERS' },
      } as any),
    ).toBe(0);
  });

  it('cas N — 6/6', () => {
    expect(
      computePrefillCount({
        aiData: {
          qualiteHeritierRapportDetectee: 'DESCENDANT',
          montantDonationsRecuesEurDetecte: 50000,
          valeurDonationAuJourPartageEurDetectee: 100000,
          dateDonationDetectee: '2020-05-01',
          donationDispenseDeRapportDetected: true,
          naturePresumeeNonRapportableDetected: false,
        },
      } as any),
    ).toBe(6);
  });

  it('surface', () => {
    expect(RapportSuccessionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
