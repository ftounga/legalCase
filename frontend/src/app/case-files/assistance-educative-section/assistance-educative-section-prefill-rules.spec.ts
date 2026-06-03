/**
 * SF-222-04 — Tests Jest pour `assistance-educative-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 5 critères IA ;
 *  - gestion des cas absents / non-booléens ;
 *  - parité computePrefillCount = somme des mappings non-null (max 5).
 */

import {
  AssistanceEducativePrefillRules,
  computeAdhesion,
  computeDanger,
  computeMaintien,
  computeMesureAmiable,
  computePrefillCount,
  computeUrgence,
} from './assistance-educative-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  aeDangerCaracterise: true,
  aeUrgence: false,
  aeAdhesionFamille: true,
  aeMaintienMilieu: true,
  aeMesureAmiable: true,
};

describe('assistance-educative-prefill-rules (SF-222-04)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeDanger({ aiData: FULL_AI })).toBeNull();
      expect(computeUrgence({ aiData: FULL_AI })).toBeNull();
      expect(computeAdhesion({ aiData: FULL_AI })).toBeNull();
      expect(computeMaintien({ aiData: FULL_AI })).toBeNull();
      expect(computeMesureAmiable({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeDanger(input)).toBeNull();
      expect(computeMaintien(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeDanger(input)).toBe(true);
      expect(computeUrgence(input)).toBe(false);
      expect(computeAdhesion(input)).toBe(true);
      expect(computeMaintien(input)).toBe(true);
      expect(computeMesureAmiable(input)).toBe(true);
    });
  });

  describe('null / absent / invalides', () => {
    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeDanger(input)).toBeNull();
      expect(computeUrgence(input)).toBeNull();
    });

    it('aiData {} → tous null', () => {
      const input = { aiData: {}, workspaceCountry: 'FRANCE' };
      expect(computeAdhesion(input)).toBeNull();
      expect(computeMesureAmiable(input)).toBeNull();
    });

    it('critère false → false (valide)', () => {
      const input = {
        aiData: { aeUrgence: false } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeUrgence(input)).toBe(false);
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

    it('partial AI (danger seul) → 1', () => {
      const partial = { aeDangerCaracterise: true } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(1);
    });

    it('partial AI (danger + urgence) → 2', () => {
      const partial = {
        aeDangerCaracterise: true,
        aeUrgence: true,
      } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(2);
    });
  });

  describe('export AssistanceEducativePrefillRules object', () => {
    it('expose computePrefillCount + computeXxx', () => {
      expect(typeof AssistanceEducativePrefillRules.computePrefillCount).toBe('function');
      expect(typeof AssistanceEducativePrefillRules.computeDanger).toBe('function');
      expect(typeof AssistanceEducativePrefillRules.computeMesureAmiable).toBe('function');
    });
  });
});
