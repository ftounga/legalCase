/**
 * SF-222-03 — Tests Jest pour `habilitation-familiale-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 6 critères IA ;
 *  - validation enum whitelist (lien / étendue) ;
 *  - gestion des cas absents / non-booléens ;
 *  - parité computePrefillCount = somme des mappings non-null (max 6).
 */

import {
  HabilitationFamilialePrefillRules,
  computeActesPatrimoniaux,
  computeActesPersonnels,
  computeAlteration,
  computeConsensus,
  computeEtendue,
  computeLienFamilial,
  computePrefillCount,
} from './habilitation-familiale-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  hfAlteration: true,
  hfLienFamilial: 'DESCENDANT',
  hfConsensus: true,
  hfActesPatrimoniaux: true,
  hfActesPersonnels: true,
  hfEtendue: 'GENERALE',
};

describe('habilitation-familiale-prefill-rules (SF-222-03)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeAlteration({ aiData: FULL_AI })).toBeNull();
      expect(computeLienFamilial({ aiData: FULL_AI })).toBeNull();
      expect(computeConsensus({ aiData: FULL_AI })).toBeNull();
      expect(computeActesPatrimoniaux({ aiData: FULL_AI })).toBeNull();
      expect(computeActesPersonnels({ aiData: FULL_AI })).toBeNull();
      expect(computeEtendue({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeAlteration(input)).toBeNull();
      expect(computeEtendue(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeAlteration(input)).toBe(true);
      expect(computeLienFamilial(input)).toBe('DESCENDANT');
      expect(computeConsensus(input)).toBe(true);
      expect(computeActesPatrimoniaux(input)).toBe(true);
      expect(computeActesPersonnels(input)).toBe(true);
      expect(computeEtendue(input)).toBe('GENERALE');
    });
  });

  describe('validation enum', () => {
    it('lien hors whitelist → null', () => {
      const input = {
        aiData: { hfLienFamilial: 'COUSIN' } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeLienFamilial(input)).toBeNull();
    });

    it('lien AUTRE valide → AUTRE', () => {
      const input = {
        aiData: { hfLienFamilial: 'AUTRE' } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeLienFamilial(input)).toBe('AUTRE');
    });

    it('étendue hors whitelist → null', () => {
      const input = {
        aiData: { hfEtendue: 'PARTIELLE' } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeEtendue(input)).toBeNull();
    });
  });

  describe('null / absent / invalides', () => {
    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeAlteration(input)).toBeNull();
      expect(computeLienFamilial(input)).toBeNull();
    });

    it('aiData {} → tous null', () => {
      const input = { aiData: {}, workspaceCountry: 'FRANCE' };
      expect(computeConsensus(input)).toBeNull();
      expect(computeEtendue(input)).toBeNull();
    });

    it('critère false → false (valide)', () => {
      const input = {
        aiData: { hfConsensus: false } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeConsensus(input)).toBe(false);
    });
  });

  describe('computePrefillCount', () => {
    it('FULL_AI + FRANCE → 6', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(6);
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

    it('partial AI (alteration seule) → 1', () => {
      const partial = { hfAlteration: true } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(1);
    });

    it('partial AI (alteration + lien valide + étendue invalide) → 2', () => {
      const partial = {
        hfAlteration: true,
        hfLienFamilial: 'ASCENDANT',
        hfEtendue: 'ZZZ',
      } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(2);
    });
  });

  describe('export HabilitationFamilialePrefillRules object', () => {
    it('expose computePrefillCount + computeXxx', () => {
      expect(typeof HabilitationFamilialePrefillRules.computePrefillCount).toBe('function');
      expect(typeof HabilitationFamilialePrefillRules.computeAlteration).toBe('function');
      expect(typeof HabilitationFamilialePrefillRules.computeEtendue).toBe('function');
    });
  });
});
