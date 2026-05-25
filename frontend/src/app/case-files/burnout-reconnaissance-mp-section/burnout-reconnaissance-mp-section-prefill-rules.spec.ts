/**
 * SF-212-28 — Tests unitaires du helper partagé
 * `burnout-reconnaissance-mp-section-prefill-rules`. Pattern aligné sur
 * SF-212-26 (lanceur-alerte-protection-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { BurnoutReconnaissanceMpSectionPrefillRules } from './burnout-reconnaissance-mp-section-prefill-rules';

describe('BurnoutReconnaissanceMpSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    burnoutDiagnostic: true,
    burnoutTauxIpp: 30,
    burnoutSurchargeDocumentee: true,
    burnoutArretsMaladie: true,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, burnoutDiagnostic: 'oui' as unknown as boolean };
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore taux IPP hors plage [0, 100]', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, burnoutTauxIpp: 150 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, burnoutTauxIpp: -5 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeDiagnostic', () => {
    it('retourne true si présent et boolean true', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeDiagnostic({
        aiData: { burnoutDiagnostic: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et boolean false', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeDiagnostic({
        aiData: { burnoutDiagnostic: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null hors France', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeDiagnostic({
        aiData: { burnoutDiagnostic: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeDiagnostic({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeTauxIpp', () => {
    it('accepte taux 0 et 100', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeTauxIpp({
        aiData: { burnoutTauxIpp: 0 },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeTauxIpp({
        aiData: { burnoutTauxIpp: 100 },
        workspaceCountry: 'FRANCE',
      })).toBe(100);
    });
    it('rejette taux > 100', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeTauxIpp({
        aiData: { burnoutTauxIpp: 101 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('rejette taux négatif', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeTauxIpp({
        aiData: { burnoutTauxIpp: -1 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeTauxIpp({
        aiData: { burnoutTauxIpp: 30 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeSurchargeDocumentee', () => {
    it('retourne le boolean tel quel', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeSurchargeDocumentee({
        aiData: { burnoutSurchargeDocumentee: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeSurchargeDocumentee({
        aiData: { burnoutSurchargeDocumentee: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
  });

  describe('computeArretsMaladie', () => {
    it('retourne le boolean tel quel', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeArretsMaladie({
        aiData: { burnoutArretsMaladie: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null hors France', () => {
      expect(BurnoutReconnaissanceMpSectionPrefillRules.computeArretsMaladie({
        aiData: { burnoutArretsMaladie: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
