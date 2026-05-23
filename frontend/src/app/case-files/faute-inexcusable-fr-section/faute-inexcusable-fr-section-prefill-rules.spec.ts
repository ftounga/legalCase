/**
 * SF-212-10 — Tests unitaires du helper partagé
 * `faute-inexcusable-fr-section-prefill-rules`. Pattern aligné sur
 * SF-212-04 (forfait-jours-fr-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { FauteInexcusableFrSectionPrefillRules } from './faute-inexcusable-fr-section-prefill-rules';

describe('FauteInexcusableFrSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    fauteInexcusableConscienceDanger: true,
    fauteInexcusableSignalementPrior: false,
    fauteInexcusableMesuresPrevention: false,
    fauteInexcusableTauxIpp: 25,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs IPP hors plage [0, 100]', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, fauteInexcusableTauxIpp: 200 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les valeurs IPP négatives', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, fauteInexcusableTauxIpp: -5 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, fauteInexcusableConscienceDanger: 'oui' as unknown as boolean };
      expect(FauteInexcusableFrSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeConscienceDanger', () => {
    it('retourne true si présent et booléen', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeConscienceDanger({
        aiData: { fauteInexcusableConscienceDanger: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et booléen false', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeConscienceDanger({
        aiData: { fauteInexcusableConscienceDanger: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null si absent', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeConscienceDanger({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeConscienceDanger({
        aiData: { fauteInexcusableConscienceDanger: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeTauxIpp', () => {
    it('retourne la valeur entière si dans [0, 100]', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeTauxIpp({
        aiData: { fauteInexcusableTauxIpp: 35 },
        workspaceCountry: 'FRANCE',
      })).toBe(35);
    });
    it('retourne 0 admis comme borne basse', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeTauxIpp({
        aiData: { fauteInexcusableTauxIpp: 0 },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });
    it('retourne 100 admis comme borne haute', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeTauxIpp({
        aiData: { fauteInexcusableTauxIpp: 100 },
        workspaceCountry: 'FRANCE',
      })).toBe(100);
    });
    it('retourne null si > 100', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeTauxIpp({
        aiData: { fauteInexcusableTauxIpp: 150 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(FauteInexcusableFrSectionPrefillRules.computeTauxIpp({
        aiData: { fauteInexcusableTauxIpp: 25 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
