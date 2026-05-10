import { MineursImmigrationPrefillRules as Rules } from './mineurs-immigration-section-prefill-rules';

describe('MineursImmigrationPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNaissance: '2010-01-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 1 for each single field present', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNaissance: '2010-01-01' } })).toBe(1);
    expect(Rules.computePrefillCount({ aiData: { dateEntreeFrance: '2020-09-01' } })).toBe(1);
    expect(Rules.computePrefillCount({ aiData: { nationalite: 'MAR' } })).toBe(1);
  });

  it('returns N=3 when all three fields are set', () => {
    const input = {
      aiData: {
        dateNaissance: '2010-01-01',
        dateEntreeFrance: '2020-09-01',
        nationalite: 'MAR',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
