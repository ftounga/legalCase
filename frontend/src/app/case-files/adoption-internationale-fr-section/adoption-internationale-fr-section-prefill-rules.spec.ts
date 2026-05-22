/**
 * SF-216-18 — Tests Jest pour
 * `adoption-internationale-fr-section-prefill-rules.ts`.
 *
 * V1 — 3 champs pré-remplissables (pays, agrément, exequatur). FRANCE
 * UNIQUEMENT.
 */

import {
  AdoptionInternationaleFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './adoption-internationale-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('adoption-internationale-fr-prefill-rules (SF-216-18)', () => {
  describe('computePrefillCount', () => {
    it('aiData absent → 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('BELGIQUE → 0 (gate single-country)', () => {
      const aiData = {
        paysOrigineAdopteDetecte: 'VIETNAM',
        agrement2025DetecteValide: true,
        exequaturRequisDetecte: false,
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'BELGIQUE' }),
      ).toBe(0);
    });

    it('FRANCE + 3 champs IA présents → 3', () => {
      const aiData = {
        paysOrigineAdopteDetecte: 'VIETNAM',
        agrement2025DetecteValide: true,
        exequaturRequisDetecte: false,
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(3);
    });

    it('FRANCE + pays seul → 1', () => {
      const aiData = {
        paysOrigineAdopteDetecte: 'COLOMBIE',
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(1);
    });

    it('FRANCE + pays vide → 0', () => {
      const aiData = {
        paysOrigineAdopteDetecte: '   ',
      } as unknown as FamilleExtractedData;
      expect(
        computePrefillCount({ aiData, workspaceCountry: 'FRANCE' }),
      ).toBe(0);
    });
  });

  describe('prefillFromAi', () => {
    it('FRANCE + IA complète → 3 valeurs', () => {
      const aiData = {
        paysOrigineAdopteDetecte: 'VIETNAM',
        agrement2025DetecteValide: true,
        exequaturRequisDetecte: false,
      } as unknown as FamilleExtractedData;
      const v = prefillFromAi({ aiData, workspaceCountry: 'FRANCE' });
      expect(v.paysOrigineEnfant).toBe('VIETNAM');
      expect(v.agrement2025).toBe(true);
      expect(v.exequaturRequis).toBe(false);
    });

    it('BELGIQUE → tout null', () => {
      const aiData = {
        paysOrigineAdopteDetecte: 'VIETNAM',
        agrement2025DetecteValide: true,
      } as unknown as FamilleExtractedData;
      const v = prefillFromAi({ aiData, workspaceCountry: 'BELGIQUE' });
      expect(v.paysOrigineEnfant).toBeNull();
      expect(v.agrement2025).toBeNull();
      expect(v.exequaturRequis).toBeNull();
    });
  });

  describe('export AdoptionInternationaleFrPrefillRules object', () => {
    it('expose computePrefillCount + prefillFromAi', () => {
      expect(typeof AdoptionInternationaleFrPrefillRules.computePrefillCount).toBe('function');
      expect(typeof AdoptionInternationaleFrPrefillRules.prefillFromAi).toBe('function');
    });
  });
});
