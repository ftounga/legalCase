import { AtFedrisDeclarationPrefillRules as Rules } from './at-fedris-declaration-section-prefill-rules';

describe('AtFedrisDeclarationPrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when both fields malformed', () => {
    const input = {
      aiData: {
        dateAccident: '15/03/2025',  // non ISO
        dateConnaissanceAccidentEmployeur: '   ',  // empty
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only dateAccident is set (ISO valid)', () => {
    const input = { aiData: { dateAccident: '2025-03-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateAccident(input)).toBe('2025-03-15');
    expect(Rules.computeDateConnaissanceEmployeur(input)).toBeNull();
  });

  it('returns 1 when only dateConnaissanceAccidentEmployeur is set', () => {
    const input = { aiData: { dateConnaissanceAccidentEmployeur: '2025-03-16' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateConnaissanceEmployeur(input)).toBe('2025-03-16');
    expect(Rules.computeDateAccident(input)).toBeNull();
  });

  // ── Cas 2 : nominal complet ───────────────────────────────────────────
  it('returns 2 when both instrumented fields are set', () => {
    const input = {
      aiData: {
        dateAccident: '2025-03-15',
        dateConnaissanceAccidentEmployeur: '2025-03-16',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeDateAccident(input)).toBe('2025-03-15');
    expect(Rules.computeDateConnaissanceEmployeur(input)).toBe('2025-03-16');
  });

  // ── Dates ISO ────────────────────────────────────────────────────────
  describe('Dates ISO — validation stricte', () => {
    it('dates non ISO → null', () => {
      expect(Rules.computeDateAccident({
        aiData: { dateAccident: '15/03/2025' },
      })).toBeNull();
      expect(Rules.computeDateConnaissanceEmployeur({
        aiData: { dateConnaissanceAccidentEmployeur: '16-03-2025' },
      })).toBeNull();
    });

    it('date ISO invalide (mois 13) → null', () => {
      expect(Rules.computeDateAccident({
        aiData: { dateAccident: '2025-13-99' },
      })).toBeNull();
    });

    it('date ISO valide → conservée', () => {
      expect(Rules.computeDateAccident({
        aiData: { dateAccident: '2025-03-15' },
      })).toBe('2025-03-15');
    });

    it('date non string → null', () => {
      expect(Rules.computeDateAccident({
        aiData: { dateAccident: 12345 as unknown as string },
      })).toBeNull();
      expect(Rules.computeDateConnaissanceEmployeur({
        aiData: { dateConnaissanceAccidentEmployeur: true as unknown as string },
      })).toBeNull();
    });
  });
});
