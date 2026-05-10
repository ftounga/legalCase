/** F-236 SF-236-02 — Tests `RecherchePaternitePrefillRules`. */
import {
  RecherchePaternitePrefillRules,
  computePrefillCount,
} from './recherche-paternite-section-prefill-rules';

describe('RecherchePaternitePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/6', () => {
    expect(
      computePrefillCount({
        aiData: {
          qualiteDuDemandeurRechercheDetected: 'MERE',
          dateNaissanceEnfantRechercheDetectee: '2010-08-15',
        },
      } as any),
    ).toBe(2);
  });

  it('cas N — 6/6', () => {
    expect(
      computePrefillCount({
        aiData: {
          qualiteDuDemandeurRechercheDetected: 'MERE',
          dateNaissanceEnfantRechercheDetectee: '2010-08-15',
          presomptionPossessionEtatRechercheDetected: true,
          expertiseAdnDemandeeRechercheDetected: true,
          pereDesigneRefuseADNDetected: true,
          motifsSerieuxRechercheDetected: true,
        },
      } as any),
    ).toBe(6);
  });

  it('surface', () => {
    expect(RecherchePaternitePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
