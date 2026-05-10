import {
  TribunalTravailFicheSectionPrefillRules,
  computePrefillCount,
  computeTypeContrat,
} from './tribunal-travail-fiche-section-prefill-rules';

describe('TribunalTravailFicheSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — partiel (2 champs) retourne 2', () => {
      expect(
        computePrefillCount({
          aiData: {
            conventionCollective: 'CCN 2030',
            dateEntree: '2020-01-15',
          },
        }),
      ).toBe(2);
    });

    it('cas N — nominal 5 champs retourne 5', () => {
      expect(
        computePrefillCount({
          aiData: {
            conventionCollective: 'CCN 2030',
            typeContrat: 'CDI',
            dateEntree: '2020-01-15',
            dateLicenciement: '2024-09-30',
            motifLicenciement: 'ECONOMIQUE',
          },
        }),
      ).toBe(5);
    });
  });

  describe('computeTypeContrat', () => {
    it('mappe CDI → EMPLOYE', () => {
      expect(computeTypeContrat({ aiData: { typeContrat: 'CDI' } })).toBe('EMPLOYE');
    });
    it('mappe OUVRIER → OUVRIER', () => {
      expect(computeTypeContrat({ aiData: { typeContrat: 'Ouvrier' } })).toBe('OUVRIER');
    });
    it('mappe inconnu → null', () => {
      expect(computeTypeContrat({ aiData: { typeContrat: 'FREELANCE' } })).toBeNull();
    });
  });

  it('expose TribunalTravailFicheSectionPrefillRules barrel', () => {
    expect(TribunalTravailFicheSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
