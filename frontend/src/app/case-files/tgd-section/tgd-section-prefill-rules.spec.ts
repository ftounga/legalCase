/**
 * SF-222-02 — Tests Jest pour `tgd-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 5 critères IA ;
 *  - gestion des cas absents / non-booléens ;
 *  - parité computePrefillCount = somme des mappings non-null (max 5).
 */

import {
  TgdPrefillRules,
  computeConsentement,
  computeDangerGrave,
  computeInterdictionContact,
  computeNonCohabitation,
  computePrefillCount,
  computeViolences,
} from './tgd-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  tgdDangerGrave: true,
  tgdViolences: true,
  tgdInterdictionContact: true,
  tgdNonCohabitation: true,
  tgdConsentement: true,
};

describe('tgd-prefill-rules (SF-222-02)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeDangerGrave({ aiData: FULL_AI })).toBeNull();
      expect(computeViolences({ aiData: FULL_AI })).toBeNull();
      expect(computeInterdictionContact({ aiData: FULL_AI })).toBeNull();
      expect(computeNonCohabitation({ aiData: FULL_AI })).toBeNull();
      expect(computeConsentement({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeDangerGrave(input)).toBeNull();
      expect(computeConsentement(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeDangerGrave(input)).toBe(true);
      expect(computeViolences(input)).toBe(true);
      expect(computeInterdictionContact(input)).toBe(true);
      expect(computeNonCohabitation(input)).toBe(true);
      expect(computeConsentement(input)).toBe(true);
    });
  });

  describe('null / absent / invalides', () => {
    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeDangerGrave(input)).toBeNull();
      expect(computeConsentement(input)).toBeNull();
    });

    it('aiData {} → tous null', () => {
      const input = { aiData: {}, workspaceCountry: 'FRANCE' };
      expect(computeViolences(input)).toBeNull();
      expect(computeInterdictionContact(input)).toBeNull();
    });

    it('critère false → false (valide)', () => {
      const input = {
        aiData: { tgdInterdictionContact: false } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeInterdictionContact(input)).toBe(false);
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

    it('partial AI (danger grave seul) → 1', () => {
      const partial = { tgdDangerGrave: true } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(1);
    });

    it('partial AI (danger + violences) → 2', () => {
      const partial = {
        tgdDangerGrave: true,
        tgdViolences: false,
      } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(2);
    });
  });

  describe('export TgdPrefillRules object', () => {
    it('expose computePrefillCount + computeXxx', () => {
      expect(typeof TgdPrefillRules.computePrefillCount).toBe('function');
      expect(typeof TgdPrefillRules.computeDangerGrave).toBe('function');
      expect(typeof TgdPrefillRules.computeConsentement).toBe('function');
    });
  });
});
