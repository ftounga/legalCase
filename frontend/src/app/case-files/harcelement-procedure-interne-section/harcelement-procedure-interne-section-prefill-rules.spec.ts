import { HarcelementProcedureInternePrefillRules as Rules } from './harcelement-procedure-interne-section-prefill-rules';

describe('HarcelementProcedureInternePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { pseNombreSalaries: 50, harcelementSignalementInterne: true },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeEffectif(input)).toBeNull();
    expect(Rules.computeSignalementRecu(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only effectif present', () => {
    const input = { aiData: { pseNombreSalaries: 50 } };
    expect(Rules.computeEffectif(input)).toBe(50);
    expect(Rules.computeSignalementRecu(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only signalementRecu present', () => {
    const input = { aiData: { harcelementSignalementInterne: true } };
    expect(Rules.computeSignalementRecu(input)).toBe(true);
    expect(Rules.computeEffectif(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('maps signalementRecu=false (booléen explicite, compte comme pré-rempli)', () => {
    const input = { aiData: { harcelementSignalementInterne: false } };
    expect(Rules.computeSignalementRecu(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = {
      aiData: { pseNombreSalaries: 120, harcelementSignalementInterne: true },
    };
    expect(Rules.computeEffectif(input)).toBe(120);
    expect(Rules.computeSignalementRecu(input)).toBe(true);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('truncates a non-integer effectif and rejects non-positive / non-numeric', () => {
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: 11.9 } })).toBe(11);
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: 0 } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: -5 } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: '50' as unknown as number } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: null } })).toBeNull();
  });

  it('rejects a non-boolean signalementRecu', () => {
    expect(Rules.computeSignalementRecu({ aiData: { harcelementSignalementInterne: 'oui' as unknown as boolean } })).toBeNull();
    expect(Rules.computeSignalementRecu({ aiData: { harcelementSignalementInterne: 1 as unknown as boolean } })).toBeNull();
    expect(Rules.computeSignalementRecu({ aiData: { harcelementSignalementInterne: null } })).toBeNull();
  });

  it('does NOT count harcelementProcedureInterneDetectee flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { harcelementProcedureInterneDetectee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { pseNombreSalaries: 120, harcelementSignalementInterne: true },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
