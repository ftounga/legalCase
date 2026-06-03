import { VpfJeuneMajeurPrefillRules as Rules } from './vpf-jeune-majeur-section-prefill-rules';

describe('VpfJeuneMajeurPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        jeuneMajeurAge: 18,
        jeuneMajeurEntreMineur: true,
        jeuneMajeurPriseEnChargeAse: true,
        jeuneMajeurScolarise: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns age when non-negative number, rejects invalid', () => {
    expect(Rules.computeAge({ aiData: { jeuneMajeurAge: 18 } })).toBe(18);
    expect(Rules.computeAge({ aiData: { jeuneMajeurAge: 0 } })).toBe(0);
    expect(Rules.computeAge({ aiData: { jeuneMajeurAge: -2 } })).toBeNull();
    expect(Rules.computeAge({
      aiData: { jeuneMajeurAge: '18' as unknown as number },
    })).toBeNull();
    expect(Rules.computeAge({ aiData: { jeuneMajeurAge: null } })).toBeNull();
  });

  it('returns entreMineur when boolean, rejects non-boolean', () => {
    expect(Rules.computeEntreMineur({ aiData: { jeuneMajeurEntreMineur: true } })).toBe(true);
    expect(Rules.computeEntreMineur({ aiData: { jeuneMajeurEntreMineur: false } })).toBe(false);
    expect(Rules.computeEntreMineur({
      aiData: { jeuneMajeurEntreMineur: 'true' as unknown as boolean },
    })).toBeNull();
  });

  it('returns priseEnChargeAse when boolean, rejects non-boolean', () => {
    expect(Rules.computePriseEnChargeAse({ aiData: { jeuneMajeurPriseEnChargeAse: true } })).toBe(true);
    expect(Rules.computePriseEnChargeAse({ aiData: { jeuneMajeurPriseEnChargeAse: false } })).toBe(false);
    expect(Rules.computePriseEnChargeAse({
      aiData: { jeuneMajeurPriseEnChargeAse: 1 as unknown as boolean },
    })).toBeNull();
  });

  it('returns scolariseOuFormation when boolean, rejects non-boolean', () => {
    expect(Rules.computeScolariseOuFormation({ aiData: { jeuneMajeurScolarise: true } })).toBe(true);
    expect(Rules.computeScolariseOuFormation({ aiData: { jeuneMajeurScolarise: false } })).toBe(false);
    expect(Rules.computeScolariseOuFormation({ aiData: { jeuneMajeurScolarise: null } })).toBeNull();
  });

  it('returns 1 when only age is present (partiel)', () => {
    expect(Rules.computePrefillCount({ aiData: { jeuneMajeurAge: 18 } })).toBe(1);
  });

  it('returns 4 when all 4 prefill fields are present (complet)', () => {
    const input = {
      aiData: {
        jeuneMajeurAge: 18,
        jeuneMajeurEntreMineur: true,
        jeuneMajeurPriseEnChargeAse: true,
        jeuneMajeurScolarise: false,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});
