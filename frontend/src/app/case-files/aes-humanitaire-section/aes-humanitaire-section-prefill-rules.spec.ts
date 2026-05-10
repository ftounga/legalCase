import { AesHumanitairePrefillRules as Rules } from './aes-humanitaire-section-prefill-rules';

describe('AesHumanitairePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateDepotProcedure: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 when dépôt anterieur à entrée France', () => {
    const input = {
      aiData: { dateEntreeFrance: '2024-01-01', dateDepotProcedure: '2023-01-01' },
    };
    // dépôt avant entrée → seul entrée compte
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('returns 1 when only dateDepotProcedure (sans entrée)', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('returns N=2 when both fields cohérents', () => {
    const input = {
      aiData: { dateEntreeFrance: '2020-01-01', dateDepotProcedure: '2026-04-01' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
