import { CrrvRefusVisaPrefillRules as Rules } from './crrv-refus-visa-section-prefill-rules';

describe('CrrvRefusVisaPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for future or malformed date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2099-01-01' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '01/04/2026' } })).toBe(0);
  });

  it('returns 1 when date present', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
    })).toBe(1);
  });

  it('computeDateNotificationRefus returns null when not FRANCE', () => {
    expect(Rules.computeDateNotificationRefus({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // SF-246-17 : typeVisa
  describe('computeTypeVisa', () => {
    it('returns valid code from whitelist', () => {
      expect(Rules.computeTypeVisa({
        aiData: { crrvTypeVisa: 'COURT_SEJOUR' },
      })).toBe('COURT_SEJOUR');
    });

    it('is case-insensitive', () => {
      expect(Rules.computeTypeVisa({
        aiData: { crrvTypeVisa: 'long_sejour' },
      })).toBe('LONG_SEJOUR');
    });

    it('returns null for code outside whitelist', () => {
      expect(Rules.computeTypeVisa({
        aiData: { crrvTypeVisa: 'VISA_INCONNU' },
      })).toBeNull();
    });

    it('returns null when absent or null', () => {
      expect(Rules.computeTypeVisa({ aiData: {} })).toBeNull();
      expect(Rules.computeTypeVisa({ aiData: { crrvTypeVisa: null } })).toBeNull();
    });

    it('returns null when BELGIQUE', () => {
      expect(Rules.computeTypeVisa({
        aiData: { crrvTypeVisa: 'ETUDIANT' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  // SF-246-17 : motifRefus
  describe('computeMotifRefus', () => {
    it('returns trimmed text when present', () => {
      expect(Rules.computeMotifRefus({
        aiData: { crrvMotifRefus: '  Absence de lien avec la France  ' },
      })).toBe('Absence de lien avec la France');
    });

    it('returns null when empty or whitespace', () => {
      expect(Rules.computeMotifRefus({ aiData: { crrvMotifRefus: '' } })).toBeNull();
      expect(Rules.computeMotifRefus({ aiData: { crrvMotifRefus: '   ' } })).toBeNull();
    });

    it('returns null when absent or null', () => {
      expect(Rules.computeMotifRefus({ aiData: {} })).toBeNull();
      expect(Rules.computeMotifRefus({ aiData: { crrvMotifRefus: null } })).toBeNull();
    });

    it('returns null when BELGIQUE', () => {
      expect(Rules.computeMotifRefus({
        aiData: { crrvMotifRefus: 'Motif quelconque' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  // SF-246-17 : computePrefillCount now counts 3 fields
  it('returns 3 when all 3 fields filled', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        dateNotificationDecisionContestee: '2026-04-01',
        crrvTypeVisa: 'ETUDIANT',
        crrvMotifRefus: 'Absence de lien avec la France',
      },
    })).toBe(3);
  });

  it('returns 2 when date and typeVisa filled', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        dateNotificationDecisionContestee: '2026-04-01',
        crrvTypeVisa: 'COURT_SEJOUR',
      },
    })).toBe(2);
  });
});
