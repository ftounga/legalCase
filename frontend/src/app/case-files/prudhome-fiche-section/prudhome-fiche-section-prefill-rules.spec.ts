import {
  PrudhomeFicheSectionPrefillRules,
  computePrefillCount,
  computeProfession,
  computeNomSalarie,
  computePrenomSalarie,
  computeAdresseSalarie,
  computeNomEmployeur,
  computeAdresseEmployeur,
  computeSiretEmployeur,
} from './prudhome-fiche-section-prefill-rules';

describe('PrudhomeFicheSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas partiel — seulement profession retourne 1', () => {
      expect(computePrefillCount({ aiData: { poste: 'Développeur' } })).toBe(1);
    });

    it('cas partiel — nom + prenom retourne 2', () => {
      expect(computePrefillCount({ aiData: { nomSalarie: 'Dupont', prenomSalarie: 'Jean' } })).toBe(2);
    });

    it('cas partiel — profession + nomSalarie retourne 2', () => {
      expect(computePrefillCount({ aiData: { poste: 'Dev', nomSalarie: 'Martin' } })).toBe(2);
    });

    it('cas nominal — tous les champs renseignés retourne 7', () => {
      expect(computePrefillCount({
        aiData: {
          poste: 'Développeur',
          nomSalarie: 'Dupont',
          prenomSalarie: 'Jean',
          adresseSalarie: '1 rue de la Paix',
          nomEmployeur: 'ACME Corp',
          adresseEmployeur: '2 avenue des Champs',
          siretEmployeur: '12345678901234',
        },
      })).toBe(7);
    });

    it('champs vides ou null ne comptent pas', () => {
      expect(computePrefillCount({
        aiData: {
          nomSalarie: '',
          prenomSalarie: null,
          siretEmployeur: '',
        },
      })).toBe(0);
    });
  });

  describe('computeProfession', () => {
    it('retourne null si poste absent', () => {
      expect(computeProfession({})).toBeNull();
      expect(computeProfession({ aiData: { poste: null } })).toBeNull();
      expect(computeProfession({ aiData: { poste: '' } })).toBeNull();
    });

    it('retourne la valeur si poste renseigné', () => {
      expect(computeProfession({ aiData: { poste: 'Développeur' } })).toBe('Développeur');
    });
  });

  describe('computeNomSalarie (SF-246-15)', () => {
    it('retourne null si absent ou vide', () => {
      expect(computeNomSalarie({})).toBeNull();
      expect(computeNomSalarie({ aiData: { nomSalarie: null } })).toBeNull();
      expect(computeNomSalarie({ aiData: { nomSalarie: '' } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNomSalarie({ aiData: { nomSalarie: 'Dupont' } })).toBe('Dupont');
    });
  });

  describe('computePrenomSalarie (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computePrenomSalarie({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computePrenomSalarie({ aiData: { prenomSalarie: 'Marie' } })).toBe('Marie');
    });
  });

  describe('computeAdresseSalarie (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeAdresseSalarie({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeAdresseSalarie({ aiData: { adresseSalarie: '1 rue Paris' } })).toBe('1 rue Paris');
    });
  });

  describe('computeNomEmployeur (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeNomEmployeur({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeNomEmployeur({ aiData: { nomEmployeur: 'ACME Corp' } })).toBe('ACME Corp');
    });
  });

  describe('computeAdresseEmployeur (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeAdresseEmployeur({})).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeAdresseEmployeur({ aiData: { adresseEmployeur: '2 av Paris' } })).toBe('2 av Paris');
    });
  });

  describe('computeSiretEmployeur (SF-246-15)', () => {
    it('retourne null si absent', () => {
      expect(computeSiretEmployeur({})).toBeNull();
      expect(computeSiretEmployeur({ aiData: { siretEmployeur: null } })).toBeNull();
    });

    it('retourne la valeur si renseigné', () => {
      expect(computeSiretEmployeur({ aiData: { siretEmployeur: '12345678901234' } })).toBe('12345678901234');
    });
  });

  it('expose PrudhomeFicheSectionPrefillRules barrel', () => {
    expect(PrudhomeFicheSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(PrudhomeFicheSectionPrefillRules.computeNomSalarie).toBe(computeNomSalarie);
    expect(PrudhomeFicheSectionPrefillRules.computeSiretEmployeur).toBe(computeSiretEmployeur);
  });
});
