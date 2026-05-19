/**
 * F-236 SF-236-02 — Tests `PartageJudiciairePrefillRules`.
 * SF-246-07 : mise à jour suite réalignement sur le record réel `FamilleExtractedData`.
 */
import {
  PartageJudiciairePrefillRules,
  computePrefillCount,
  computeNombreCoindivisaires,
  computeValeurBiens,
} from './partage-judiciaire-section-prefill-rules';

describe('PartageJudiciairePrefillRules', () => {
  describe('computePrefillCount — cas 0', () => {
    it('retourne 0 pour un input vide', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('retourne 0 pour workspaceCountry BELGIQUE (garde BE)', () => {
      expect(
        computePrefillCount({
          aiData: {
            pvDifficultesEtablisDetected: true,
            nombreCoindivisairesDetecte: 3,
            valeurBiensIndivisionEur: 500000,
          } as any,
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('computePrefillCount — cas M', () => {
    it('pv + tentative seulement (2/4)', () => {
      expect(
        computePrefillCount({
          aiData: { pvDifficultesEtablisDetected: true, tentativeAmiableEpuiseueeDetected: false } as any,
        }),
      ).toBe(2);
    });
  });

  describe('computePrefillCount — cas N', () => {
    it('4/4 — tous les champs présents dont nombreCoindivisaires et valeurBiens (SF-246-07)', () => {
      expect(
        computePrefillCount({
          aiData: {
            pvDifficultesEtablisDetected: true,
            tentativeAmiableEpuiseueeDetected: true,
            nombreCoindivisairesDetecte: 3,
            valeurBiensIndivisionEur: 500000,
          } as any,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });
  });

  describe('computeNombreCoindivisaires — SF-246-07', () => {
    it('rejette les valeurs < 2 (garde métier : partage judiciaire implique ≥ 2 parties)', () => {
      expect(computeNombreCoindivisaires({ aiData: { nombreCoindivisairesDetecte: 1 } as any })).toBeNull();
    });

    it('accepte 2', () => {
      expect(computeNombreCoindivisaires({ aiData: { nombreCoindivisairesDetecte: 2 } as any })).toBe(2);
    });

    it('retourne null si absent', () => {
      expect(computeNombreCoindivisaires({ aiData: {} as any })).toBeNull();
    });
  });

  describe('computeValeurBiens — invariant §5.2 (jamais 0)', () => {
    it('retourne null pour 0', () => {
      expect(computeValeurBiens({ aiData: { valeurBiensIndivisionEur: 0 } as any })).toBeNull();
    });

    it('retourne null pour valeur négative', () => {
      expect(computeValeurBiens({ aiData: { valeurBiensIndivisionEur: -100 } as any })).toBeNull();
    });

    it('retourne la valeur pour un montant strictement positif', () => {
      expect(computeValeurBiens({ aiData: { valeurBiensIndivisionEur: 180000 } as any })).toBe(180000);
    });
  });

  it('surface', () => {
    expect(PartageJudiciairePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
