import { OfpraIntroductionPrefillRules } from './ofpra-introduction-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-18 — Tests du helper de pré-fill OFPRA introduction (F-IM-33, FR).
 */
describe('OfpraIntroductionPrefillRules', () => {
  const fr = (aiData: any): PrefillCountInput => ({ aiData, workspaceCountry: 'FRANCE' });

  describe('computeDateArriveeEnFrance', () => {
    it('retourne la date ISO depuis aesDateEntreeFrance', () => {
      const input = fr({ aesDateEntreeFrance: '2026-01-10' });
      expect(OfpraIntroductionPrefillRules.computeDateArriveeEnFrance(input)).toBe('2026-01-10');
    });

    it('rejette un format non ISO', () => {
      const input = fr({ aesDateEntreeFrance: '10/01/2026' });
      expect(OfpraIntroductionPrefillRules.computeDateArriveeEnFrance(input)).toBeNull();
    });

    it('retourne null si absent', () => {
      const input = fr({});
      expect(OfpraIntroductionPrefillRules.computeDateArriveeEnFrance(input)).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = {
        aiData: { aesDateEntreeFrance: '2026-01-10' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(OfpraIntroductionPrefillRules.computeDateArriveeEnFrance(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('compte 1 quand la date d\'arrivée est présente (nominal)', () => {
      const input = fr({ aesDateEntreeFrance: '2026-01-10' });
      expect(OfpraIntroductionPrefillRules.computePrefillCount(input)).toBe(1);
    });

    it('compte 0 quand aiData absent', () => {
      const input = fr(null);
      expect(OfpraIntroductionPrefillRules.computePrefillCount(input)).toBe(0);
    });

    it('compte 0 hors France', () => {
      const input: PrefillCountInput = {
        aiData: { aesDateEntreeFrance: '2026-01-10' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(OfpraIntroductionPrefillRules.computePrefillCount(input)).toBe(0);
    });
  });
});
