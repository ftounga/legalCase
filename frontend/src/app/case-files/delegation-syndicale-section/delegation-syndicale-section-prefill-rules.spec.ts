import { DelegationSyndicalePrefillRules as Rules } from './delegation-syndicale-section-prefill-rules';

describe('DelegationSyndicalePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { pseNombreSalaries: 80, mandatSyndicalType: 'DELEGUE_SYNDICAL' as const },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeEffectif(input)).toBeNull();
    expect(Rules.computeTypeMandat(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only effectif present', () => {
    const input = { aiData: { pseNombreSalaries: 80 } };
    expect(Rules.computeEffectif(input)).toBe(80);
    expect(Rules.computeTypeMandat(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only typeMandat present', () => {
    const input = { aiData: { mandatSyndicalType: 'RSS' as const } };
    expect(Rules.computeTypeMandat(input)).toBe('RSS');
    expect(Rules.computeEffectif(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = {
      aiData: { pseNombreSalaries: 120, mandatSyndicalType: 'DELEGUE_SYNDICAL' as const },
    };
    expect(Rules.computeEffectif(input)).toBe(120);
    expect(Rules.computeTypeMandat(input)).toBe('DELEGUE_SYNDICAL');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('truncates a non-integer effectif and rejects non-positive / non-numeric', () => {
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: 50.7 } })).toBe(50);
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: 0 } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: -3 } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: '80' as unknown as number } })).toBeNull();
    expect(Rules.computeEffectif({ aiData: { pseNombreSalaries: null } })).toBeNull();
  });

  it('rejects an unknown mandatSyndicalType value', () => {
    expect(Rules.computeTypeMandat({ aiData: { mandatSyndicalType: 'AUTRE' as never } })).toBeNull();
    expect(Rules.computeTypeMandat({ aiData: { mandatSyndicalType: null } })).toBeNull();
    expect(Rules.computeTypeMandat({ aiData: {} })).toBeNull();
  });

  it('does NOT count delegationSyndicaleDetectee flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { delegationSyndicaleDetectee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { pseNombreSalaries: 120, mandatSyndicalType: 'DELEGUE_SYNDICAL' as const },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
