/**
 * SF-216-16 — Tests Jest pour `adoption-intra-fr-section-prefill-rules.ts`.
 *
 * V1 — PREFILL_COUNT_ALWAYS_ZERO : aucun champ saisissable n'est pré-rempli
 * (cf. SF-216-16 V1). Le helper est minimaliste mais conserve la signature
 * imposée par F-237 pour rester cohérent avec les autres outils F-216.
 */

import {
  AdoptionIntraFrPrefillRules,
  computePrefillCount,
} from './adoption-intra-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('adoption-intra-fr-prefill-rules (SF-216-16)', () => {
  describe('computePrefillCount — V1 always 0', () => {
    it('aiData absent → 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('FRANCE + aiData rempli → 0 (V1)', () => {
      const aiData = {
        adoptionIntraEnvisagee: true,
      } as unknown as FamilleExtractedData;
      expect(computePrefillCount({ aiData, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('BELGIQUE → 0', () => {
      const aiData = {
        adoptionIntraEnvisagee: true,
      } as unknown as FamilleExtractedData;
      expect(computePrefillCount({ aiData, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });
  });

  describe('export AdoptionIntraFrPrefillRules object', () => {
    it('expose computePrefillCount', () => {
      expect(typeof AdoptionIntraFrPrefillRules.computePrefillCount).toBe('function');
      expect(AdoptionIntraFrPrefillRules.computePrefillCount({})).toBe(0);
    });
  });
});
