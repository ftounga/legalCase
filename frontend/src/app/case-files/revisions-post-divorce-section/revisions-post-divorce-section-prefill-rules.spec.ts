/**
 * F-236 SF-236-02 — Tests `RevisionsPostDivorcePrefillRules`.
 * SF-246-08 : ajout tests garde BE + `revenusAnnuelsEpoux` (§5.2)
 * + `nbEnfantsACharge` champ réel. Cas N passe de 3 → 4 champs.
 */
import {
  RevisionsPostDivorcePrefillRules,
  computePrefillCount,
  computeRevenusEpoux,
} from './revisions-post-divorce-section-prefill-rules';

describe('RevisionsPostDivorcePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('retourne 0 pour workspaceCountry BELGIQUE (garde BE)', () => {
    expect(
      computePrefillCount({
        aiData: {
          revenusAnnuelsEpoux1Eur: 45000,
          nbEnfantsACharge: 2,
          revenusAnnuelsEpoux: 60000,
        },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — revenus seul (strict >0)', () => {
    expect(
      computePrefillCount({ aiData: { revenusAnnuelsEpoux1Eur: 45000 } }),
    ).toBe(1);
    expect(
      computePrefillCount({ aiData: { revenusAnnuelsEpoux1Eur: 0 } }),
    ).toBe(0);
  });

  it('nbEnfants 0 compte (valide)', () => {
    expect(computePrefillCount({ aiData: { nbEnfantsACharge: 0 } })).toBe(1);
  });

  describe('computeRevenusEpoux — invariant §5.2', () => {
    it('retourne null pour 0', () => {
      expect(computeRevenusEpoux({ aiData: { revenusAnnuelsEpoux: 0 } as any })).toBeNull();
    });

    it('retourne null pour valeur négative', () => {
      expect(computeRevenusEpoux({ aiData: { revenusAnnuelsEpoux: -1 } as any })).toBeNull();
    });

    it('retourne la valeur pour un montant strictement positif', () => {
      expect(computeRevenusEpoux({ aiData: { revenusAnnuelsEpoux: 60000 } as any })).toBe(60000);
    });
  });

  it('cas N — 4/4 champs (SF-246-08)', () => {
    expect(
      computePrefillCount({
        aiData: {
          revenusAnnuelsEpoux1Eur: 45000,
          revenusAnnuelsEpoux2Eur: 30000,
          nbEnfantsACharge: 2,
          revenusAnnuelsEpoux: 60000,
        },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(4);
  });

  it('surface', () => {
    expect(RevisionsPostDivorcePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
