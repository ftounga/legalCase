/**
 * SF-212-22 / F-256 — Tests unitaires du helper partagé
 * `demission-equivoque-section-prefill-rules` (5 champs IA réactivés).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { DemissionEquivoqueSectionPrefillRules } from './demission-equivoque-section-prefill-rules';

describe('DemissionEquivoqueSectionPrefillRules', () => {
  const EMPTY_AI_DATA: TravailExtractedData = {};

  const FULL_AI_DATA: TravailExtractedData = {
    demissionEquivoqueDetail: {
      demissionModeExpression: 'SMS',
      demissionContexteAltercation: true,
      demissionPression: true,
      demissionRetractation: false,
      demissionManquementsEmployeur: true,
    },
  };

  describe('computePrefillCount', () => {
    it('retourne 0 sur fixture vide FR', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: EMPTY_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 hors France même si sous-objet rempli', () => {
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

    it('retourne 5 avec sous-objet entièrement rempli FR', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });

    it('retourne le bon partial count avec quelques champs', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePrefillCount({
        aiData: {
          demissionEquivoqueDetail: {
            demissionContexteAltercation: true,
            demissionRetractation: false,
          },
        },
        workspaceCountry: 'FRANCE',
      })).toBe(2);
    });
  });

  describe('compute* (par champ)', () => {
    it('computeModeExpression retourne string', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computeModeExpression({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe('SMS');
    });

    it('computeContexteAltercation retourne boolean', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computeContexteAltercation({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });

    it('computePression retourne null hors FR', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computePression({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });

    it('computeRetractation false correctement retourné', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computeRetractation({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });

    it('computeManquementsEmployeur retourne true', () => {
      expect(DemissionEquivoqueSectionPrefillRules.computeManquementsEmployeur({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
  });
});
