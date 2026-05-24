/**
 * SF-212-26 — Tests unitaires du helper partagé
 * `lanceur-alerte-protection-section-prefill-rules`. Pattern aligné sur
 * SF-212-10 (faute-inexcusable-fr-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { LanceurAlerteProtectionSectionPrefillRules } from './lanceur-alerte-protection-section-prefill-rules';

describe('LanceurAlerteProtectionSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    lanceurAlerteNatureSignalement: 'CRIME_DELIT',
    lanceurAlerteProcedure: 'EXTERNE',
    lanceurAlerteMesureRepresaille: true,
    lanceurAlerteNatureMesure: 'LICENCIEMENT',
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs enum hors plage', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, lanceurAlerteNatureSignalement: 'INEXISTANT' },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les procédures invalides', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, lanceurAlerteProcedure: 'INVALIDE' },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les natures de mesure invalides', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, lanceurAlerteNatureMesure: 'OTHER_VALUE' },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, lanceurAlerteMesureRepresaille: 'oui' as unknown as boolean };
      expect(LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeNatureSignalement', () => {
    it('accepte les 4 enum values valides', () => {
      const values = ['CRIME_DELIT', 'VIOLATION_DROIT_UE', 'MENACE_INTERET_GENERAL', 'AUTRE'];
      values.forEach((v) => {
        expect(LanceurAlerteProtectionSectionPrefillRules.computeNatureSignalement({
          aiData: { lanceurAlerteNatureSignalement: v },
          workspaceCountry: 'FRANCE',
        })).toBe(v);
      });
    });
    it('retourne null si absent', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeNatureSignalement({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeNatureSignalement({
        aiData: { lanceurAlerteNatureSignalement: 'CRIME_DELIT' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeProcedure', () => {
    it('accepte INTERNE, EXTERNE, DIVULGATION_PUBLIQUE', () => {
      const values = ['INTERNE', 'EXTERNE', 'DIVULGATION_PUBLIQUE'];
      values.forEach((v) => {
        expect(LanceurAlerteProtectionSectionPrefillRules.computeProcedure({
          aiData: { lanceurAlerteProcedure: v },
          workspaceCountry: 'FRANCE',
        })).toBe(v);
      });
    });
    it('retourne null sur valeur inconnue', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeProcedure({
        aiData: { lanceurAlerteProcedure: 'AUTRE_VALUE' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeMesureRepresaille', () => {
    it('retourne true si présent et booléen true', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeMesureRepresaille({
        aiData: { lanceurAlerteMesureRepresaille: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et booléen false', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeMesureRepresaille({
        aiData: { lanceurAlerteMesureRepresaille: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null si absent', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeMesureRepresaille({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeNatureMesure', () => {
    it('accepte les 5 enum values valides', () => {
      const values = ['LICENCIEMENT', 'SANCTION', 'MESURE_DISCRIMINATOIRE', 'AUTRE', 'AUCUNE'];
      values.forEach((v) => {
        expect(LanceurAlerteProtectionSectionPrefillRules.computeNatureMesure({
          aiData: { lanceurAlerteNatureMesure: v },
          workspaceCountry: 'FRANCE',
        })).toBe(v);
      });
    });
    it('retourne null sur valeur inconnue', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeNatureMesure({
        aiData: { lanceurAlerteNatureMesure: 'INVALID' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(LanceurAlerteProtectionSectionPrefillRules.computeNatureMesure({
        aiData: { lanceurAlerteNatureMesure: 'LICENCIEMENT' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
