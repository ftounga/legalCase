import { RttMonetisationPrefillRules } from './rtt-monetisation-section-prefill-rules';

describe('RttMonetisationPrefillRules', () => {
  it('computeNombreJoursRttRenonces : entier > 0 (FR) sinon null', () => {
    expect(RttMonetisationPrefillRules.computeNombreJoursRttRenonces({
      aiData: { nombre_jours_rtt_renonces: 5 } as any, workspaceCountry: 'FRANCE',
    })).toBe(5);
    expect(RttMonetisationPrefillRules.computeNombreJoursRttRenonces({
      aiData: { nombre_jours_rtt_renonces: 0 } as any, workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(RttMonetisationPrefillRules.computeNombreJoursRttRenonces({
      aiData: { nombre_jours_rtt_renonces: 5 } as any, workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computeSalaireJournalierBrut : nombre > 0 (FR) sinon null', () => {
    expect(RttMonetisationPrefillRules.computeSalaireJournalierBrut({
      aiData: { salaire_journalier_brut: 180.5 } as any, workspaceCountry: 'FRANCE',
    })).toBe(180.5);
    expect(RttMonetisationPrefillRules.computeSalaireJournalierBrut({
      aiData: { salaire_journalier_brut: -1 } as any, workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(RttMonetisationPrefillRules.computeSalaireJournalierBrut({})).toBeNull();
  });

  it('computePrefillCount : 0 / partiel / nominal', () => {
    expect(RttMonetisationPrefillRules.computePrefillCount({})).toBe(0);
    expect(RttMonetisationPrefillRules.computePrefillCount({
      aiData: { nombre_jours_rtt_renonces: 5 } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(RttMonetisationPrefillRules.computePrefillCount({
      aiData: { nombre_jours_rtt_renonces: 5, salaire_journalier_brut: 200 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('computePrefillCount = 0 hors FRANCE même avec données', () => {
    expect(RttMonetisationPrefillRules.computePrefillCount({
      aiData: { nombre_jours_rtt_renonces: 5, salaire_journalier_brut: 200 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
