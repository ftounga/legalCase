import {
  LicenciementBeStatutUniquePreavisSectionPrefillRules as R,
} from './licenciement-be-statut-unique-preavis-section-prefill-rules';

/**
 * SF-213-03b — tests unitaires du helper pur.
 * Garantit la parité runtime/static (`prefill-count-integrity.spec.ts`).
 */
describe('LicenciementBeStatutUniquePreavisSectionPrefillRules', () => {

  describe('Gate workspaceCountry', () => {
    it('FRANCE → tous les compute* retournent null + count 0', () => {
      const input = {
        workspaceCountry: 'FRANCE',
        aiData: {
          dateEntree: '2018-06-01',
          dateLicenciement: '2026-03-15',
          salaireBrutMensuel: 3000,
          salaireBrutAnnuel: 36000,
        },
      };
      expect(R.computeAncienneteAnnees(input)).toBeNull();
      expect(R.computeAncienneteMoisSupplementaires(input)).toBeNull();
      expect(R.computeSalaireHebdomadaireBrut(input)).toBeNull();
      expect(R.computeDateNotificationLicenciement(input)).toBeNull();
      expect(R.computePartieStatutUniqueSeulement(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('workspaceCountry absent → tous null', () => {
      expect(R.computePrefillCount({ aiData: { dateEntree: '2018-06-01' } })).toBe(0);
    });
  });

  describe('aiData absent', () => {
    it('aiData null → tous null (0)', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: null };
      expect(R.computeAncienneteAnnees(input)).toBeNull();
      expect(R.computeSalaireHebdomadaireBrut(input)).toBeNull();
      expect(R.computeDateNotificationLicenciement(input)).toBeNull();
      expect(R.computePartieStatutUniqueSeulement(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData undefined → 0', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: undefined };
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData {} → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });
  });

  describe('computeAnciennete (couplé années/mois)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('dates valides → calcule années + mois', () => {
      const a = R.computeAnciennete({
        ...base,
        aiData: { dateEntree: '2020-01-15', dateLicenciement: '2026-07-15' },
      });
      expect(a).toEqual({ years: 6, months: 6 });
    });

    it('dateLicenciement absente → fallback today (ancienneté non-null calculable)', () => {
      const a = R.computeAnciennete({
        ...base,
        aiData: { dateEntree: '2020-01-15' },
      });
      expect(a).not.toBeNull();
      expect(a!.years).toBeGreaterThanOrEqual(5);
      expect(a!.months).toBeGreaterThanOrEqual(0);
      expect(a!.months).toBeLessThanOrEqual(11);
    });

    it('dateEntree absente → null', () => {
      expect(R.computeAnciennete({ ...base, aiData: { dateLicenciement: '2026-07-15' } })).toBeNull();
    });

    it('dateLicenciement < dateEntree → null (cas pathologique IA)', () => {
      expect(R.computeAnciennete({
        ...base,
        aiData: { dateEntree: '2026-07-15', dateLicenciement: '2020-01-15' },
      })).toBeNull();
    });

    it('jour résiduel négatif → décrémente le mois', () => {
      // dateEntree 2020-06-20, dateLicenciement 2026-06-10 → 5 ans 11 mois
      const a = R.computeAnciennete({
        ...base,
        aiData: { dateEntree: '2020-06-20', dateLicenciement: '2026-06-10' },
      });
      expect(a).toEqual({ years: 5, months: 11 });
    });

    it('même mois, même jour → exactement N années 0 mois', () => {
      const a = R.computeAnciennete({
        ...base,
        aiData: { dateEntree: '2020-03-15', dateLicenciement: '2026-03-15' },
      });
      expect(a).toEqual({ years: 6, months: 0 });
    });

    it('dateEntree ISO invalide → null', () => {
      expect(R.computeAnciennete({
        ...base,
        aiData: { dateEntree: '15/06/2020', dateLicenciement: '2026-07-15' },
      })).toBeNull();
    });
  });

  describe('computeAncienneteAnnees et computeAncienneteMoisSupplementaires', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('5 ans 6 mois → annees=5, mois=6', () => {
      const input = {
        ...base,
        aiData: { dateEntree: '2020-01-15', dateLicenciement: '2025-07-15' },
      };
      expect(R.computeAncienneteAnnees(input)).toBe(5);
      expect(R.computeAncienneteMoisSupplementaires(input)).toBe(6);
    });

    it('données insuffisantes → null sur les deux', () => {
      const input = { ...base, aiData: {} };
      expect(R.computeAncienneteAnnees(input)).toBeNull();
      expect(R.computeAncienneteMoisSupplementaires(input)).toBeNull();
    });
  });

  describe('computeSalaireHebdomadaireBrut', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('priorité salaireBrutAnnuel → annuel / 52', () => {
      const v = R.computeSalaireHebdomadaireBrut({
        ...base,
        aiData: { salaireBrutAnnuel: 52000 },
      });
      expect(v).toBe(1000);
    });

    it('fallback salaireBrutMensuel → mensuel × 12 / 52', () => {
      const v = R.computeSalaireHebdomadaireBrut({
        ...base,
        aiData: { salaireBrutMensuel: 3000 },
      });
      // 3000 × 12 / 52 = 692.3076923... → arrondi 692.31
      expect(v).toBeCloseTo(692.31, 2);
    });

    it('annuel ET mensuel présents → priorité à annuel', () => {
      const v = R.computeSalaireHebdomadaireBrut({
        ...base,
        aiData: { salaireBrutAnnuel: 52000, salaireBrutMensuel: 9999 },
      });
      expect(v).toBe(1000);
    });

    it('valeurs nulles / ≤ 0 → null', () => {
      expect(R.computeSalaireHebdomadaireBrut({ ...base, aiData: { salaireBrutAnnuel: 0 } })).toBeNull();
      expect(R.computeSalaireHebdomadaireBrut({ ...base, aiData: { salaireBrutMensuel: -100 } })).toBeNull();
    });

    it('non-number → null', () => {
      expect(R.computeSalaireHebdomadaireBrut({
        ...base,
        aiData: { salaireBrutAnnuel: '52000' as unknown as number },
      })).toBeNull();
    });
  });

  describe('computeDateNotificationLicenciement', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('ISO valide → date', () => {
      expect(R.computeDateNotificationLicenciement({
        ...base, aiData: { dateLicenciement: '2026-03-15' },
      })).toBe('2026-03-15');
    });

    it('format invalide → null', () => {
      expect(R.computeDateNotificationLicenciement({
        ...base, aiData: { dateLicenciement: '15/03/2026' },
      })).toBeNull();
    });

    it('date impossible → null', () => {
      expect(R.computeDateNotificationLicenciement({
        ...base, aiData: { dateLicenciement: '2026-02-31' },
      })).toBeNull();
    });
  });

  describe('computePartieStatutUniqueSeulement (dérivé dateEntree)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('dateEntree post-2014 → true (statut unique pur)', () => {
      expect(R.computePartieStatutUniqueSeulement({
        ...base, aiData: { dateEntree: '2018-06-01' },
      })).toBe(true);
    });

    it('dateEntree pile 2014-01-01 → true (frontière inclusive)', () => {
      expect(R.computePartieStatutUniqueSeulement({
        ...base, aiData: { dateEntree: '2014-01-01' },
      })).toBe(true);
    });

    it('dateEntree pré-2014 → false (contrat mixte)', () => {
      expect(R.computePartieStatutUniqueSeulement({
        ...base, aiData: { dateEntree: '2010-06-01' },
      })).toBe(false);
    });

    it('dateEntree absente / invalide → null', () => {
      expect(R.computePartieStatutUniqueSeulement({ ...base, aiData: {} })).toBeNull();
      expect(R.computePartieStatutUniqueSeulement({
        ...base, aiData: { dateEntree: 'pas-une-date' },
      })).toBeNull();
    });
  });

  describe('computePrefillCount (intégration — 3 champs max comptés)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('0 champ → 0', () => {
      expect(R.computePrefillCount({ ...base, aiData: {} })).toBe(0);
    });

    it('1 champ (ancienneté seule via dateEntree) → 1', () => {
      // dateEntree → ancienneté calculée (fallback today). Pas de salaire ni dateLicenciement.
      const input = { ...base, aiData: { dateEntree: '2020-01-15' } };
      expect(R.computePrefillCount(input)).toBe(1);
    });

    it('2 champs (ancienneté + salaire hebdo) → 2', () => {
      const input = {
        ...base,
        aiData: { dateEntree: '2020-01-15', salaireBrutAnnuel: 52000 },
      };
      expect(R.computePrefillCount(input)).toBe(2);
    });

    it('3 champs (ancienneté + salaire + dateNotification) → 3 (max théorique)', () => {
      const input = {
        ...base,
        aiData: {
          dateEntree: '2020-01-15',
          dateLicenciement: '2026-03-15',
          salaireBrutAnnuel: 52000,
        },
      };
      expect(R.computePrefillCount(input)).toBe(3);
    });

    it('partieStatutUniqueSeulement dérivé n\'est PAS compté', () => {
      // dateEntree seule pré-fill ancienneté ET dérive partieStatutUniqueSeulement → 1, pas 2.
      const input = { ...base, aiData: { dateEntree: '2020-01-15' } };
      expect(R.computePrefillCount(input)).toBe(1);
      expect(R.computePartieStatutUniqueSeulement(input)).toBe(true);
    });

    it('ancienneteAnnees + ancienneteMois (couple) comptés 1 seule fois', () => {
      // dateEntree + dateLicenciement → fournit annees ET mois en même temps,
      // mais c'est 1 source IA = 1 champ "ancienneté".
      const input = {
        ...base,
        aiData: { dateEntree: '2020-01-15', dateLicenciement: '2025-07-15' },
      };
      expect(R.computeAncienneteAnnees(input)).toBe(5);
      expect(R.computeAncienneteMoisSupplementaires(input)).toBe(6);
      expect(R.computePrefillCount(input)).toBe(2); // 1 ancienneté + 1 dateNotification (= dateLicenciement)
    });
  });
});
