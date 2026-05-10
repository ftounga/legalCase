import { AesEtudiantPrefillRules as Rules } from './aes-etudiant-section-prefill-rules';

describe('AesEtudiantPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateDepotProcedure: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for future date or malformed', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2099-12-31' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: 'bad' } })).toBe(0);
  });

  it('returns 1 when only dateDepotProcedure is set', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-01-15' } })).toBe(1);
  });

  it('returns 1 when only dateEntreeFrance is set', () => {
    expect(Rules.computePrefillCount({ aiData: { dateEntreeFrance: '2022-01-01' } })).toBe(1);
  });

  it('returns N=2 when both fields are set', () => {
    const input = {
      aiData: { dateEntreeFrance: '2022-09-01', dateDepotProcedure: '2026-01-15' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
