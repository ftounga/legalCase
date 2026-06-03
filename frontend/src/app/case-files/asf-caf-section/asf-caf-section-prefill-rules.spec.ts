/**
 * SF-222-01 — Tests Jest pour `asf-caf-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 5 champs IA ;
 *  - gestion des cas absents / négatifs / NaN / nb enfants < 1 / enum hors whitelist ;
 *  - parité computePrefillCount = somme des mappings non-null (max 5).
 */

import {
  AsfCafPrefillRules,
  computeMontantPensionMensuel,
  computeNbEnfantsACharge,
  computeParentIsole,
  computePensionFixee,
  computePensionPayee,
  computePrefillCount,
} from './asf-caf-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  asfParentIsole: true,
  asfPensionFixee: true,
  asfMontantPension: 300,
  asfPensionPayee: 'NON_PAYEE',
  asfNbEnfants: 2,
};

describe('asf-caf-prefill-rules (SF-222-01)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeParentIsole({ aiData: FULL_AI })).toBeNull();
      expect(computePensionFixee({ aiData: FULL_AI })).toBeNull();
      expect(computeMontantPensionMensuel({ aiData: FULL_AI })).toBeNull();
      expect(computePensionPayee({ aiData: FULL_AI })).toBeNull();
      expect(computeNbEnfantsACharge({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeParentIsole(input)).toBeNull();
      expect(computeNbEnfantsACharge(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeParentIsole(input)).toBe(true);
      expect(computePensionFixee(input)).toBe(true);
      expect(computeMontantPensionMensuel(input)).toBe(300);
      expect(computePensionPayee(input)).toBe('NON_PAYEE');
      expect(computeNbEnfantsACharge(input)).toBe(2);
    });
  });

  describe('null / absent / invalides', () => {
    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeParentIsole(input)).toBeNull();
      expect(computeNbEnfantsACharge(input)).toBeNull();
    });

    it('aiData {} → tous null', () => {
      const input = { aiData: {}, workspaceCountry: 'FRANCE' };
      expect(computeMontantPensionMensuel(input)).toBeNull();
      expect(computePensionPayee(input)).toBeNull();
    });

    it('montant négatif → null', () => {
      const input = {
        aiData: { asfMontantPension: -10 } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeMontantPensionMensuel(input)).toBeNull();
    });

    it('montant NaN → null', () => {
      const input = {
        aiData: { asfMontantPension: Number.NaN } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeMontantPensionMensuel(input)).toBeNull();
    });

    it('parentIsole = false → false (valide)', () => {
      const input = {
        aiData: { asfParentIsole: false } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeParentIsole(input)).toBe(false);
    });

    it('nb enfants = 0 → null (doit être ≥ 1)', () => {
      const input = {
        aiData: { asfNbEnfants: 0 } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeNbEnfantsACharge(input)).toBeNull();
    });

    it('pensionPayee hors whitelist → null', () => {
      const input = {
        aiData: { asfPensionPayee: 'BIDON' } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computePensionPayee(input)).toBeNull();
    });

    it('pensionPayee minuscule normalisée → valide', () => {
      const input = {
        aiData: { asfPensionPayee: 'partielle' } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computePensionPayee(input)).toBe('PARTIELLE');
    });
  });

  describe('computePrefillCount', () => {
    it('FULL_AI + FRANCE → 5', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(5);
    });

    it('FULL_AI + BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('empty {} → 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('partial AI (nb enfants seul) → 1', () => {
      const partial = { asfNbEnfants: 1 } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(1);
    });

    it('partial AI (parent isolé + pension fixée) → 2', () => {
      const partial = {
        asfParentIsole: true,
        asfPensionFixee: true,
      } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(2);
    });
  });

  describe('export AsfCafPrefillRules object', () => {
    it('expose computePrefillCount + computeXxx', () => {
      expect(typeof AsfCafPrefillRules.computePrefillCount).toBe('function');
      expect(typeof AsfCafPrefillRules.computeParentIsole).toBe('function');
      expect(typeof AsfCafPrefillRules.computePensionPayee).toBe('function');
      expect(typeof AsfCafPrefillRules.computeNbEnfantsACharge).toBe('function');
    });
  });
});
