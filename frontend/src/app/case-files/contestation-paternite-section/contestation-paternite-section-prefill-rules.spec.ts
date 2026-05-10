/** F-236 SF-236-02 — Tests `ContestationPaternitePrefillRules`. */
import {
  ContestationPaternitePrefillRules,
  computePrefillCount,
} from './contestation-paternite-section-prefill-rules';

describe('ContestationPaternitePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 3/7', () => {
    expect(
      computePrefillCount({
        aiData: {
          qualiteAagirContestationDetected: 'MERE',
          dateEtablissementFiliationDetectee: '2020-01-15',
          possessionEtatConforme5AnsDetected: true,
        },
      } as any),
    ).toBe(3);
  });

  it('cas N — 7/7', () => {
    expect(
      computePrefillCount({
        aiData: {
          qualiteAagirContestationDetected: 'MERE',
          dateEtablissementFiliationDetectee: '2020-01-15',
          dateConnaissanceVeriteDetectee: '2024-03-01',
          dateMajoriteEnfantDetectee: '2038-05-10',
          possessionEtatConforme5AnsDetected: false,
          expertiseAdnDemandeeDetected: true,
          motifsSerieuxDetected: true,
        },
      } as any),
    ).toBe(7);
  });

  it('surface', () => {
    expect(ContestationPaternitePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
