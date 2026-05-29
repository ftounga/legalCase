import { OqtfCategoriesPrefillRules } from './oqtf-categories-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-10 — Tests du helper de pré-fill OQTF catégories L.611-1 (F-IM-29, FR).
 */
describe('OqtfCategoriesPrefillRules', () => {
  const fr = (aiData: any): PrefillCountInput => ({ aiData, workspaceCountry: 'FRANCE' });

  describe('computeDateNotificationOqtf', () => {
    it('retourne la date ISO non future', () => {
      const input = fr({ dateNotificationOqtf: '2025-01-15' });
      expect(OqtfCategoriesPrefillRules.computeDateNotificationOqtf(input)).toBe('2025-01-15');
    });

    it('rejette une date future', () => {
      const future = new Date(Date.now() + 86400000 * 30).toISOString().slice(0, 10);
      const input = fr({ dateNotificationOqtf: future });
      expect(OqtfCategoriesPrefillRules.computeDateNotificationOqtf(input)).toBeNull();
    });

    it('rejette un format non ISO', () => {
      const input = fr({ dateNotificationOqtf: '15/01/2025' });
      expect(OqtfCategoriesPrefillRules.computeDateNotificationOqtf(input)).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = {
        aiData: { dateNotificationOqtf: '2025-01-15' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(OqtfCategoriesPrefillRules.computeDateNotificationOqtf(input)).toBeNull();
    });
  });

  describe('computeMotifOqtf', () => {
    it('mappe le code enum vers le libellé lisible', () => {
      const input = fr({ motifOqtfCode: 'REFUS_TITRE' });
      expect(OqtfCategoriesPrefillRules.computeMotifOqtf(input)).toBe('Refus de titre de séjour');
    });

    it('retourne null pour un code inconnu', () => {
      const input = fr({ motifOqtfCode: 'XXX' });
      expect(OqtfCategoriesPrefillRules.computeMotifOqtf(input)).toBeNull();
    });

    it('retourne null si pas de code', () => {
      const input = fr({});
      expect(OqtfCategoriesPrefillRules.computeMotifOqtf(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('compte 2 quand date + motif présents', () => {
      const input = fr({ dateNotificationOqtf: '2025-01-15', motifOqtfCode: 'SEJOUR_IRREGULIER' });
      expect(OqtfCategoriesPrefillRules.computePrefillCount(input)).toBe(2);
    });

    it('compte 1 quand seule la date est présente', () => {
      const input = fr({ dateNotificationOqtf: '2025-01-15' });
      expect(OqtfCategoriesPrefillRules.computePrefillCount(input)).toBe(1);
    });

    it('compte 0 quand aiData absent', () => {
      const input = fr(null);
      expect(OqtfCategoriesPrefillRules.computePrefillCount(input)).toBe(0);
    });

    it('compte 0 hors France', () => {
      const input: PrefillCountInput = {
        aiData: { dateNotificationOqtf: '2025-01-15', motifOqtfCode: 'REFUS_TITRE' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(OqtfCategoriesPrefillRules.computePrefillCount(input)).toBe(0);
    });
  });
});
