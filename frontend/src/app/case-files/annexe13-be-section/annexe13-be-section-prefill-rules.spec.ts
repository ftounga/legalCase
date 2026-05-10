import { Annexe13BePrefillRules as Rules, MOTIFS_OQT_WHITELIST } from './annexe13-be-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 * Default contexte BE pour les cas nominaux ; cas FR explicite pour le gating.
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };

describe('Annexe13BePrefillRules', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 0 when delai is negative or non-integer (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { delaiDepartImposeJours: -1 } })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { delaiDepartImposeJours: 3.5 } })).toBe(0);
  });

  it('returns 0 when motif is unknown (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { motifOqtCodeBe: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when only date is set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateNotificationAnnexe13: '2026-04-01' } })).toBe(1);
  });

  it('returns 1 when only delai jours is set (>=0) (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { delaiDepartImposeJours: 0 } })).toBe(1);
    expect(Rules.computePrefillCount({ ...BE, aiData: { delaiDepartImposeJours: 30 } })).toBe(1);
  });

  it('returns 1 when only transfertImminent boolean is set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { transfertImminentDetected: false } })).toBe(1);
  });

  it('returns 1 when only motif valid (case sensitive) (BE)', () => {
    const motif = [...MOTIFS_OQT_WHITELIST][0];
    expect(Rules.computePrefillCount({ ...BE, aiData: { motifOqtCodeBe: motif } })).toBe(1);
  });

  it('returns N=4 when all four sources alimente (BELGIQUE)', () => {
    const motif = [...MOTIFS_OQT_WHITELIST][0];
    const input = {
      ...BE,
      aiData: {
        dateNotificationAnnexe13: '2026-04-01',
        delaiDepartImposeJours: 7,
        motifOqtCodeBe: motif,
        transfertImminentDetected: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  // F-236 SF-236-04 — gating BE-only.
  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const motif = [...MOTIFS_OQT_WHITELIST][0];
    const input = {
      aiData: {
        dateNotificationAnnexe13: '2026-04-01',
        delaiDepartImposeJours: 7,
        motifOqtCodeBe: motif,
        transfertImminentDetected: true,
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateNotificationAnnexe13(input)).toBeNull();
    expect(Rules.computeDelaiDepartImposeJours(input)).toBeNull();
    expect(Rules.computeMotifOqt(input)).toBeNull();
    expect(Rules.computeTransfertImminent(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    const input = {
      aiData: { dateNotificationAnnexe13: '2026-04-01' },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
