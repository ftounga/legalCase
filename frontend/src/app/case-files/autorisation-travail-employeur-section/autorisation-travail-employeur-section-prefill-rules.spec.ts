import { AutorisationTravailEmployeurPrefillRules as Rules } from './autorisation-travail-employeur-section-prefill-rules';

describe('AutorisationTravailEmployeurPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { nationalite: 'Algérienne' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeNationaliteCandidat(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when nationalite present (FRANCE)', () => {
    const input = { aiData: { nationalite: 'Algérienne' } };
    expect(Rules.computeNationaliteCandidat(input)).toBe('Algérienne');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the nationalite', () => {
    expect(Rules.computeNationaliteCandidat({ aiData: { nationalite: '  Marocaine  ' } }))
      .toBe('Marocaine');
    expect(Rules.computePrefillCount({ aiData: { nationalite: '  Marocaine  ' } })).toBe(1);
  });

  it('rejects empty / whitespace-only / non-string nationalite', () => {
    expect(Rules.computeNationaliteCandidat({ aiData: { nationalite: '' } })).toBeNull();
    expect(Rules.computeNationaliteCandidat({ aiData: { nationalite: '   ' } })).toBeNull();
    expect(Rules.computeNationaliteCandidat({
      aiData: { nationalite: 123 as unknown as string },
    })).toBeNull();
    expect(Rules.computeNationaliteCandidat({ aiData: { nationalite: null } })).toBeNull();
    expect(Rules.computePrefillCount({ aiData: { nationalite: '' } })).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    expect(Rules.computePrefillCount({ aiData: { nationalite: 'Tunisienne' } })).toBe(1);
  });
});
