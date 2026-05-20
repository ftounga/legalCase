import { C4OnemChecklistPrefillRules } from './c4-onem-checklist-section-prefill-rules';

describe('C4OnemChecklistPrefillRules', () => {

  describe('computeRaisonSocialeEmployeur', () => {
    it('returns raisonSocialeEmployeur when present', () => {
      expect(C4OnemChecklistPrefillRules.computeRaisonSocialeEmployeur({
        aiData: { raisonSocialeEmployeur: 'ACME BVBA' },
      })).toBe('ACME BVBA');
    });

    it('falls back to nomEmployeur', () => {
      expect(C4OnemChecklistPrefillRules.computeRaisonSocialeEmployeur({
        aiData: { nomEmployeur: 'ACME' },
      })).toBe('ACME');
    });

    it('returns null when both null/empty', () => {
      expect(C4OnemChecklistPrefillRules.computeRaisonSocialeEmployeur({
        aiData: { raisonSocialeEmployeur: '', nomEmployeur: null },
      })).toBeNull();
    });
  });

  describe('computeNumeroBce', () => {
    it('returns BCE when 10 digits', () => {
      expect(C4OnemChecklistPrefillRules.computeNumeroBce({
        aiData: { numeroBce: '0123456789' },
      })).toBe('0123456789');
    });

    it('strips non-digits and validates length', () => {
      expect(C4OnemChecklistPrefillRules.computeNumeroBce({
        aiData: { numeroBce: '0123.456.789' },
      })).toBe('0123456789');
    });

    it('returns null when < 10 digits', () => {
      expect(C4OnemChecklistPrefillRules.computeNumeroBce({
        aiData: { numeroBce: '12345' },
      })).toBeNull();
    });

    it('falls back to bceEmployeur', () => {
      expect(C4OnemChecklistPrefillRules.computeNumeroBce({
        aiData: { bceEmployeur: '9876543210' },
      })).toBe('9876543210');
    });
  });

  describe('computeDateEntreeService / computeDateSortieService', () => {
    it('returns ISO date when valid', () => {
      expect(C4OnemChecklistPrefillRules.computeDateEntreeService({
        aiData: { dateEntree: '2020-03-15' },
      })).toBe('2020-03-15');
    });

    it('rejects non-ISO format', () => {
      expect(C4OnemChecklistPrefillRules.computeDateEntreeService({
        aiData: { dateEntree: '15/03/2020' },
      })).toBeNull();
    });

    it('dateSortie prefers dateRuptureContrat over dateLicenciement', () => {
      expect(C4OnemChecklistPrefillRules.computeDateSortieService({
        aiData: {
          dateRuptureContrat: '2025-09-30',
          dateLicenciement: '2025-08-01',
        },
      })).toBe('2025-09-30');
    });

    it('dateSortie falls back to dateLicenciement', () => {
      expect(C4OnemChecklistPrefillRules.computeDateSortieService({
        aiData: { dateLicenciement: '2025-08-01' },
      })).toBe('2025-08-01');
    });
  });

  describe('computeMotifExplicite', () => {
    it('returns motifExplicite when ≥ 5 chars', () => {
      expect(C4OnemChecklistPrefillRules.computeMotifExplicite({
        aiData: { motifExplicite: 'Licenciement économique' },
      })).toBe('Licenciement économique');
    });

    it('rejects motifExplicite < 5 chars', () => {
      expect(C4OnemChecklistPrefillRules.computeMotifExplicite({
        aiData: { motifExplicite: 'ABC' },
      })).toBeNull();
    });

    it('falls back to motifLicenciement', () => {
      expect(C4OnemChecklistPrefillRules.computeMotifExplicite({
        aiData: { motifLicenciement: 'Insuffisance professionnelle' },
      })).toBe('Insuffisance professionnelle');
    });

    it('falls back to motifRupture as last resort', () => {
      expect(C4OnemChecklistPrefillRules.computeMotifExplicite({
        aiData: { motifRupture: 'rupture amiable' },
      })).toBe('rupture amiable');
    });
  });

  describe('computeFauteGraveMentionnee', () => {
    it('detects "faute grave" in motifRupture', () => {
      expect(C4OnemChecklistPrefillRules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'licenciement pour faute grave' },
      })).toBe(true);
    });

    it('detects in upper-case', () => {
      expect(C4OnemChecklistPrefillRules.computeFauteGraveMentionnee({
        aiData: { motifExplicite: 'LICENCIEMENT POUR FAUTE GRAVE' },
      })).toBe(true);
    });

    it('detects "motif grave" variant', () => {
      expect(C4OnemChecklistPrefillRules.computeFauteGraveMentionnee({
        aiData: { motifLicenciement: 'motif grave' },
      })).toBe(true);
    });

    it('returns null when no keyword found', () => {
      expect(C4OnemChecklistPrefillRules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'démission' },
      })).toBeNull();
    });

    it('returns null when no motif fields', () => {
      expect(C4OnemChecklistPrefillRules.computeFauteGraveMentionnee({
        aiData: {},
      })).toBeNull();
    });
  });

  describe('computePreavisPresteJours / computeDernierSalaireMensuelBrut', () => {
    it('preavis : returns integer value', () => {
      expect(C4OnemChecklistPrefillRules.computePreavisPresteJours({
        aiData: { preavisPresteJours: 90 },
      })).toBe(90);
    });

    it('preavis : rejects negative', () => {
      expect(C4OnemChecklistPrefillRules.computePreavisPresteJours({
        aiData: { preavisPresteJours: -5 },
      })).toBeNull();
    });

    it('salaire : direct value', () => {
      expect(C4OnemChecklistPrefillRules.computeDernierSalaireMensuelBrut({
        aiData: { dernierSalaireMensuelBrut: 3500.5 },
      })).toBe(3500.5);
    });

    it('salaire : fallback to salaireBrutMensuel', () => {
      expect(C4OnemChecklistPrefillRules.computeDernierSalaireMensuelBrut({
        aiData: { salaireBrutMensuel: 2800 },
      })).toBe(2800);
    });
  });

  describe('computePrefillCount', () => {
    it('returns 0 for empty input', () => {
      expect(C4OnemChecklistPrefillRules.computePrefillCount({})).toBe(0);
    });

    it('counts every non-null field once', () => {
      expect(C4OnemChecklistPrefillRules.computePrefillCount({
        aiData: {
          raisonSocialeEmployeur: 'ACME',
          numeroBce: '1234567890',
          nomSalarie: 'Jean Dupont',
          dateEntree: '2020-01-01',
          dateRuptureContrat: '2025-01-01',
          categorieOnem: '1',
          motifExplicite: 'Licenciement avec préavis',
          motifRupture: 'faute grave',
          preavisPresteJours: 90,
          dernierSalaireMensuelBrut: 3500,
        },
      })).toBe(10);
    });

    it('counts fallback fields equally', () => {
      expect(C4OnemChecklistPrefillRules.computePrefillCount({
        aiData: {
          nomEmployeur: 'ACME',
          bceEmployeur: '0123456789',
          nomSalarie: 'Jean',
          dateEntree: '2020-01-01',
          dateLicenciement: '2025-01-01',
          motifLicenciement: 'Insuffisance professionnelle',
          salaireBrutMensuel: 3500,
        },
      })).toBe(7);
    });
  });
});
