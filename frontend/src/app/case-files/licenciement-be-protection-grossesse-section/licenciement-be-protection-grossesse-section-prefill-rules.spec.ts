import {
  LicenciementBeProtectionGrossesseSectionPrefillRules as R,
} from './licenciement-be-protection-grossesse-section-prefill-rules';

/**
 * SF-213-05b — tests unitaires du helper pur.
 * Garantit la parité runtime/static (`prefill-count-integrity.spec.ts`).
 */
describe('LicenciementBeProtectionGrossesseSectionPrefillRules', () => {

  describe('Gate workspaceCountry', () => {
    it('FRANCE → tous les compute* retournent null + count 0', () => {
      const input = {
        workspaceCountry: 'FRANCE',
        aiData: {
          dateLicenciement: '2026-03-15',
          salaireBrutMensuel: 3500,
          salaireBrutAnnuel: 42000,
        },
      };
      expect(R.computeDateLicenciement(input)).toBeNull();
      expect(R.computeRemunerationMensuelleBrute(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('workspaceCountry absent → 0', () => {
      expect(R.computePrefillCount({ aiData: { dateLicenciement: '2026-03-15' } })).toBe(0);
    });
  });

  describe('aiData absent', () => {
    it('aiData null → tous null', () => {
      const input = { workspaceCountry: 'BELGIQUE', aiData: null };
      expect(R.computeDateLicenciement(input)).toBeNull();
      expect(R.computeRemunerationMensuelleBrute(input)).toBeNull();
      expect(R.computePrefillCount(input)).toBe(0);
    });

    it('aiData undefined → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: undefined })).toBe(0);
    });

    it('aiData {} → 0', () => {
      expect(R.computePrefillCount({ workspaceCountry: 'BELGIQUE', aiData: {} })).toBe(0);
    });
  });

  describe('computeDateLicenciement', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('ISO valide → renvoyé tel quel', () => {
      expect(R.computeDateLicenciement({
        ...base,
        aiData: { dateLicenciement: '2026-03-15' },
      })).toBe('2026-03-15');
    });

    it('absent → null', () => {
      expect(R.computeDateLicenciement({
        ...base,
        aiData: { salaireBrutMensuel: 3500 },
      })).toBeNull();
    });

    it('ISO invalide → null', () => {
      expect(R.computeDateLicenciement({
        ...base,
        aiData: { dateLicenciement: '15/03/2026' },
      })).toBeNull();
    });

    it('date impossible (mois 13) → null', () => {
      expect(R.computeDateLicenciement({
        ...base,
        aiData: { dateLicenciement: '2026-13-01' },
      })).toBeNull();
    });

    it('non-string → null', () => {
      expect(R.computeDateLicenciement({
        ...base,
        aiData: { dateLicenciement: 20260315 as unknown as string },
      })).toBeNull();
    });
  });

  describe('computeRemunerationMensuelleBrute', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('priorité salaireBrutMensuel → renvoyé direct', () => {
      expect(R.computeRemunerationMensuelleBrute({
        ...base,
        aiData: { salaireBrutMensuel: 3500 },
      })).toBe(3500);
    });

    it('fallback salaireBrutAnnuel → annuel / 12', () => {
      // 42000 / 12 = 3500
      expect(R.computeRemunerationMensuelleBrute({
        ...base,
        aiData: { salaireBrutAnnuel: 42000 },
      })).toBe(3500);
    });

    it('mensuel ET annuel → priorité mensuel', () => {
      expect(R.computeRemunerationMensuelleBrute({
        ...base,
        aiData: { salaireBrutMensuel: 3500, salaireBrutAnnuel: 99999 },
      })).toBe(3500);
    });

    it('valeurs ≤ 0 → null', () => {
      expect(R.computeRemunerationMensuelleBrute({
        ...base, aiData: { salaireBrutMensuel: 0 },
      })).toBeNull();
      expect(R.computeRemunerationMensuelleBrute({
        ...base, aiData: { salaireBrutAnnuel: -1000 },
      })).toBeNull();
    });

    it('arrondi 2 décimales (fallback annuel)', () => {
      // 42345 / 12 = 3528.75 (déjà 2 dec)
      expect(R.computeRemunerationMensuelleBrute({
        ...base, aiData: { salaireBrutAnnuel: 42345 },
      })).toBe(3528.75);
      // 42333 / 12 = 3527.75 (arrondi)
      expect(R.computeRemunerationMensuelleBrute({
        ...base, aiData: { salaireBrutAnnuel: 42333 },
      })).toBe(3527.75);
    });

    it('non-number mensuel → null', () => {
      expect(R.computeRemunerationMensuelleBrute({
        ...base,
        aiData: { salaireBrutMensuel: '3500' as unknown as number },
      })).toBeNull();
    });
  });

  describe('computePrefillCount (intégration — max 2)', () => {
    const base = { workspaceCountry: 'BELGIQUE' };

    it('0 champ → 0', () => {
      expect(R.computePrefillCount({ ...base, aiData: {} })).toBe(0);
    });

    it('date seule → 1', () => {
      expect(R.computePrefillCount({
        ...base, aiData: { dateLicenciement: '2026-03-15' },
      })).toBe(1);
    });

    it('rémunération seule (mensuel) → 1', () => {
      expect(R.computePrefillCount({
        ...base, aiData: { salaireBrutMensuel: 3500 },
      })).toBe(1);
    });

    it('rémunération seule (annuel fallback) → 1', () => {
      expect(R.computePrefillCount({
        ...base, aiData: { salaireBrutAnnuel: 42000 },
      })).toBe(1);
    });

    it('date + rémunération mensuelle → max 2', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: { dateLicenciement: '2026-03-15', salaireBrutMensuel: 3500 },
      })).toBe(2);
    });

    it('date + rémunération annuelle (fallback) → max 2', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: { dateLicenciement: '2026-03-15', salaireBrutAnnuel: 42000 },
      })).toBe(2);
    });

    it('rémunération comptée 1 seule fois même si mensuel ET annuel', () => {
      expect(R.computePrefillCount({
        ...base,
        aiData: { salaireBrutMensuel: 3500, salaireBrutAnnuel: 42000 },
      })).toBe(1);
    });
  });
});
