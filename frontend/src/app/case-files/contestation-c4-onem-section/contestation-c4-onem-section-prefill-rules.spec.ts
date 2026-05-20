import { ContestationC4OnemPrefillRules as Rules } from './contestation-c4-onem-section-prefill-rules';

describe('ContestationC4OnemPrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when all 3 fields malformed', () => {
    const input = {
      aiData: {
        dateNotificationDecisionOnem: '15/03/2025',  // non ISO
        dateDecisionDirecteur: '   ',  // empty
        recoursAdminDejaForme: 'oui' as unknown as boolean,  // non bool
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only dateNotificationDecisionOnem is set (ISO valid)', () => {
    const input = { aiData: { dateNotificationDecisionOnem: '2025-03-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateNotificationDecisionOnem(input)).toBe('2025-03-15');
  });

  it('returns 1 when only recoursAdminDejaForme=false is set', () => {
    const input = { aiData: { recoursAdminDejaForme: false } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRecoursAdminDejaForme(input)).toBe(false);
  });

  // ── Cas 2 : 2 champs pré-remplis ──────────────────────────────────────
  it('returns 2 when 2 fields set', () => {
    const input = {
      aiData: {
        dateNotificationDecisionOnem: '2025-03-15',
        recoursAdminDejaForme: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  // ── Cas 3 : nominal complet ───────────────────────────────────────────
  it('returns 3 when all 3 instrumented fields are set', () => {
    const input = {
      aiData: {
        dateNotificationDecisionOnem: '2025-03-15',
        dateDecisionDirecteur: '2025-04-20',
        recoursAdminDejaForme: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
    expect(Rules.computeDateDecisionDirecteur(input)).toBe('2025-04-20');
    expect(Rules.computeRecoursAdminDejaForme(input)).toBe(true);
  });

  // ── recoursAdminDejaForme : true vs false vs null ─────────────────────
  describe('recoursAdminDejaForme — mapping booléen', () => {
    it('true → true (pré-fill)', () => {
      expect(Rules.computeRecoursAdminDejaForme({ aiData: { recoursAdminDejaForme: true } })).toBe(true);
    });

    it('false → false (pré-fill — signal exploitable)', () => {
      expect(Rules.computeRecoursAdminDejaForme({ aiData: { recoursAdminDejaForme: false } })).toBe(false);
    });

    it('null/undefined → null (pas de pré-fill, défaut form = false)', () => {
      expect(Rules.computeRecoursAdminDejaForme({ aiData: {} })).toBeNull();
      expect(Rules.computeRecoursAdminDejaForme({ aiData: { recoursAdminDejaForme: null } })).toBeNull();
      expect(Rules.computeRecoursAdminDejaForme({ aiData: { recoursAdminDejaForme: undefined } })).toBeNull();
    });

    it('valeur non-bool → null (string, number)', () => {
      expect(Rules.computeRecoursAdminDejaForme({
        aiData: { recoursAdminDejaForme: 'true' as unknown as boolean },
      })).toBeNull();
      expect(Rules.computeRecoursAdminDejaForme({
        aiData: { recoursAdminDejaForme: 1 as unknown as boolean },
      })).toBeNull();
    });
  });

  // ── Dates ISO ────────────────────────────────────────────────────────
  describe('Dates ISO — validation stricte', () => {
    it('dates non ISO → null', () => {
      expect(Rules.computeDateNotificationDecisionOnem({
        aiData: { dateNotificationDecisionOnem: '15/03/2025' },
      })).toBeNull();
      expect(Rules.computeDateDecisionDirecteur({
        aiData: { dateDecisionDirecteur: '20-04-2025' },
      })).toBeNull();
    });

    it('date ISO invalide (mois 13) → null', () => {
      expect(Rules.computeDateNotificationDecisionOnem({
        aiData: { dateNotificationDecisionOnem: '2025-13-99' },
      })).toBeNull();
    });

    it('date ISO valide → conservée', () => {
      expect(Rules.computeDateNotificationDecisionOnem({
        aiData: { dateNotificationDecisionOnem: '2025-03-15' },
      })).toBe('2025-03-15');
    });

    it('date non string → null', () => {
      expect(Rules.computeDateNotificationDecisionOnem({
        aiData: { dateNotificationDecisionOnem: 12345 as unknown as string },
      })).toBeNull();
    });
  });
});
