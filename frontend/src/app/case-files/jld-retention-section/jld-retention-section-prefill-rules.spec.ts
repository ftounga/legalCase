import { JldRetentionPrefillRules as Rules } from './jld-retention-section-prefill-rules';

describe('JldRetentionPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateNotificationOqtf: '2026-04-01',
        motifOqtfCode: 'REFUS_TITRE',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 for future date or malformed date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationOqtf: '2099-12-31' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateNotificationOqtf: 'not-a-date' } })).toBe(0);
  });

  it('returns 1 when only date is present', () => {
    const input = { aiData: { dateNotificationOqtf: '2026-04-01' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateNotificationPlacement(input)).toBe('2026-04-01');
  });

  it('returns 1 when only motif inferable (motifOqtfCode -> EXECUTION_OQTF)', () => {
    const input = { aiData: { motifOqtfCode: 'REFUS_TITRE' } };
    expect(Rules.computeMotifPlacement(input)).toBe('EXECUTION_OQTF');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 when both date and motif are present', () => {
    const input = {
      aiData: {
        dateNotificationOqtf: '2026-04-01',
        motifOqtfCode: 'REFUS_TITRE',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('falls back to dateNotificationDecisionContestee when dateNotificationOqtf absent', () => {
    const input = { aiData: { dateNotificationDecisionContestee: '2026-04-01' } };
    expect(Rules.computeDateNotificationPlacement(input)).toBe('2026-04-01');
  });

  it('returns null for motif when no IA signal', () => {
    expect(Rules.computeMotifPlacement({ aiData: {} })).toBeNull();
  });

  // SF-246-17 : computeRecoursForme
  describe('computeRecoursForme', () => {
    it('returns true when recoursFormeDetected.reponse is OUI', () => {
      expect(Rules.computeRecoursForme({
        aiData: { recoursFormeDetected: { reponse: 'OUI', confidence: 'HIGH' } },
      })).toBe(true);
    });

    it('returns false when recoursFormeDetected.reponse is NON', () => {
      expect(Rules.computeRecoursForme({
        aiData: { recoursFormeDetected: { reponse: 'NON', confidence: 'HIGH' } },
      })).toBe(false);
    });

    it('returns null when recoursFormeDetected.reponse is INCONNU', () => {
      expect(Rules.computeRecoursForme({
        aiData: { recoursFormeDetected: { reponse: 'INCONNU', confidence: 'LOW' } },
      })).toBeNull();
    });

    it('returns null when recoursFormeDetected absent or null', () => {
      expect(Rules.computeRecoursForme({ aiData: {} })).toBeNull();
      expect(Rules.computeRecoursForme({ aiData: { recoursFormeDetected: null } })).toBeNull();
    });

    it('is case-insensitive for reponse', () => {
      expect(Rules.computeRecoursForme({
        aiData: { recoursFormeDetected: { reponse: 'oui', confidence: 'HIGH' } },
      })).toBe(true);
    });

    it('returns null when BELGIQUE', () => {
      expect(Rules.computeRecoursForme({
        aiData: { recoursFormeDetected: { reponse: 'OUI', confidence: 'HIGH' } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  // SF-246-17 : computePrefillCount now counts up to 3 fields
  it('returns 3 when date + motif + recoursForme all detected', () => {
    const input = {
      aiData: {
        dateNotificationOqtf: '2026-04-01',
        motifOqtfCode: 'REFUS_TITRE',
        recoursFormeDetected: { reponse: 'OUI', confidence: 'HIGH' },
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('computeRecoursForme does not increment count when INCONNU', () => {
    const input = {
      aiData: {
        dateNotificationOqtf: '2026-04-01',
        motifOqtfCode: 'REFUS_TITRE',
        recoursFormeDetected: { reponse: 'INCONNU', confidence: 'LOW' },
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
