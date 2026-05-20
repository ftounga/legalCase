import {
  CreditTempsBeSectionPrefillRules,
  computePrefillCount,
  computeAgeDemandeur,
  computeAncienneteMois,
  computeDateDemande,
} from './credit-temps-be-section-prefill-rules';

describe('CreditTempsBeSectionPrefillRules', () => {
  const NOW = new Date('2025-01-01');
  const BE = 'BELGIQUE';

  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({ workspaceCountry: BE, now: NOW })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: null, now: NOW })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: {}, now: NOW })).toBe(0);
    });

    it('cas M — âge seul retourne 1', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { ageDemandeurAnnees: 55 },
          now: NOW,
        }),
      ).toBe(1);
    });

    it('cas N — 2 champs nominal', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { dateEntree: '2020-01-01', ageDemandeurAnnees: 55 },
          now: NOW,
        }),
      ).toBe(2);
    });

    it('SF-246-23 — tous les 3 champs renseignés retourne 3', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: {
            dateEntree: '2020-01-01',
            ageDemandeurAnnees: 55,
            dateDemandeCreditTemps: '2024-03-01',
          },
          now: NOW,
        }),
      ).toBe(3);
    });
  });

  it('gating BE : workspaceCountry FRANCE retourne 0 même avec aiData complet', () => {
    expect(
      computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: { dateEntree: '2020-01-01', ageDemandeurAnnees: 55 },
        now: NOW,
      }),
    ).toBe(0);
    expect(
      computeAncienneteMois({
        workspaceCountry: 'FRANCE',
        aiData: { dateEntree: '2020-01-01' },
        now: NOW,
      }),
    ).toBeNull();
    expect(
      computeAgeDemandeur({
        workspaceCountry: 'FRANCE',
        aiData: { ageDemandeurAnnees: 55 },
      }),
    ).toBeNull();
  });

  it('gating BE : workspaceCountry absent retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateEntree: '2020-01-01', ageDemandeurAnnees: 55 },
        now: NOW,
      }),
    ).toBe(0);
  });

  it('rejette âge hors range', () => {
    expect(computeAgeDemandeur({ workspaceCountry: BE, aiData: { ageDemandeurAnnees: -1 } })).toBeNull();
    expect(computeAgeDemandeur({ workspaceCountry: BE, aiData: { ageDemandeurAnnees: 76 } })).toBeNull();
  });

  it('rejette dateEntree future', () => {
    expect(
      computePrefillCount({
        workspaceCountry: BE,
        aiData: { dateEntree: '2050-01-01' },
        now: NOW,
      }),
    ).toBe(0);
  });

  it('expose barrel', () => {
    expect(CreditTempsBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });

  // SF-246-23 — computeDateDemande

  describe('computeDateDemande (SF-246-23)', () => {
    it('retourne la date si string non vide', () => {
      expect(
        computeDateDemande({
          workspaceCountry: BE,
          aiData: { dateDemandeCreditTemps: '2024-03-01' },
        }),
      ).toBe('2024-03-01');
    });

    it('retourne null si string vide', () => {
      expect(
        computeDateDemande({
          workspaceCountry: BE,
          aiData: { dateDemandeCreditTemps: '' },
        }),
      ).toBeNull();
    });

    it('retourne null si absente', () => {
      expect(
        computeDateDemande({ workspaceCountry: BE, aiData: {} }),
      ).toBeNull();
    });

    it('retourne null si pays ≠ BELGIQUE', () => {
      expect(
        computeDateDemande({
          workspaceCountry: 'FRANCE',
          aiData: { dateDemandeCreditTemps: '2024-03-01' },
        }),
      ).toBeNull();
    });
  });
});
