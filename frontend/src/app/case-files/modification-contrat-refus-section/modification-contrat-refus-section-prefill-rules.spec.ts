/**
 * SF-212-12 — Tests unitaires du helper partagé
 * `modification-contrat-refus-section-prefill-rules`. Pattern aligné sur
 * SF-212-10 (faute-inexcusable-fr-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ModificationContratRefusSectionPrefillRules } from './modification-contrat-refus-section-prefill-rules';

describe('ModificationContratRefusSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    modifContratElementModifie: 'REMUNERATION',
    modifContratContractualise: true,
    modifContratMotifEco: true,
    modifContratNotifEcrite: false,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(ModificationContratRefusSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(ModificationContratRefusSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(ModificationContratRefusSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs elementModifie hors enum', () => {
      expect(ModificationContratRefusSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, modifContratElementModifie: 'FANTAISIE' },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, modifContratContractualise: 'oui' as unknown as boolean };
      expect(ModificationContratRefusSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeElementModifie', () => {
    it('normalise les valeurs minuscules en majuscules admises', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeElementModifie({
        aiData: { modifContratElementModifie: 'remuneration' },
        workspaceCountry: 'FRANCE',
      })).toBe('REMUNERATION');
    });
    it('accepte chacune des 7 valeurs admises', () => {
      const all = ['REMUNERATION', 'QUALIFICATION', 'DUREE_TRAVAIL', 'LIEU_TRAVAIL', 'HORAIRES', 'TACHES', 'AUTRE'];
      for (const v of all) {
        expect(ModificationContratRefusSectionPrefillRules.computeElementModifie({
          aiData: { modifContratElementModifie: v },
          workspaceCountry: 'FRANCE',
        })).toBe(v as ReturnType<typeof ModificationContratRefusSectionPrefillRules.computeElementModifie>);
      }
    });
    it('retourne null si absent', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeElementModifie({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeElementModifie({
        aiData: { modifContratElementModifie: 'REMUNERATION' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeContractualise', () => {
    it('retourne true si présent et booléen', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeContractualise({
        aiData: { modifContratContractualise: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et booléen false', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeContractualise({
        aiData: { modifContratContractualise: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null si absent', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeContractualise({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeContractualise({
        aiData: { modifContratContractualise: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeMotifEco', () => {
    it('retourne true si présent et booléen', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeMotifEco({
        aiData: { modifContratMotifEco: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeMotifEco({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeNotifEcrite', () => {
    it('retourne false si présent et booléen false', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeNotifEcrite({
        aiData: { modifContratNotifEcrite: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null hors France', () => {
      expect(ModificationContratRefusSectionPrefillRules.computeNotifEcrite({
        aiData: { modifContratNotifEcrite: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
