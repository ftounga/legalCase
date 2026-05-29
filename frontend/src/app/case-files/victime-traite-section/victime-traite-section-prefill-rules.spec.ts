import { VictimeTraitePrefillRules } from './victime-traite-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-22 — Tests du helper de pré-fill victime de traite (F-IM-35, FR).
 */
describe('VictimeTraitePrefillRules', () => {
  const fr = (aiData: any): PrefillCountInput => ({ aiData, workspaceCountry: 'FRANCE' });

  describe('computePlainteDeposee', () => {
    it('retourne true depuis tehPlainteDeposee', () => {
      expect(VictimeTraitePrefillRules.computePlainteDeposee(fr({ tehPlainteDeposee: true }))).toBe(true);
    });

    it('retourne false depuis tehPlainteDeposee', () => {
      expect(VictimeTraitePrefillRules.computePlainteDeposee(fr({ tehPlainteDeposee: false }))).toBe(false);
    });

    it('retourne null si absent', () => {
      expect(VictimeTraitePrefillRules.computePlainteDeposee(fr({}))).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = { aiData: { tehPlainteDeposee: true }, workspaceCountry: 'BELGIQUE' };
      expect(VictimeTraitePrefillRules.computePlainteDeposee(input)).toBeNull();
    });
  });

  describe('computeDatePlainte', () => {
    it('retourne la date ISO depuis tehDatePlainte', () => {
      expect(VictimeTraitePrefillRules.computeDatePlainte(fr({ tehDatePlainte: '2026-01-10' }))).toBe('2026-01-10');
    });

    it('rejette un format non ISO', () => {
      expect(VictimeTraitePrefillRules.computeDatePlainte(fr({ tehDatePlainte: '10/01/2026' }))).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = { aiData: { tehDatePlainte: '2026-01-10' }, workspaceCountry: 'BELGIQUE' };
      expect(VictimeTraitePrefillRules.computeDatePlainte(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('compte 2 quand plainte + date présents (nominal)', () => {
      const input = fr({ tehPlainteDeposee: true, tehDatePlainte: '2026-01-10' });
      expect(VictimeTraitePrefillRules.computePrefillCount(input)).toBe(2);
    });

    it('compte 1 quand seul plainteDeposee présent (false compte aussi)', () => {
      const input = fr({ tehPlainteDeposee: false });
      expect(VictimeTraitePrefillRules.computePrefillCount(input)).toBe(1);
    });

    it('compte 0 quand aiData absent', () => {
      expect(VictimeTraitePrefillRules.computePrefillCount(fr(null))).toBe(0);
    });

    it('compte 0 hors France', () => {
      const input: PrefillCountInput = {
        aiData: { tehPlainteDeposee: true, tehDatePlainte: '2026-01-10' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(VictimeTraitePrefillRules.computePrefillCount(input)).toBe(0);
    });
  });
});
