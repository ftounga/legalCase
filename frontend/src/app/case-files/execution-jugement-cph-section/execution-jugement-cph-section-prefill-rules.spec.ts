import { ExecutionJugementCphPrefillRules as Rules } from './execution-jugement-cph-section-prefill-rules';

describe('ExecutionJugementCphPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { montantCondamnationCph: 12000, situationEmployeurDetectee: 'LIQUIDATION' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeMontantCondamnation(input)).toBeNull();
    expect(Rules.computeSituationEmployeur(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only montantCondamnationCph present', () => {
    const input = { aiData: { montantCondamnationCph: 8500 } };
    expect(Rules.computeMontantCondamnation(input)).toBe(8500);
    expect(Rules.computeSituationEmployeur(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only situationEmployeurDetectee present', () => {
    const input = { aiData: { situationEmployeurDetectee: 'REDRESSEMENT' } };
    expect(Rules.computeSituationEmployeur(input)).toBe('REDRESSEMENT');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = { aiData: { montantCondamnationCph: 15000, situationEmployeurDetectee: 'LIQUIDATION' } };
    expect(Rules.computeMontantCondamnation(input)).toBe(15000);
    expect(Rules.computeSituationEmployeur(input)).toBe('LIQUIDATION');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects non-positive / non-finite / non-number montants', () => {
    expect(Rules.computeMontantCondamnation({ aiData: { montantCondamnationCph: 0 } })).toBeNull();
    expect(Rules.computeMontantCondamnation({ aiData: { montantCondamnationCph: -100 } })).toBeNull();
    expect(Rules.computeMontantCondamnation({ aiData: { montantCondamnationCph: NaN } })).toBeNull();
    expect(Rules.computeMontantCondamnation({ aiData: { montantCondamnationCph: '12000' as unknown as number } })).toBeNull();
    expect(Rules.computeMontantCondamnation({ aiData: { montantCondamnationCph: null } })).toBeNull();
  });

  it('rejects unknown / non-string situation values', () => {
    expect(Rules.computeSituationEmployeur({ aiData: { situationEmployeurDetectee: 'FAILLITE' } })).toBeNull();
    expect(Rules.computeSituationEmployeur({ aiData: { situationEmployeurDetectee: 42 as unknown as string } })).toBeNull();
    expect(Rules.computeSituationEmployeur({ aiData: { situationEmployeurDetectee: null } })).toBeNull();
  });

  it('does NOT count executionJugementCphEnvisagee flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { executionJugementCphEnvisagee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { montantCondamnationCph: 9000, situationEmployeurDetectee: 'IN_BONIS' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
