import { NaoNegociationAnnuellePrefillRules as Rules } from './nao-negociation-annuelle-section-prefill-rules';

describe('NaoNegociationAnnuellePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { pseNombreSalaries: 80, delegueSyndicalPresent: true },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeEffectif(input)).toBeNull();
    expect(Rules.computeDelegueSyndicalPresent(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only effectif present', () => {
    const input = { aiData: { pseNombreSalaries: 80 } };
    expect(Rules.computeEffectif(input)).toBe(80);
    expect(Rules.computeDelegueSyndicalPresent(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only delegueSyndicalPresent present', () => {
    const input = { aiData: { delegueSyndicalPresent: true } };
    expect(Rules.computeDelegueSyndicalPresent(input)).toBe(true);
    expect(Rules.computeEffectif(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('maps delegueSyndicalPresent=false (booléen explicite, compte comme pré-rempli)', () => {
    const input = { aiData: { delegueSyndicalPresent: false } };
    expect(Rules.computeDelegueSyndicalPresent(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = {
      aiData: { pseNombreSalaries: 120, delegueSyndicalPresent: true },
    };
    expect(Rules.computeEffectif(input)).toBe(120);
    expect(Rules.computeDelegueSyndicalPresent(input)).toBe(true);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('truncates a non-integer effectif and rejects non-positive / non-numeric', () => {
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: 50.7 } })).toBe(50);
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: 0 } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: -3 } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: '80' as unknown as number } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: null } })).toBeNull();
  });

  it('rejects a non-boolean delegueSyndicalPresent', () => {
    expect(Rules.computeDelegueSyndicalPresent({ aiData: { delegueSyndicalPresent: 'oui' as unknown as boolean } })).toBeNull();
    expect(Rules.computeDelegueSyndicalPresent({ aiData: { delegueSyndicalPresent: 1 as unknown as boolean } })).toBeNull();
    expect(Rules.computeDelegueSyndicalPresent({ aiData: { delegueSyndicalPresent: null } })).toBeNull();
  });

  it('does NOT count naoDetectee flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { naoDetectee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { pseNombreSalaries: 120, delegueSyndicalPresent: true },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
