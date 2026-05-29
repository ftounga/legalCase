import { RegroupementFamilialPrefillRules as Rules } from './regroupement-familial-section-prefill-rules';

describe('RegroupementFamilialPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        aesDureePresenceMois: 24,
        regroupementRessourcesMensuelles: 1800,
        regroupementType: 'CONJOINT',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only dureeSejourRegulierMois is present', () => {
    const input = { aiData: { aesDureePresenceMois: 30 } };
    expect(Rules.computeDureeSejourRegulierMois(input)).toBe(30);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('truncates and rejects negative / non-finite duree', () => {
    expect(Rules.computeDureeSejourRegulierMois({ aiData: { aesDureePresenceMois: 24.7 } })).toBe(24);
    expect(Rules.computeDureeSejourRegulierMois({ aiData: { aesDureePresenceMois: -3 } })).toBeNull();
    expect(Rules.computeDureeSejourRegulierMois({
      aiData: { aesDureePresenceMois: '24' as unknown as number },
    })).toBeNull();
    expect(Rules.computeDureeSejourRegulierMois({ aiData: { aesDureePresenceMois: null } })).toBeNull();
  });

  it('returns ressources when finite and >= 0', () => {
    expect(Rules.computeRessourcesMensuellesNettes({ aiData: { regroupementRessourcesMensuelles: 1800 } }))
      .toBe(1800);
    expect(Rules.computeRessourcesMensuellesNettes({ aiData: { regroupementRessourcesMensuelles: 0 } }))
      .toBe(0);
  });

  it('rejects negative / non-numeric ressources', () => {
    expect(Rules.computeRessourcesMensuellesNettes({ aiData: { regroupementRessourcesMensuelles: -1 } }))
      .toBeNull();
    expect(Rules.computeRessourcesMensuellesNettes({
      aiData: { regroupementRessourcesMensuelles: '1800' as unknown as number },
    })).toBeNull();
    expect(Rules.computeRessourcesMensuellesNettes({ aiData: { regroupementRessourcesMensuelles: null } }))
      .toBeNull();
  });

  it('returns typeRegroupement when whitelist CONJOINT/ENFANT_MINEUR/AUTRE', () => {
    expect(Rules.computeTypeRegroupement({ aiData: { regroupementType: 'CONJOINT' } })).toBe('CONJOINT');
    expect(Rules.computeTypeRegroupement({ aiData: { regroupementType: 'ENFANT_MINEUR' } })).toBe('ENFANT_MINEUR');
    expect(Rules.computeTypeRegroupement({ aiData: { regroupementType: 'AUTRE' } })).toBe('AUTRE');
  });

  it('normalizes typeRegroupement case (conjoint → CONJOINT)', () => {
    expect(Rules.computeTypeRegroupement({
      aiData: { regroupementType: 'conjoint' as unknown as 'CONJOINT' },
    })).toBe('CONJOINT');
  });

  it('rejects typeRegroupement outside whitelist', () => {
    expect(Rules.computeTypeRegroupement({
      aiData: { regroupementType: 'PARENT' as unknown as 'AUTRE' },
    })).toBeNull();
    expect(Rules.computeTypeRegroupement({ aiData: { regroupementType: null } })).toBeNull();
  });

  it('returns 3 (partiel) when only duree + type present (getPrefillCount nominal partiel)', () => {
    const input = {
      aiData: {
        aesDureePresenceMois: 24,
        regroupementType: 'CONJOINT',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 3 when all 3 prefill fields are present (getPrefillCount nominal complet)', () => {
    const input = {
      aiData: {
        aesDureePresenceMois: 24,
        regroupementRessourcesMensuelles: 1800,
        regroupementType: 'ENFANT_MINEUR',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('does NOT count regroupementFamilialEnvisage alone (signal global, pas champ saisissable)', () => {
    const input = { aiData: { regroupementFamilialEnvisage: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
