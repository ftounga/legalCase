/**
 * SF-212-20 — Tests unitaires du helper partagé
 * `mise-a-pied-disciplinaire-section-prefill-rules`. Pattern aligné sur
 * SF-212-14 (mutation-clause-mobilite-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { MiseAPiedDisciplinaireSectionPrefillRules } from './mise-a-pied-disciplinaire-section-prefill-rules';

describe('MiseAPiedDisciplinaireSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    mapDisciplinaireNature: 'DISCIPLINAIRE',
    mapDisciplinaireProcedureSuivie: true,
    mapDisciplinairePrescriptionFaute: true,
    mapDisciplinaireDureeRi: true,
    mapDisciplinaireDureeJours: 3,
    mapDisciplinaireSalaireSuspendu: true,
    mapDisciplinaireSanctionsAnterieures: false,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 7 sur fixture IA FR complète', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(7);
    });

    it('retourne 0 hors France', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs hors plage de la durée', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, mapDisciplinaireDureeJours: 999 },
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, mapDisciplinaireProcedureSuivie: 'oui' as unknown as boolean };
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('ignore la durée négative', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, mapDisciplinaireDureeJours: -1 },
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('ignore une nature hors enum', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, mapDisciplinaireNature: 'PROVISOIRE' },
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });
  });

  describe('computeNature', () => {
    it('retourne DISCIPLINAIRE si présent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeNature({
        aiData: { mapDisciplinaireNature: 'DISCIPLINAIRE' },
        workspaceCountry: 'FRANCE',
      })).toBe('DISCIPLINAIRE');
    });
    it('retourne CONSERVATOIRE si présent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeNature({
        aiData: { mapDisciplinaireNature: 'CONSERVATOIRE' },
        workspaceCountry: 'FRANCE',
      })).toBe('CONSERVATOIRE');
    });
    it('retourne INCONNUE si présent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeNature({
        aiData: { mapDisciplinaireNature: 'INCONNUE' },
        workspaceCountry: 'FRANCE',
      })).toBe('INCONNUE');
    });
    it('retourne null pour valeur hors enum', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeNature({
        aiData: { mapDisciplinaireNature: 'INVALID' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeNature({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeNature({
        aiData: { mapDisciplinaireNature: 'DISCIPLINAIRE' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeProcedureSuivie', () => {
    it('retourne true si présent et booléen', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeProcedureSuivie({
        aiData: { mapDisciplinaireProcedureSuivie: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et booléen false', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeProcedureSuivie({
        aiData: { mapDisciplinaireProcedureSuivie: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null si absent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeProcedureSuivie({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computePrescription', () => {
    it('retourne false si présent et booléen false', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrescription({
        aiData: { mapDisciplinairePrescriptionFaute: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null hors France', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computePrescription({
        aiData: { mapDisciplinairePrescriptionFaute: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeDureeRi', () => {
    it('retourne true si présent et booléen', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeRi({
        aiData: { mapDisciplinaireDureeRi: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeRi({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeDureeJours', () => {
    it('retourne la valeur entière dans la plage [0, 365]', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeJours({
        aiData: { mapDisciplinaireDureeJours: 5 },
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });
    it('tronque les décimaux', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeJours({
        aiData: { mapDisciplinaireDureeJours: 3.7 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
    it('retourne null pour valeur négative', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeJours({
        aiData: { mapDisciplinaireDureeJours: -1 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null pour valeur > 365', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeJours({
        aiData: { mapDisciplinaireDureeJours: 1000 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeJours({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null pour non-number', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeDureeJours({
        aiData: { mapDisciplinaireDureeJours: '4' as unknown as number },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeSalaireSuspendu', () => {
    it('retourne true si présent et booléen', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeSalaireSuspendu({
        aiData: { mapDisciplinaireSalaireSuspendu: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeSalaireSuspendu({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeSanctionsAnterieures', () => {
    it('retourne true si présent et booléen', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeSanctionsAnterieures({
        aiData: { mapDisciplinaireSanctionsAnterieures: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null hors France', () => {
      expect(MiseAPiedDisciplinaireSectionPrefillRules.computeSanctionsAnterieures({
        aiData: { mapDisciplinaireSanctionsAnterieures: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
