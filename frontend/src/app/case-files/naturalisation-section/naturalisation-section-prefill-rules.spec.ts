import { NaturalisationPrefillRules as Rules } from './naturalisation-section-prefill-rules';

describe('NaturalisationPrefillRules', () => {
  it('returns 0 quand aucune donnée IA (no-op runtime)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 même avec aiData rempli (crochet futur — pas de mapping aujourd\'hui)', () => {
    expect(Rules.computePrefillCount({
      aiData: { dureeResidenceAnnees: 10, ageDemandeurAnnees: 35 },
    })).toBe(0);
  });

  it('returns 0 quel que soit workspaceCountry', () => {
    expect(Rules.computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
    expect(Rules.computePrefillCount({ workspaceCountry: 'BELGIQUE' })).toBe(0);
  });
});
