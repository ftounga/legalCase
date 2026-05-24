/**
 * SF-212-36 — Tests unitaires du helper partagé
 * `pdv-rcc-conformite-section-prefill-rules`. Pattern aligné sur SF-212-24
 * (egalite-salariale-fh-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PdvRccConformiteSectionPrefillRules } from './pdv-rcc-conformite-section-prefill-rules';

describe('PdvRccConformiteSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    pdvRccDetail: {
      pdvRccTypeDispositif: 'RCC',
      pdvRccAccordMajoritaire: true,
      pdvRccValidationDREETS: true,
      pdvRccIndemnitesLegales: true,
    },
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 si pdvRccDetail null', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: { pdvRccDetail: null },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore un type dispositif hors enum', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: { pdvRccDetail: { ...FULL_AI_DATA.pdvRccDetail!, pdvRccTypeDispositif: 'INCONNU' } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore un accord majoritaire de type non-boolean', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: { pdvRccDetail: { ...FULL_AI_DATA.pdvRccDetail!, pdvRccAccordMajoritaire: 'true' as unknown as boolean } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('retourne 0 si aiData.pdvRccDetail undefined (cas dette F-DT-46 actuel)', () => {
      expect(PdvRccConformiteSectionPrefillRules.computePrefillCount({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });
  });

  describe('computeTypeDispositif', () => {
    it('retourne RCC si présent', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeTypeDispositif({
        aiData: { pdvRccDetail: { pdvRccTypeDispositif: 'RCC' } },
        workspaceCountry: 'FRANCE',
      })).toBe('RCC');
    });
    it('retourne PDV si présent', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeTypeDispositif({
        aiData: { pdvRccDetail: { pdvRccTypeDispositif: 'PDV' } },
        workspaceCountry: 'FRANCE',
      })).toBe('PDV');
    });
    it('null hors enum', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeTypeDispositif({
        aiData: { pdvRccDetail: { pdvRccTypeDispositif: 'X' } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null hors France', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeTypeDispositif({
        aiData: { pdvRccDetail: { pdvRccTypeDispositif: 'RCC' } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
    it('null si type non string', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeTypeDispositif({
        aiData: { pdvRccDetail: { pdvRccTypeDispositif: 123 as unknown as string } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeAccordMajoritaire', () => {
    it('retourne true si présent', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeAccordMajoritaire({
        aiData: { pdvRccDetail: { pdvRccAccordMajoritaire: true } },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeAccordMajoritaire({
        aiData: { pdvRccDetail: { pdvRccAccordMajoritaire: false } },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('null hors France', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeAccordMajoritaire({
        aiData: { pdvRccDetail: { pdvRccAccordMajoritaire: true } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeValidationDreets', () => {
    it('retourne true si présent', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeValidationDreets({
        aiData: { pdvRccDetail: { pdvRccValidationDREETS: true } },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('null si type non boolean', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeValidationDreets({
        aiData: { pdvRccDetail: { pdvRccValidationDREETS: 1 as unknown as boolean } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeIndemnitesLegales', () => {
    it('retourne false si présent', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeIndemnitesLegales({
        aiData: { pdvRccDetail: { pdvRccIndemnitesLegales: false } },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('null hors France', () => {
      expect(PdvRccConformiteSectionPrefillRules.computeIndemnitesLegales({
        aiData: { pdvRccDetail: { pdvRccIndemnitesLegales: true } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
