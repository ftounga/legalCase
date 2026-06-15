import {
  CongesPayesSectionPrefillRules,
  computePrefillCount,
} from './conges-payes-section-prefill-rules';

describe('CongesPayesSectionPrefillRules', () => {
  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500, dateLicenciement: '2024-05-01' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — salaire seul retourne 2 (salaire + total dérivé SF-246-22)', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(2);
  });

  it('cas N — salaire + date retourne 3 (avec total dérivé)', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500, dateLicenciement: '2024-05-01' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(3);
  });

  // SF-246-22 — total rémunérations de la période = salaire × 12 (FR uniquement).
  it('computeTotalRemunerationPeriodeEur — FR salaire 2500 → 30000', () => {
    expect(
      CongesPayesSectionPrefillRules.computeTotalRemunerationPeriodeEur({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(30000);
  });

  it('computeTotalRemunerationPeriodeEur — BELGIQUE ou salaire absent → null', () => {
    expect(
      CongesPayesSectionPrefillRules.computeTotalRemunerationPeriodeEur({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBeNull();
    expect(
      CongesPayesSectionPrefillRules.computeTotalRemunerationPeriodeEur({
        aiData: {},
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('expose CongesPayesSectionPrefillRules barrel', () => {
    expect(CongesPayesSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
