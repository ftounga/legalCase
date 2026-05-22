/**
 * SF-216-12 — Tests Jest pour `retrait-ap-fr-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 4 champs IA (âge enfant, condamnation pénale,
 *    danger immédiat, violences conjugales) ;
 *  - gestion des cas absents / négatifs / NaN / non-fini ;
 *  - combinaison violences alléguées (string[] F-246) + violences LMVSS
 *    2022 (boolean SF-216-11) ;
 *  - parité computePrefillCount = somme des mappings non-null (max 4).
 */

import {
  RetraitApFrPrefillRules,
  computeAgeEnfant,
  computeCondamnationPenale,
  computeDangerCaracterise,
  computePrefillCount,
  computeViolencesConjugales,
} from './retrait-ap-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  agesEnfantsDetectes: [8, 12],
  condamnationPenaleDetectee: true,
  dangerImmediatDetected: true,
  violencesLmvss2022Detectees: true,
  violencesAllegueesDetectees: ['PHYSIQUES'],
};

describe('retrait-ap-fr-prefill-rules (SF-216-12)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeAgeEnfant({ aiData: FULL_AI })).toBeNull();
      expect(computeCondamnationPenale({ aiData: FULL_AI })).toBeNull();
      expect(computeDangerCaracterise({ aiData: FULL_AI })).toBeNull();
      expect(computeViolencesConjugales({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeAgeEnfant(input)).toBeNull();
      expect(computeCondamnationPenale(input)).toBeNull();
      expect(computeDangerCaracterise(input)).toBeNull();
      expect(computeViolencesConjugales(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping appliqué', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeAgeEnfant(input)).toBe(8);
      expect(computeCondamnationPenale(input)).toBe(true);
      expect(computeDangerCaracterise(input)).toBe(true);
      expect(computeViolencesConjugales(input)).toBe(true);
    });
  });

  describe('computeAgeEnfant', () => {
    it('aiData null → null', () => {
      expect(computeAgeEnfant({ aiData: null, workspaceCountry: 'FRANCE' })).toBeNull();
    });

    it('agesEnfantsDetectes vide → null', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('âge négatif → null', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [-1] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('âge > 18 → null', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [19] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('âge décimal → tronqué', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [7.9] },
        workspaceCountry: 'FRANCE',
      })).toBe(7);
    });

    it('prend le premier enfant de la liste', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [3, 7, 12] },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeCondamnationPenale', () => {
    it('true → true', () => {
      expect(computeCondamnationPenale({
        aiData: { condamnationPenaleDetectee: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });

    it('false → false', () => {
      expect(computeCondamnationPenale({
        aiData: { condamnationPenaleDetectee: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });

    it('null → null', () => {
      expect(computeCondamnationPenale({
        aiData: { condamnationPenaleDetectee: null },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('absent → null', () => {
      expect(computeCondamnationPenale({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeDangerCaracterise', () => {
    it('réutilise dangerImmediatDetected (F-246) — true', () => {
      expect(computeDangerCaracterise({
        aiData: { dangerImmediatDetected: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });

    it('absent → null', () => {
      expect(computeDangerCaracterise({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeViolencesConjugales', () => {
    it('violences LMVSS true → true', () => {
      expect(computeViolencesConjugales({
        aiData: { violencesLmvss2022Detectees: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });

    it('violences alléguées non vide (F-246) → true', () => {
      expect(computeViolencesConjugales({
        aiData: { violencesAllegueesDetectees: ['PHYSIQUES'] },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });

    it('violences LMVSS false → false', () => {
      expect(computeViolencesConjugales({
        aiData: {
          violencesLmvss2022Detectees: false,
          violencesAllegueesDetectees: null,
        },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });

    it('liste vide + LMVSS undefined → null', () => {
      expect(computeViolencesConjugales({
        aiData: { violencesAllegueesDetectees: [] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('aucune info → null', () => {
      expect(computeViolencesConjugales({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('FRANCE + données complètes → 4', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(4);
    });

    it('BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('FRANCE + données partielles (âge seul) → 1', () => {
      expect(computePrefillCount({
        aiData: { agesEnfantsDetectes: [5] },
        workspaceCountry: 'FRANCE',
      })).toBe(1);
    });

    it('FRANCE + aucune donnée → 0', () => {
      expect(computePrefillCount({ aiData: {}, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('Object Rules namespace expose les 5 fonctions', () => {
      expect(typeof RetraitApFrPrefillRules.computeAgeEnfant).toBe('function');
      expect(typeof RetraitApFrPrefillRules.computeCondamnationPenale).toBe('function');
      expect(typeof RetraitApFrPrefillRules.computeDangerCaracterise).toBe('function');
      expect(typeof RetraitApFrPrefillRules.computeViolencesConjugales).toBe('function');
      expect(typeof RetraitApFrPrefillRules.computePrefillCount).toBe('function');
    });
  });
});
