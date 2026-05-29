import { RenouvellementDelaiPrefillRules as Rules } from './renouvellement-delai-section-prefill-rules';

describe('RenouvellementDelaiPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateExpirationTitre: '2026-09-15', typeTitreSejour: 'Carte pluriannuelle' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateExpirationTitre(input)).toBeNull();
    expect(Rules.computeTypeTitre(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only dateExpirationTitre is a valid ISO date', () => {
    const input = { aiData: { dateExpirationTitre: '2026-09-15' } };
    expect(Rules.computeDateExpirationTitre(input)).toBe('2026-09-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 when dateExpirationTitre + typeTitreSejour present', () => {
    const input = {
      aiData: { dateExpirationTitre: '2026-09-15', typeTitreSejour: 'Carte de séjour pluriannuelle' },
    };
    expect(Rules.computeDateExpirationTitre(input)).toBe('2026-09-15');
    expect(Rules.computeTypeTitre(input)).toBe('Carte de séjour pluriannuelle');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('trims surrounding whitespace on date and type', () => {
    expect(Rules.computeDateExpirationTitre({ aiData: { dateExpirationTitre: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
    expect(Rules.computeTypeTitre({ aiData: { typeTitreSejour: '  Étudiant  ' } }))
      .toBe('Étudiant');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateExpirationTitre({ aiData: { dateExpirationTitre: '15/09/2026' } })).toBeNull();
    expect(Rules.computeDateExpirationTitre({ aiData: { dateExpirationTitre: '2026-9-5' } })).toBeNull();
    expect(Rules.computeDateExpirationTitre({
      aiData: { dateExpirationTitre: 20260915 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateExpirationTitre({ aiData: { dateExpirationTitre: null } })).toBeNull();
  });

  it('rejects empty / non-string typeTitreSejour', () => {
    expect(Rules.computeTypeTitre({ aiData: { typeTitreSejour: '' } })).toBeNull();
    expect(Rules.computeTypeTitre({ aiData: { typeTitreSejour: '   ' } })).toBeNull();
    expect(Rules.computeTypeTitre({ aiData: { typeTitreSejour: 42 as unknown as string } })).toBeNull();
    expect(Rules.computeTypeTitre({ aiData: { typeTitreSejour: null } })).toBeNull();
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { dateExpirationTitre: '2026-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
