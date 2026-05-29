import { CarteResidentPrefillRules } from './carte-resident-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-24 — Tests du helper de pré-fill carte de résident (F-IM-36, FR).
 */
describe('CarteResidentPrefillRules', () => {
  const fr = (aiData: any): PrefillCountInput => ({ aiData, workspaceCountry: 'FRANCE' });

  describe('computeDureeSejourRegulierAnnees', () => {
    it('convertit aesDureePresenceMois en années entières (÷12)', () => {
      expect(CarteResidentPrefillRules.computeDureeSejourRegulierAnnees(fr({ aesDureePresenceMois: 66 }))).toBe(5);
    });

    it('arrondit vers le bas (61 mois → 5 ans)', () => {
      expect(CarteResidentPrefillRules.computeDureeSejourRegulierAnnees(fr({ aesDureePresenceMois: 61 }))).toBe(5);
    });

    it('retourne null si absent', () => {
      expect(CarteResidentPrefillRules.computeDureeSejourRegulierAnnees(fr({}))).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = { aiData: { aesDureePresenceMois: 66 }, workspaceCountry: 'BELGIQUE' };
      expect(CarteResidentPrefillRules.computeDureeSejourRegulierAnnees(input)).toBeNull();
    });
  });

  describe('computeRessourcesMensuellesNettes', () => {
    it('retourne le montant depuis carteResidentRessources', () => {
      expect(CarteResidentPrefillRules.computeRessourcesMensuellesNettes(fr({ carteResidentRessources: 1850 }))).toBe(1850);
    });

    it('retourne null si négatif', () => {
      expect(CarteResidentPrefillRules.computeRessourcesMensuellesNettes(fr({ carteResidentRessources: -10 }))).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = { aiData: { carteResidentRessources: 1850 }, workspaceCountry: 'BELGIQUE' };
      expect(CarteResidentPrefillRules.computeRessourcesMensuellesNettes(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('compte 2 quand durée + ressources présents (nominal)', () => {
      const input = fr({ aesDureePresenceMois: 66, carteResidentRessources: 1850 });
      expect(CarteResidentPrefillRules.computePrefillCount(input)).toBe(2);
    });

    it('compte 1 quand seule la durée est présente', () => {
      expect(CarteResidentPrefillRules.computePrefillCount(fr({ aesDureePresenceMois: 66 }))).toBe(1);
    });

    it('compte 0 quand aiData absent', () => {
      expect(CarteResidentPrefillRules.computePrefillCount(fr(null))).toBe(0);
    });

    it('compte 0 hors France', () => {
      const input: PrefillCountInput = {
        aiData: { aesDureePresenceMois: 66, carteResidentRessources: 1850 },
        workspaceCountry: 'BELGIQUE',
      };
      expect(CarteResidentPrefillRules.computePrefillCount(input)).toBe(0);
    });
  });
});
