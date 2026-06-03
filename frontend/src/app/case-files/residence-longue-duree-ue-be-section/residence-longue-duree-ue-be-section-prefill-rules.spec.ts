import { ResidenceLongueDureeUeBePrefillRules as Rules } from './residence-longue-duree-ue-be-section-prefill-rules';

describe('ResidenceLongueDureeUeBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        rlueDateDebutSejour: '2020-01-01',
        rlueRessourcesSuffisantes: true,
        rlueAssuranceMaladie: true,
        rlueIntegrationRemplie: true,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDebut(input)).toBeNull();
    expect(Rules.computeRessources(input)).toBeNull();
    expect(Rules.computeAssurance(input)).toBeNull();
    expect(Rules.computeIntegration(input)).toBeNull();
  });

  it('returns 1 when only date present (BELGIQUE)', () => {
    const input = {
      aiData: { rlueDateDebutSejour: '2020-01-01' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateDebut(input)).toBe('2020-01-01');
    expect(Rules.computeRessources(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 4 (nominal) when all real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        rlueDateDebutSejour: '2020-01-01',
        rlueRessourcesSuffisantes: true,
        rlueAssuranceMaladie: false,
        rlueIntegrationRemplie: true,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateDebut(input)).toBe('2020-01-01');
    expect(Rules.computeRessources(input)).toBe(true);
    expect(Rules.computeAssurance(input)).toBe(false);
    expect(Rules.computeIntegration(input)).toBe(true);
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('counts boolean false as a present prefilled value', () => {
    const input = {
      aiData: {
        rlueRessourcesSuffisantes: false,
        rlueAssuranceMaladie: false,
        rlueIntegrationRemplie: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeRessources(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('rejects malformed / invalid debut dates', () => {
    expect(Rules.computeDateDebut({
      aiData: { rlueDateDebutSejour: '01/01/2020' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateDebut({
      aiData: { rlueDateDebutSejour: '2020-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateDebut({
      aiData: { rlueDateDebutSejour: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('rejects non-boolean values for booleans', () => {
    expect(Rules.computeRessources({
      aiData: { rlueRessourcesSuffisantes: 'true' as unknown as boolean },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields continuité / absences hors UE', () => {
    const input = {
      aiData: {
        rlueDateDebutSejour: '2020-01-01',
        rlueRessourcesSuffisantes: true,
        rlueAssuranceMaladie: true,
        rlueIntegrationRemplie: true,
        // simulate AI accidentally returning appréciation fields — must be ignored:
        sejourLegalIninterrompu: true,
        absencesHorsUeExcessives: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('does NOT count other Immigration BE tool fields (carte B séjour illimité)', () => {
    const input = {
      aiData: {
        carteBDateDebutSejour: '2020-01-01',
        carteBSejourIninterrompu: true,
        carteBMotifStable: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
