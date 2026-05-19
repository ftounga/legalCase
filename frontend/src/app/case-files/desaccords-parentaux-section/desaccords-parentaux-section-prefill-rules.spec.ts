/** F-236 SF-236-02 — Tests `DesaccordsParentauxPrefillRules`. */
import {
  DesaccordsParentauxPrefillRules,
  computePrefillCount,
} from './desaccords-parentaux-section-prefill-rules';

describe('DesaccordsParentauxPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/5', () => {
    expect(
      computePrefillCount({
        aiData: { domaineDesaccordDetecte: 'SCOLARITE', urgenceDetectee: true },
      } as any),
    ).toBe(2);
  });

  it('filtre tentatives invalides', () => {
    expect(
      DesaccordsParentauxPrefillRules.computeTentatives({
        aiData: { tentativesMediationDetectees: ['MEDIATION_FAMILIALE', 'INVALIDE', 'aucune'] },
      } as any),
    ).toEqual(['MEDIATION_FAMILIALE', 'AUCUNE']);
  });

  it('cas N — 5/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          domaineDesaccordDetecte: 'SCOLARITE',
          intensiteDesaccordDetecte: 'MAJEUR',
          tentativesMediationDetectees: ['MEDIATION_FAMILIALE'],
          agesEnfantsDetectes: [10],
          urgenceDetectee: true,
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(DesaccordsParentauxPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
