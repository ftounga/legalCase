import { AnefProcedurePrefillRules } from './anef-procedure-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-26 — Tests du helper de pré-fill ANEF procédure (F-IM-37, FR).
 */
describe('AnefProcedurePrefillRules', () => {
  const fr = (aiData: any): PrefillCountInput => ({ aiData, workspaceCountry: 'FRANCE' });

  describe('computeDateExpirationTitre', () => {
    it('retourne la date ISO depuis dateExpirationTitre', () => {
      const input = fr({ dateExpirationTitre: '2026-03-10' });
      expect(AnefProcedurePrefillRules.computeDateExpirationTitre(input)).toBe('2026-03-10');
    });

    it('rejette un format non ISO', () => {
      const input = fr({ dateExpirationTitre: '10/03/2026' });
      expect(AnefProcedurePrefillRules.computeDateExpirationTitre(input)).toBeNull();
    });

    it('retourne null si absent', () => {
      expect(AnefProcedurePrefillRules.computeDateExpirationTitre(fr({}))).toBeNull();
    });

    it('retourne null hors France', () => {
      const input: PrefillCountInput = {
        aiData: { dateExpirationTitre: '2026-03-10' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(AnefProcedurePrefillRules.computeDateExpirationTitre(input)).toBeNull();
    });
  });

  describe('computeTypeTitreConcerne', () => {
    it('retourne le type depuis typeTitreSejour (trim)', () => {
      const input = fr({ typeTitreSejour: '  VPF  ' });
      expect(AnefProcedurePrefillRules.computeTypeTitreConcerne(input)).toBe('VPF');
    });

    it('retourne null si chaîne vide', () => {
      expect(AnefProcedurePrefillRules.computeTypeTitreConcerne(fr({ typeTitreSejour: '   ' }))).toBeNull();
    });

    it('retourne null si absent', () => {
      expect(AnefProcedurePrefillRules.computeTypeTitreConcerne(fr({}))).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('compte 2 quand date + type présents (nominal)', () => {
      const input = fr({ dateExpirationTitre: '2026-03-10', typeTitreSejour: 'VPF' });
      expect(AnefProcedurePrefillRules.computePrefillCount(input)).toBe(2);
    });

    it('compte 1 quand seule la date est présente', () => {
      const input = fr({ dateExpirationTitre: '2026-03-10' });
      expect(AnefProcedurePrefillRules.computePrefillCount(input)).toBe(1);
    });

    it('compte 0 quand aiData absent', () => {
      expect(AnefProcedurePrefillRules.computePrefillCount(fr(null))).toBe(0);
    });

    it('compte 0 hors France', () => {
      const input: PrefillCountInput = {
        aiData: { dateExpirationTitre: '2026-03-10', typeTitreSejour: 'VPF' },
        workspaceCountry: 'BELGIQUE',
      };
      expect(AnefProcedurePrefillRules.computePrefillCount(input)).toBe(0);
    });
  });
});
