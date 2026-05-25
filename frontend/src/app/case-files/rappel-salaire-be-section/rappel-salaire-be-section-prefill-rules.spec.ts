import { RappelSalaireBeSectionPrefillRules as R } from './rappel-salaire-be-section-prefill-rules';

/**
 * SF-213-02b — tests unitaires du helper pur.
 * Garantit la parité runtime/static (`prefill-count-integrity.spec.ts`).
 */
describe('RappelSalaireBeSectionPrefillRules', () => {

  describe('Gate workspaceCountry', () => {
    it('FRANCE → tous les compute* retournent null + count 0', () => {
      const input = {
        workspaceCountry: 'FRANCE',
        aiData: {
          montantArrieresSalaireBrut: 5000,
          dateDebutArrieresSalaire: '2025-01-01',
          dateFinArrieresSalaire: '2025-12-31',
          dateRuptureContrat: '2025-12-31',
        },
      };
      expect(R.computeMontantBrut(input)).toBeNull();
      expect(R.computeDateDebutPeriode(input)).toBeNull();
      expect(R.computeDateFinPeriode(input)).toBeNull();
      expect(R.computeDateRupture(input)).toBeNull();
      expect(R.computeTypeArriereEnum(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('workspaceCountry absent → tous null', () => {
      expect(R.computePrefillCount({ aiData: { montantArrieresSalaireBrut: 5000 } })).toBe(0);
    });
  });

  describe('aiData absent', () => {
    it('aiData null → tous null (0)', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: null };
      expect(R.computeMontantBrut(input)).toBeNull();
      expect(R.computeDateDebutPeriode(input)).toBeNull();
      expect(R.computeDateFinPeriode(input)).toBeNull();
      expect(R.computeDateRupture(input)).toBeNull();
      expect(R.computeTypeArriereEnum(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData undefined → tous null', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: undefined };
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData {} → tous null', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });
  });

  describe('computeMontantBrut (positive number)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('valeur > 0 → retourne la valeur', () => {
      expect(R.computeMontantBrut({ ...base, aiData: { montantArrieresSalaireBrut: 5000 } })).toBe(5000);
    });

    it('valeur = 0 → null', () => {
      expect(R.computeMontantBrut({ ...base, aiData: { montantArrieresSalaireBrut: 0 } })).toBeNull();
    });

    it('valeur négative → null', () => {
      expect(R.computeMontantBrut({ ...base, aiData: { montantArrieresSalaireBrut: -100 } })).toBeNull();
    });

    it('NaN / non-number → null', () => {
      expect(R.computeMontantBrut({ ...base, aiData: { montantArrieresSalaireBrut: NaN } })).toBeNull();
      expect(R.computeMontantBrut({ ...base, aiData: { montantArrieresSalaireBrut: '5000' as any } })).toBeNull();
    });
  });

  describe('computeDateDebutPeriode (ISO YYYY-MM-DD)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('ISO valide → retourne la date', () => {
      expect(R.computeDateDebutPeriode({ ...base, aiData: { dateDebutArrieresSalaire: '2025-01-01' } }))
        .toBe('2025-01-01');
    });

    it('format invalide → null', () => {
      expect(R.computeDateDebutPeriode({ ...base, aiData: { dateDebutArrieresSalaire: '01/01/2025' } })).toBeNull();
      expect(R.computeDateDebutPeriode({ ...base, aiData: { dateDebutArrieresSalaire: '2025-1-1' } })).toBeNull();
    });

    it('date impossible (2025-02-31) → null', () => {
      expect(R.computeDateDebutPeriode({ ...base, aiData: { dateDebutArrieresSalaire: '2025-02-31' } })).toBeNull();
    });

    it('non-string → null', () => {
      expect(R.computeDateDebutPeriode({ ...base, aiData: { dateDebutArrieresSalaire: 20250101 as any } })).toBeNull();
    });

    it('string vide → null', () => {
      expect(R.computeDateDebutPeriode({ ...base, aiData: { dateDebutArrieresSalaire: '' } })).toBeNull();
    });
  });

  describe('computeDateFinPeriode (ISO YYYY-MM-DD)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('ISO valide → retourne la date', () => {
      expect(R.computeDateFinPeriode({ ...base, aiData: { dateFinArrieresSalaire: '2025-12-31' } }))
        .toBe('2025-12-31');
    });

    it('format invalide → null', () => {
      expect(R.computeDateFinPeriode({ ...base, aiData: { dateFinArrieresSalaire: '31-12-2025' } })).toBeNull();
    });
  });

  describe('computeDateRupture (réutilise dateRuptureContrat SF-207-01)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('ISO valide → retourne la date', () => {
      expect(R.computeDateRupture({ ...base, aiData: { dateRuptureContrat: '2025-06-15' } }))
        .toBe('2025-06-15');
    });

    it('absent → null', () => {
      expect(R.computeDateRupture({ ...base, aiData: {} })).toBeNull();
    });
  });

  describe('computeTypeArriereEnum (dérivé)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('dateRupture présente → POST_RUPTURE', () => {
      expect(R.computeTypeArriereEnum({ ...base, aiData: { dateRuptureContrat: '2025-06-15' } }))
        .toBe('POST_RUPTURE');
    });

    it('dateRupture absente → PENDANT_CONTRAT', () => {
      expect(R.computeTypeArriereEnum({ ...base, aiData: { montantArrieresSalaireBrut: 5000 } }))
        .toBe('PENDANT_CONTRAT');
    });

    it('dateRupture ISO invalide → PENDANT_CONTRAT (traité comme absente)', () => {
      expect(R.computeTypeArriereEnum({ ...base, aiData: { dateRuptureContrat: 'pas-une-date' } }))
        .toBe('PENDANT_CONTRAT');
    });

    it('aiData absent → null', () => {
      expect(R.computeTypeArriereEnum({ ...base, aiData: null })).toBeNull();
    });
  });

  describe('computePrefillCount (intégration — 4 champs max)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('0 champ → 0', () => {
      expect(R.computePrefillCount({ ...base, aiData: {} })).toBe(0);
    });

    it('1 champ (montant) → 1', () => {
      expect(R.computePrefillCount({ ...base, aiData: { montantArrieresSalaireBrut: 5000 } })).toBe(1);
    });

    it('2 champs (montant + dateDebut) → 2', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: { montantArrieresSalaireBrut: 5000, dateDebutArrieresSalaire: '2025-01-01' },
      })).toBe(2);
    });

    it('3 champs (montant + dateDebut + dateFin) → 3', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: {
          montantArrieresSalaireBrut: 5000,
          dateDebutArrieresSalaire: '2025-01-01',
          dateFinArrieresSalaire: '2025-12-31',
        },
      })).toBe(3);
    });

    it('4 champs (tous) → 4 (max théorique)', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: {
          montantArrieresSalaireBrut: 5000,
          dateDebutArrieresSalaire: '2025-01-01',
          dateFinArrieresSalaire: '2025-12-31',
          dateRuptureContrat: '2025-12-31',
        },
      })).toBe(4);
    });

    it('typeArriereEnum dérivé n\'est PAS compté séparément', () => {
      // 3 champs IA présents + dateRupture qui dérive le type → count = 4, pas 5.
      const input = {
        ...base,
        aiData: {
          montantArrieresSalaireBrut: 5000,
          dateDebutArrieresSalaire: '2025-01-01',
          dateFinArrieresSalaire: '2025-12-31',
          dateRuptureContrat: '2025-12-31',
        },
      };
      expect(R.computePrefillCount(input)).toBe(4);
      // Sanity : le dérivé existe bien à côté.
      expect(R.computeTypeArriereEnum(input)).toBe('POST_RUPTURE');
    });
  });
});
