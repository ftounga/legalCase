/**
 * SF-216-14 — Tests Jest pour `audition-mineur-fr-section-prefill-rules.ts`.
 *
 * V1 — 2 champs pré-remplissables (âge enfant, demande formalisée).
 * FRANCE UNIQUEMENT.
 */

import {
  AuditionMineurFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './audition-mineur-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('audition-mineur-fr-prefill-rules (SF-216-14)', () => {
  describe('computePrefillCount', () => {
    it('aiData absent → 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('BELGIQUE → 0 (gate single-country)', () => {
      const aiData = {
        agesEnfantsDetectes: [10, 7],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'BELGIQUE' }),
      ).toBe(0);
    });

    it('FRANCE + 2 champs IA présents → 2', () => {
      const aiData = {
        agesEnfantsDetectes: [10],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(2);
    });

    it('FRANCE + âge seul → 1', () => {
      const aiData = {
        agesEnfantsDetectes: [12],
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(1);
    });

    it('FRANCE + demande formalisée seule → 1', () => {
      const aiData = {
        demandeAuditionFormaliseeDetectee: false,
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(1);
    });

    it('FRANCE + agesEnfantsDetectes vide → 0', () => {
      const aiData = {
        agesEnfantsDetectes: [],
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(0);
    });

    it('FRANCE + âge invalide (négatif ou >= 18) → 0', () => {
      const aiData = {
        agesEnfantsDetectes: [25],
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(0);
    });
  });

  describe('prefillFromAi', () => {
    it('FRANCE + IA complète → 2 valeurs', () => {
      const aiData = {
        agesEnfantsDetectes: [10, 6],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      const v = prefillFromAi({ aiData, workspaceCountry: 'FRANCE' });
      expect(v.ageEnfant).toBe(10);
      expect(v.demandeFormalisee).toBe(true);
    });

    it('BELGIQUE → tout null', () => {
      const aiData = {
        agesEnfantsDetectes: [10],
        demandeAuditionFormaliseeDetectee: true,
      } as unknown as FamilleExtractedData;
      const v = prefillFromAi({ aiData, workspaceCountry: 'BELGIQUE' });
      expect(v.ageEnfant).toBeNull();
      expect(v.demandeFormalisee).toBeNull();
    });

    it('FRANCE + premier enfant = 0 (bébé) → ageEnfant pré-rempli à 0', () => {
      const aiData = {
        agesEnfantsDetectes: [0],
      } as unknown as FamilleExtractedData;
      const v = prefillFromAi({ aiData, workspaceCountry: 'FRANCE' });
      expect(v.ageEnfant).toBe(0);
    });
  });

  describe('export AuditionMineurFrPrefillRules object', () => {
    it('expose computePrefillCount + prefillFromAi', () => {
      expect(typeof AuditionMineurFrPrefillRules.computePrefillCount).toBe('function');
      expect(typeof AuditionMineurFrPrefillRules.prefillFromAi).toBe('function');
    });
  });
});
