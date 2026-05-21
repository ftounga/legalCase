import {
  ContestationFiliationBeSectionPrefillRules,
  computePrefillCount,
} from './contestation-filiation-be-section-prefill-rules';

describe('ContestationFiliationBeSectionPrefillRules — SF-217-19', () => {
  // PREFILL_COUNT_ALWAYS_ZERO = true → toujours 0 en V1.

  it('retourne 0 sur input vide', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('retourne 0 si aiData null', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('retourne 0 si aiData fourni avec des champs Famille (non utilisés)', () => {
    expect(
      computePrefillCount({
        aiData: { dateMariageBeDetectee: '2015-06-20' },
      }),
    ).toBe(0);
  });

  it('retourne 0 pour les 6 fixtures canoniques', () => {
    const fixtures: ReadonlyArray<Record<string, unknown>> = [
      {},
      { aiData: null },
      { aiData: undefined, procedureChecks: [], aiQuestions: [], piecesManquantes: [] },
      { aiData: {} },
      { workspaceCountry: 'FRANCE' },
      { workspaceCountry: 'BELGIQUE' },
    ];
    for (const f of fixtures) {
      expect(computePrefillCount(f)).toBe(0);
    }
  });

  it('expose le barrel ContestationFiliationBeSectionPrefillRules', () => {
    expect(
      ContestationFiliationBeSectionPrefillRules.computePrefillCount,
    ).toBe(computePrefillCount);
  });
});
