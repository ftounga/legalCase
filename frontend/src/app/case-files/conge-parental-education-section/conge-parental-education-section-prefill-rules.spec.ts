import { CongeParentalEducationPrefillRules } from './conge-parental-education-section-prefill-rules';

describe('CongeParentalEducationPrefillRules', () => {

  it('computeDateNaissanceOuAdoption lit une date ISO valide depuis Sf218dDetail', () => {
    expect(CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption({
      aiData: { date_naissance_ou_adoption: '2025-03-01' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe('2025-03-01');
  });

  it('computeDateNaissanceOuAdoption retourne null si format invalide / absent', () => {
    expect(CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption({
      aiData: { date_naissance_ou_adoption: '01/03/2025' } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption({
      aiData: {} as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption({})).toBeNull();
  });

  it('computeDateNaissanceOuAdoption retourne null hors France', () => {
    expect(CongeParentalEducationPrefillRules.computeDateNaissanceOuAdoption({
      aiData: { date_naissance_ou_adoption: '2025-03-01' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computePrefillCount = 1 si date reconnue, 0 sinon', () => {
    expect(CongeParentalEducationPrefillRules.computePrefillCount({
      aiData: { date_naissance_ou_adoption: '2025-03-01' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CongeParentalEducationPrefillRules.computePrefillCount({})).toBe(0);
    expect(CongeParentalEducationPrefillRules.computePrefillCount({
      aiData: { date_naissance_ou_adoption: '2025-03-01' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
