import {
  TribunalTravailFicheSectionPrefillRules,
  computePrefillCount,
  computeTypeContrat,
  computeNomRequerant,
  computePrenomRequerant,
  computeDomicileRequerant,
  computeNomDefendeur,
  computeSiegeSocialDefendeur,
  computeNumeroBce,
} from './tribunal-travail-fiche-section-prefill-rules';

describe('TribunalTravailFicheSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas partiel — 2 champs retourne 2', () => {
      expect(
        computePrefillCount({
          aiData: {
            conventionCollective: 'CCN 2030',
            dateEntree: '2020-01-15',
          },
        }),
      ).toBe(2);
    });

    it('cas partiel — 5 champs originaux retourne 5', () => {
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

    it('cas partiel — identités seulement retourne 6 (SF-246-15)', () => {
      expect(
        computePrefillCount({
          aiData: {
            nomSalarie: 'Dupont',
            prenomSalarie: 'Jean',
            adresseSalarie: '1 rue Paris',
            nomEmployeur: 'ACME',
            adresseEmployeur: '2 av Lyon',
            bceEmployeur: 'BE0123456789',
          },
        }),
      ).toBe(6);
    });

    it('cas nominal — 11 champs retourne 11 (SF-246-15)', () => {
      expect(
        computePrefillCount({
          aiData: {
            conventionCollective: 'CCN 2030',
            typeContrat: 'CDI',
            dateEntree: '2020-01-15',
            dateLicenciement: '2024-09-30',
            motifLicenciement: 'ECONOMIQUE',
            nomSalarie: 'Dupont',
            prenomSalarie: 'Jean',
            adresseSalarie: '1 rue Paris',
            nomEmployeur: 'ACME',
            adresseEmployeur: '2 av Lyon',
            bceEmployeur: 'BE0123456789',
          },
        }),
      ).toBe(11);
    });

    it('champs vides ou null ne comptent pas', () => {
      expect(computePrefillCount({
        aiData: {
          nomSalarie: '',
          prenomSalarie: null,
          bceEmployeur: '',
        },
      })).toBe(0);
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

  describe('computeNomRequerant (SF-246-15)', () => {
    it('retourne null si absent ou vide', () => {
      expect(computeNomRequerant({})).toBeNull();
      expect(computeNomRequerant({ aiData: { nomSalarie: '' } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNomRequerant({ aiData: { nomSalarie: 'Dupont' } })).toBe('Dupont');
    });
  });

  describe('computePrenomRequerant (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computePrenomRequerant({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computePrenomRequerant({ aiData: { prenomSalarie: 'Marie' } })).toBe('Marie');
    });
  });

  describe('computeDomicileRequerant (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeDomicileRequerant({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeDomicileRequerant({ aiData: { adresseSalarie: '1 rue Paris' } })).toBe('1 rue Paris');
    });
  });

  describe('computeNomDefendeur (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeNomDefendeur({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNomDefendeur({ aiData: { nomEmployeur: 'ACME Corp' } })).toBe('ACME Corp');
    });
  });

  describe('computeSiegeSocialDefendeur (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeSiegeSocialDefendeur({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeSiegeSocialDefendeur({ aiData: { adresseEmployeur: '2 av Lyon' } })).toBe('2 av Lyon');
    });
  });

  describe('computeNumeroBce (SF-246-15)', () => {
    it('retourne null si absent ou vide', () => {
      expect(computeNumeroBce({})).toBeNull();
      expect(computeNumeroBce({ aiData: { bceEmployeur: null } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNumeroBce({ aiData: { bceEmployeur: 'BE0123456789' } })).toBe('BE0123456789');
    });
  });

  it('expose TribunalTravailFicheSectionPrefillRules barrel', () => {
    expect(TribunalTravailFicheSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(TribunalTravailFicheSectionPrefillRules.computeNomRequerant).toBe(computeNomRequerant);
    expect(TribunalTravailFicheSectionPrefillRules.computeNumeroBce).toBe(computeNumeroBce);
  });
});
