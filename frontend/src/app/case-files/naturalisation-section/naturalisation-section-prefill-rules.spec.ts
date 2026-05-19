import { NaturalisationPrefillRules as Rules } from './naturalisation-section-prefill-rules';

describe('NaturalisationPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { natDureeResidenceReguliereAnnees: 5 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(Rules.computeDureeResidenceReguliereAnnees({
      aiData: { natDureeResidenceReguliereAnnees: 5 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── SF-246-19 : computeDureeResidenceReguliereAnnees ──────────────────
  it('computeDureeResidenceReguliereAnnees returns null when absent', () => {
    expect(Rules.computeDureeResidenceReguliereAnnees({ aiData: {} })).toBeNull();
  });

  it('computeDureeResidenceReguliereAnnees returns null for > 70', () => {
    expect(Rules.computeDureeResidenceReguliereAnnees({ aiData: { natDureeResidenceReguliereAnnees: 71 } })).toBeNull();
  });

  it('computeDureeResidenceReguliereAnnees returns value for 0–70', () => {
    expect(Rules.computeDureeResidenceReguliereAnnees({ aiData: { natDureeResidenceReguliereAnnees: 5 } })).toBe(5);
    expect(Rules.computeDureeResidenceReguliereAnnees({ aiData: { natDureeResidenceReguliereAnnees: 0 } })).toBe(0);
    expect(Rules.computeDureeResidenceReguliereAnnees({ aiData: { natDureeResidenceReguliereAnnees: 70 } })).toBe(70);
  });

  // ── SF-246-19 : computeDureeMariageAnnees ────────────────────────────
  it('computeDureeMariageAnnees returns null when absent', () => {
    expect(Rules.computeDureeMariageAnnees({ aiData: {} })).toBeNull();
  });

  it('computeDureeMariageAnnees returns null for > 70', () => {
    expect(Rules.computeDureeMariageAnnees({ aiData: { natDureeMariageAnnees: 71 } })).toBeNull();
  });

  it('computeDureeMariageAnnees returns value for 0–70', () => {
    expect(Rules.computeDureeMariageAnnees({ aiData: { natDureeMariageAnnees: 4 } })).toBe(4);
    expect(Rules.computeDureeMariageAnnees({ aiData: { natDureeMariageAnnees: 0 } })).toBe(0);
  });

  // ── SF-246-19 : computeAgeDemandeur ──────────────────────────────────
  it('computeAgeDemandeur returns null when absent', () => {
    expect(Rules.computeAgeDemandeur({ aiData: {} })).toBeNull();
  });

  it('computeAgeDemandeur returns null for > 120', () => {
    expect(Rules.computeAgeDemandeur({ aiData: { natAgeDemandeur: 121 } })).toBeNull();
  });

  it('computeAgeDemandeur returns value for 0–120', () => {
    expect(Rules.computeAgeDemandeur({ aiData: { natAgeDemandeur: 35 } })).toBe(35);
    expect(Rules.computeAgeDemandeur({ aiData: { natAgeDemandeur: 0 } })).toBe(0);
    expect(Rules.computeAgeDemandeur({ aiData: { natAgeDemandeur: 120 } })).toBe(120);
  });

  // ── computePrefillCount — cas maximal ────────────────────────────────
  it('returns 3 when all 3 fields present', () => {
    const input = {
      aiData: {
        natDureeResidenceReguliereAnnees: 5,
        natDureeMariageAnnees: 4,
        natAgeDemandeur: 35,
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('returns 1 when only dureeResidence present', () => {
    expect(Rules.computePrefillCount({ aiData: { natDureeResidenceReguliereAnnees: 5 } })).toBe(1);
  });

  it('returns 0 for BELGIQUE even with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: { natDureeResidenceReguliereAnnees: 5, natAgeDemandeur: 35 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
