import { DublinRecoursPrefillRules as Rules } from './dublin-recours-section-prefill-rules';

describe('DublinRecoursPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for future or malformed date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '2099-01-01' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateNotificationDecisionContestee: '01/04/2026' } })).toBe(0);
  });

  it('returns 1 when dateNotificationDecisionContestee is valid', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-01' },
    })).toBe(1);
  });

  it('falls back to dateNotificationOqtf when dateNotificationDecisionContestee absent', () => {
    expect(Rules.computeDateNotificationDecisionTransfert({
      aiData: { dateNotificationOqtf: '2026-04-01' },
    })).toBe('2026-04-01');
  });

  // SF-246-17 : etatMembreResponsable
  describe('computeEtatMembreResponsable', () => {
    it('returns trimmed text when present', () => {
      expect(Rules.computeEtatMembreResponsable({
        aiData: { dublinEtatMembreResponsable: '  ITALIE  ' },
      })).toBe('ITALIE');
    });

    it('returns null when empty or whitespace', () => {
      expect(Rules.computeEtatMembreResponsable({ aiData: { dublinEtatMembreResponsable: '' } })).toBeNull();
      expect(Rules.computeEtatMembreResponsable({ aiData: { dublinEtatMembreResponsable: '   ' } })).toBeNull();
    });

    it('returns null when absent or null', () => {
      expect(Rules.computeEtatMembreResponsable({ aiData: {} })).toBeNull();
      expect(Rules.computeEtatMembreResponsable({ aiData: { dublinEtatMembreResponsable: null } })).toBeNull();
    });

    it('returns null when BELGIQUE', () => {
      expect(Rules.computeEtatMembreResponsable({
        aiData: { dublinEtatMembreResponsable: 'ITALIE' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  // SF-246-17 : motifTransfert
  describe('computeMotifTransfert', () => {
    it('returns valid code from whitelist', () => {
      expect(Rules.computeMotifTransfert({
        aiData: { dublinMotifTransfert: 'DEMANDE_ASILE_AUTRE_ETAT' },
      })).toBe('DEMANDE_ASILE_AUTRE_ETAT');
    });

    it('is case-insensitive', () => {
      expect(Rules.computeMotifTransfert({
        aiData: { dublinMotifTransfert: 'visa_delivre_autre_etat' },
      })).toBe('VISA_DELIVRE_AUTRE_ETAT');
    });

    it('returns null for code outside whitelist', () => {
      expect(Rules.computeMotifTransfert({
        aiData: { dublinMotifTransfert: 'CODE_INCONNU' },
      })).toBeNull();
    });

    it('returns null when absent or null', () => {
      expect(Rules.computeMotifTransfert({ aiData: {} })).toBeNull();
      expect(Rules.computeMotifTransfert({ aiData: { dublinMotifTransfert: null } })).toBeNull();
    });

    it('returns null when BELGIQUE', () => {
      expect(Rules.computeMotifTransfert({
        aiData: { dublinMotifTransfert: 'AUTRE' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  // SF-246-17 : computePrefillCount now counts 3 fields
  it('returns 3 when all 3 fields filled', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        dateNotificationDecisionContestee: '2026-04-01',
        dublinEtatMembreResponsable: 'ITALIE',
        dublinMotifTransfert: 'DEMANDE_ASILE_AUTRE_ETAT',
      },
    })).toBe(3);
  });

  it('returns 2 when date and etatMembre filled', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        dateNotificationDecisionContestee: '2026-04-01',
        dublinEtatMembreResponsable: 'ESPAGNE',
      },
    })).toBe(2);
  });
});
