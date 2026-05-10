/**
 * F-236 SF-236-02 — Tests `MesuresProvisoiresPrefillRules`.
 */
import {
  MesuresProvisoiresPrefillRules,
  computePrefillCount,
  computeRevenusDemandeur,
} from './mesures-provisoires-section-prefill-rules';

describe('MesuresProvisoiresPrefillRules', () => {
  it('cas 0 — vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — date seule + invalide rejetée', () => {
    expect(
      computePrefillCount({ aiData: { dateAudienceAOMP: '2026-05-10' } }),
    ).toBe(1);
    expect(
      computePrefillCount({ aiData: { dateAudienceAOMP: '10/05/2026' } }),
    ).toBe(0);
  });

  it('cas M — revenus mensuel direct priorité sur annuel', () => {
    expect(
      computeRevenusDemandeur({
        aiData: { revenusEpouxDemandeurEur: 3500, revenusAnnuelsEpoux1Eur: 12000 },
      }),
    ).toBe(3500);
  });

  it('cas M — revenus annuel /12 si mensuel absent', () => {
    expect(
      computeRevenusDemandeur({ aiData: { revenusAnnuelsEpoux1Eur: 48000 } }),
    ).toBe(4000);
  });

  it('cas N — 5/5 champs', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateAudienceAOMP: '2026-05-10',
          revenusEpouxDemandeurEur: 3500,
          revenusEpouxDefendeurEur: 2800,
          violencesAlleguees: true,
          patrimoineCommunSignificatif: true,
        },
      }),
    ).toBe(5);
  });

  it('surface', () => {
    expect(MesuresProvisoiresPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
