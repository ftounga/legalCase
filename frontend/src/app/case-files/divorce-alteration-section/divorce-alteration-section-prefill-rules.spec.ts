/**
 * F-236 SF-236-02 / SF-246-27 — Tests `DivorceAlterationPrefillRules`.
 */
import {
  DivorceAlterationPrefillRules,
  computePrefillCount,
  computeDateAssignation,
} from './divorce-alteration-section-prefill-rules';

describe('DivorceAlterationPrefillRules', () => {
  it('cas 0 — input vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
    expect(computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('cas M — partiels (2/6 champs)', () => {
    expect(
      computePrefillCount({
        aiData: { dateCessationVieCommune: '2024-01-15', dureeMariageAnnees: 12 },
      }),
    ).toBe(2);
  });

  it('cas M — rejette valeurs invalides', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateCessationVieCommune: '',
          dureeMariageAnnees: -5,
          revenusAnnuelsEpoux1Eur: -100,
        },
      }),
    ).toBe(0);
  });

  it('cas N — nominal 5/6 champs (sans dateAssignation)', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateCessationVieCommune: '2024-01-15',
          dureeMariageAnnees: 12,
          revenusAnnuelsEpoux1Eur: 45000,
          revenusAnnuelsEpoux2Eur: 38000,
          patrimoineCommunSignificatif: true,
        },
      }),
    ).toBe(5);
  });

  it('patrimoineCommunSignificatif=false compte aussi (boolean valide)', () => {
    expect(
      computePrefillCount({ aiData: { patrimoineCommunSignificatif: false } }),
    ).toBe(1);
  });

  // SF-246-27 : computeDateAssignation
  it('SF-246-27 : computeDateAssignation retourne la date ISO valide', () => {
    expect(computeDateAssignation({ aiData: { dateAssignationDivorce: '2024-09-30' } })).toBe('2024-09-30');
  });

  it('SF-246-27 : computeDateAssignation rejette date non-ISO', () => {
    expect(computeDateAssignation({ aiData: { dateAssignationDivorce: '30/09/2024' } })).toBeNull();
    expect(computeDateAssignation({ aiData: { dateAssignationDivorce: '2024-09' } })).toBeNull();
    expect(computeDateAssignation({})).toBeNull();
  });

  it('SF-246-27 : cas N nominal 6/6 avec dateAssignation', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateCessationVieCommune: '2024-01-15',
          dureeMariageAnnees: 12,
          revenusAnnuelsEpoux1Eur: 45000,
          revenusAnnuelsEpoux2Eur: 38000,
          patrimoineCommunSignificatif: true,
          dateAssignationDivorce: '2024-09-30',
        },
      }),
    ).toBe(6);
  });

  it('expose la surface', () => {
    expect(DivorceAlterationPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
