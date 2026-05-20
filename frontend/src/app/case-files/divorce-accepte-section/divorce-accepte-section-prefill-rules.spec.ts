/**
 * F-236 SF-236-02 / SF-246-27 — Tests `DivorceAcceptePrefillRules`.
 */
import {
  DivorceAcceptePrefillRules,
  computePrefillCount,
  computeDateAssignation,
} from './divorce-accepte-section-prefill-rules';

describe('DivorceAcceptePrefillRules', () => {
  it('cas 0 — retourne 0 quand vide', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
    expect(computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('cas M — partiels (2/6)', () => {
    expect(
      computePrefillCount({
        aiData: { dureeMariageAnnees: 15, revenusAnnuelsEpoux1Eur: 45000 },
      }),
    ).toBe(2);
  });

  it('cas M — rejette dureeMariage non-entière', () => {
    expect(computePrefillCount({ aiData: { dureeMariageAnnees: 12.5 } })).toBe(0);
  });

  it('cas M — rejette date non-ISO (dateAcceptationPV)', () => {
    expect(computePrefillCount({ aiData: { dateAcceptationPV: '15/03/2026' } })).toBe(0);
  });

  it('cas N — nominal 5/6 (sans dateAssignation)', () => {
    expect(
      computePrefillCount({
        aiData: {
          dureeMariageAnnees: 12,
          revenusAnnuelsEpoux1Eur: 45000,
          revenusAnnuelsEpoux2Eur: 38000,
          patrimoineCommun: true,
          dateAcceptationPV: '2026-03-15',
        },
      }),
    ).toBe(5);
  });

  it('patrimoineCommun=false compte (boolean valide)', () => {
    expect(computePrefillCount({ aiData: { patrimoineCommun: false } })).toBe(1);
  });

  // SF-246-27 : computeDateAssignation
  it('SF-246-27 : computeDateAssignation retourne la date ISO valide', () => {
    expect(computeDateAssignation({ aiData: { dateAssignationDivorce: '2024-11-04' } })).toBe('2024-11-04');
  });

  it('SF-246-27 : computeDateAssignation rejette date non-ISO', () => {
    expect(computeDateAssignation({ aiData: { dateAssignationDivorce: '04/11/2024' } })).toBeNull();
    expect(computeDateAssignation({ aiData: { dateAssignationDivorce: '' } })).toBeNull();
    expect(computeDateAssignation({})).toBeNull();
  });

  it('SF-246-27 : cas N nominal 6/6 avec dateAssignation', () => {
    expect(
      computePrefillCount({
        aiData: {
          dureeMariageAnnees: 12,
          revenusAnnuelsEpoux1Eur: 45000,
          revenusAnnuelsEpoux2Eur: 38000,
          patrimoineCommun: true,
          dateAcceptationPV: '2026-03-15',
          dateAssignationDivorce: '2024-11-04',
        },
      }),
    ).toBe(6);
  });

  it('expose surface', () => {
    expect(DivorceAcceptePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
