import {
  LicenciementBeFormuleClaeysSectionPrefillRules as R,
} from './licenciement-be-formule-claeys-section-prefill-rules';

/**
 * SF-213-04b — tests unitaires du helper pur.
 * Garantit la parité runtime/static (`prefill-count-integrity.spec.ts`).
 */
describe('LicenciementBeFormuleClaeysSectionPrefillRules', () => {

  describe('Gate workspaceCountry', () => {
    it('FRANCE → tous les compute* retournent null + count 0', () => {
      const input = {
        workspaceCountry: 'FRANCE',
        aiData: {
          dateEntree: '2010-06-01',
          dateLicenciement: '2026-03-15',
          salaireBrutAnnuel: 60000,
        },
      };
      expect(R.computeAncienneteAnneesPreStatutUnique(input)).toBeNull();
      expect(R.computeAncienneteMoisPreStatutUnique(input)).toBeNull();
      expect(R.computeAncienneteAnneesPostStatutUnique(input)).toBeNull();
      expect(R.computeRemunerationAnnuelleBruteEnMilliers(input)).toBeNull();
      expect(R.computeSalaireHebdomadaireBrut(input)).toBeNull();
      expect(R.computeAppliquerClauseSauvegarde(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('workspaceCountry absent → 0', () => {
      expect(R.computePrefillCount({ aiData: { dateEntree: '2010-06-01' } })).toBe(0);
    });
  });

  describe('aiData absent', () => {
    it('aiData null → tous null', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: null };
      expect(R.computeAncienneteAnneesPreStatutUnique(input)).toBeNull();
      expect(R.computeRemunerationAnnuelleBruteEnMilliers(input)).toBeNull();
      expect(R.computeAppliquerClauseSauvegarde(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData undefined → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: undefined })).toBe(0);
    });

    it('aiData {} → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });
  });

  describe('computeAnciennetePreStatutUnique (de dateEntree à 2014-01-01)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('contrat pré-2014 → ancienneté pré calculée', () => {
      // 2008-06-15 → 2014-01-01 = 5 ans 6 mois
      const a = R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateEntree: '2008-06-15' },
      });
      expect(a).toEqual({ years: 5, months: 6 });
    });

    it('contrat pile à la frontière 2014-01-01 → null (statut unique pur)', () => {
      expect(R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateEntree: '2014-01-01' },
      })).toBeNull();
    });

    it('contrat post-2014 → null (Claeys non applicable)', () => {
      expect(R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateEntree: '2018-06-01' },
      })).toBeNull();
    });

    it('dateEntree absente → null', () => {
      expect(R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateLicenciement: '2026-03-15' },
      })).toBeNull();
    });

    it('dateEntree ISO invalide → null', () => {
      expect(R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateEntree: '15/06/2008' },
      })).toBeNull();
    });

    it('jour résiduel négatif → décrémente le mois', () => {
      // 2008-01-20 → 2014-01-01 = 5 ans 11 mois (jour résiduel négatif)
      const a = R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateEntree: '2008-01-20' },
      });
      expect(a).toEqual({ years: 5, months: 11 });
    });

    it('contrat très ancien (> 80 ans pré-2014) → plafonné à 80', () => {
      // 1900-01-01 → 2014-01-01 = 114 ans → plafond 80
      const a = R.computeAnciennetePreStatutUnique({
        ...base,
        aiData: { dateEntree: '1900-01-01' },
      });
      expect(a!.years).toBe(80);
    });
  });

  describe('computeAncienneteAnneesPostStatutUnique (de 2014-01-01 à dateLicenciement)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('contrat pré-2014 + dateLicenciement post-2014 → ancienneté post calculée', () => {
      // 2014-01-01 → 2026-03-15 = 12 ans 2 mois
      const v = R.computeAncienneteAnneesPostStatutUnique({
        ...base,
        aiData: { dateEntree: '2008-06-15', dateLicenciement: '2026-03-15' },
      });
      expect(v).toBe(12);
    });

    it('contrat pré-2014 sans dateLicenciement → fallback today (calculable)', () => {
      const v = R.computeAncienneteAnneesPostStatutUnique({
        ...base,
        aiData: { dateEntree: '2010-01-01' },
      });
      expect(v).not.toBeNull();
      expect(v!).toBeGreaterThanOrEqual(11);
    });

    it('contrat post-2014 → null (pas de fraction Claeys → pas non plus de fraction post à pré-fill)', () => {
      expect(R.computeAncienneteAnneesPostStatutUnique({
        ...base,
        aiData: { dateEntree: '2018-06-01', dateLicenciement: '2026-03-15' },
      })).toBeNull();
    });

    it('dateEntree absente → null', () => {
      expect(R.computeAncienneteAnneesPostStatutUnique({
        ...base,
        aiData: { dateLicenciement: '2026-03-15' },
      })).toBeNull();
    });
  });

  describe('computeRemunerationAnnuelleBruteEnMilliers (K€)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('priorité salaireBrutAnnuel → annuel / 1000', () => {
      const v = R.computeRemunerationAnnuelleBruteEnMilliers({
        ...base,
        aiData: { salaireBrutAnnuel: 60000 },
      });
      expect(v).toBe(60);
    });

    it('fallback salaireBrutMensuel → mensuel × 12 / 1000', () => {
      const v = R.computeRemunerationAnnuelleBruteEnMilliers({
        ...base,
        aiData: { salaireBrutMensuel: 5000 },
      });
      // 5000 × 12 / 1000 = 60
      expect(v).toBe(60);
    });

    it('annuel ET mensuel → priorité annuel', () => {
      const v = R.computeRemunerationAnnuelleBruteEnMilliers({
        ...base,
        aiData: { salaireBrutAnnuel: 52000, salaireBrutMensuel: 9999 },
      });
      expect(v).toBe(52);
    });

    it('valeurs ≤ 0 → null', () => {
      expect(R.computeRemunerationAnnuelleBruteEnMilliers({
        ...base, aiData: { salaireBrutAnnuel: 0 },
      })).toBeNull();
      expect(R.computeRemunerationAnnuelleBruteEnMilliers({
        ...base, aiData: { salaireBrutMensuel: -100 },
      })).toBeNull();
    });

    it('arrondi 2 décimales', () => {
      // 12345 / 1000 = 12.345 → 12.35
      const v = R.computeRemunerationAnnuelleBruteEnMilliers({
        ...base, aiData: { salaireBrutAnnuel: 12345 },
      });
      expect(v).toBe(12.35);
    });
  });

  describe('computeSalaireHebdomadaireBrut', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('priorité salaireBrutAnnuel → annuel / 52', () => {
      const v = R.computeSalaireHebdomadaireBrut({
        ...base, aiData: { salaireBrutAnnuel: 52000 },
      });
      expect(v).toBe(1000);
    });

    it('fallback salaireBrutMensuel → mensuel × 12 / 52', () => {
      const v = R.computeSalaireHebdomadaireBrut({
        ...base, aiData: { salaireBrutMensuel: 3000 },
      });
      expect(v).toBeCloseTo(692.31, 2);
    });

    it('non-number → null', () => {
      expect(R.computeSalaireHebdomadaireBrut({
        ...base,
        aiData: { salaireBrutAnnuel: '52000' as unknown as number },
      })).toBeNull();
    });
  });

  describe('computeAppliquerClauseSauvegarde (dérivé dateEntree)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('dateEntree pré-2014 → true', () => {
      expect(R.computeAppliquerClauseSauvegarde({
        ...base, aiData: { dateEntree: '2010-06-01' },
      })).toBe(true);
    });

    it('dateEntree pile 2014-01-01 → null (pas de Claeys → pas de pré-fill)', () => {
      expect(R.computeAppliquerClauseSauvegarde({
        ...base, aiData: { dateEntree: '2014-01-01' },
      })).toBeNull();
    });

    it('dateEntree post-2014 → null', () => {
      expect(R.computeAppliquerClauseSauvegarde({
        ...base, aiData: { dateEntree: '2018-06-01' },
      })).toBeNull();
    });

    it('dateEntree absente / invalide → null', () => {
      expect(R.computeAppliquerClauseSauvegarde({ ...base, aiData: {} })).toBeNull();
      expect(R.computeAppliquerClauseSauvegarde({
        ...base, aiData: { dateEntree: 'pas-une-date' },
      })).toBeNull();
    });
  });

  describe('computePrefillCount (intégration — max 4)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('0 champ → 0', () => {
      expect(R.computePrefillCount({ ...base, aiData: {} })).toBe(0);
    });

    it('contrat pré-2014 sans salaire → 1 (ancienneté pré + post fallback today)', () => {
      // dateEntree pré-2014 → ancienneté pré OK + ancienneté post fallback today.
      // Pas de salaire → ni rémunération annuelle ni salaire hebdo.
      const input = { ...base, aiData: { dateEntree: '2010-01-01' } };
      expect(R.computePrefillCount(input)).toBe(2);
    });

    it('contrat pré-2014 + salaire annuel + dateLicenciement → max 4', () => {
      const input = {
        ...base,
        aiData: {
          dateEntree: '2010-01-01',
          dateLicenciement: '2026-03-15',
          salaireBrutAnnuel: 60000,
        },
      };
      expect(R.computePrefillCount(input)).toBe(4);
    });

    it('contrat post-2014 → 1 (rémunération seule via salaire annuel — pas d\'ancienneté Claeys ni hebdo bloqué non, hebdo OK)', () => {
      // dateEntree post-2014 → pas d'ancienneté pré, pas d'ancienneté post calculable.
      // Salaire annuel OK → rémunération K€ + salaire hebdo = 2.
      const input = {
        ...base,
        aiData: { dateEntree: '2018-06-01', salaireBrutAnnuel: 60000 },
      };
      expect(R.computePrefillCount(input)).toBe(2);
    });

    it('appliquerClauseSauvegarde dérivé n\'est PAS compté', () => {
      // Seul dateEntree pré-2014 → ancienneté pré + ancienneté post fallback today = 2.
      // Pas +1 pour le flag clauseSauvegarde.
      const input = { ...base, aiData: { dateEntree: '2010-01-01' } };
      expect(R.computePrefillCount(input)).toBe(2);
      expect(R.computeAppliquerClauseSauvegarde(input)).toBe(true);
    });

    it('couple années+mois pré-2014 compté 1 seule fois', () => {
      const input = { ...base, aiData: { dateEntree: '2008-06-15' } };
      expect(R.computeAncienneteAnneesPreStatutUnique(input)).toBe(5);
      expect(R.computeAncienneteMoisPreStatutUnique(input)).toBe(6);
      // = 1 (ancienneté pré couplée) + 1 (ancienneté post fallback today) = 2
      expect(R.computePrefillCount(input)).toBe(2);
    });
  });
});
