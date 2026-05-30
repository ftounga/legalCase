/**
 * SF-218-02 — Tests unitaires du helper partagé
 * `appel-cph-section-prefill-rules`. Pattern aligné sur SF-212-38
 * (conciliation-cph-bca-section-prefill-rules).
 *
 * <p>F-218a — Procédure CPH avancée (P3 Travail FR).</p>
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { AppelCphSectionPrefillRules } from './appel-cph-section-prefill-rules';

describe('AppelCphSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    dateNotificationJugement: '2026-05-10',
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 1 sur fixture IA FR complète', () => {
      expect(AppelCphSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(1);
    });

    it('retourne 0 hors France', () => {
      expect(AppelCphSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(AppelCphSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 sur input vide {}', () => {
      expect(AppelCphSectionPrefillRules.computePrefillCount({})).toBe(0);
    });

    it('ignore une date au format invalide', () => {
      expect(AppelCphSectionPrefillRules.computePrefillCount({
        aiData: { dateNotificationJugement: '10/05/2026' },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });
  });

  describe('computeDateNotificationJugement', () => {
    it('retourne la date ISO valide (FRANCE)', () => {
      expect(AppelCphSectionPrefillRules.computeDateNotificationJugement({
        aiData: { dateNotificationJugement: '2026-05-10' },
        workspaceCountry: 'FRANCE',
      })).toBe('2026-05-10');
    });

    it('retourne null hors France', () => {
      expect(AppelCphSectionPrefillRules.computeDateNotificationJugement({
        aiData: { dateNotificationJugement: '2026-05-10' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });

    it('retourne null si format non ISO', () => {
      expect(AppelCphSectionPrefillRules.computeDateNotificationJugement({
        aiData: { dateNotificationJugement: '2026/05/10' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null si date impossible', () => {
      expect(AppelCphSectionPrefillRules.computeDateNotificationJugement({
        aiData: { dateNotificationJugement: '2026-13-99' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null si absent', () => {
      expect(AppelCphSectionPrefillRules.computeDateNotificationJugement({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });
});
