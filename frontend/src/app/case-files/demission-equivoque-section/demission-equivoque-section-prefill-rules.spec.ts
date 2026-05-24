/**
 * SF-212-22 — Tests unitaires du helper partagé
 * `demission-equivoque-section-prefill-rules`. Pattern aligné sur SF-212-20
 * (mise-a-pied-disciplinaire-section-prefill-rules).
 *
 * **Note SF-212-21** : le sous-objet IA backend n'est pas livré dans cette
 * SF (limite JVM 255 du record `TravailExtractedData`). Le helper renvoie
 * toujours 0 — les tests valident ce contrat.
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { DemissionEquivoqueSectionPrefillRules } from './demission-equivoque-section-prefill-rules';

describe('DemissionEquivoqueSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    // Pas de sous-objet `demissionEquivoqueDetail` — non disponible
    // tant que la SF de rattrapage backend n'aura pas livré le sous-record
    // dans `TravailExtractedData`.
  };

  describe('computePrefillCount', () => {
    it('retourne 0 sur fixture IA FR (sous-objet non disponible)', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 hors France', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 sans workspaceCountry', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
      })).toBe(0);
    });

    it('retourne 0 avec aiData undefined', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });
  });
});
