import { AppelCphPrefillRules as Rules } from './appel-cph-section-prefill-rules';

describe('AppelCphPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateNotificationJugement: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotificationJugement(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when dateNotificationJugement is a valid ISO date', () => {
    const input = { aiData: { dateNotificationJugement: '2026-01-15' } };
    expect(Rules.computeDateNotificationJugement(input)).toBe('2026-01-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateNotificationJugement({ aiData: { dateNotificationJugement: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateNotificationJugement({ aiData: { dateNotificationJugement: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateNotificationJugement({ aiData: { dateNotificationJugement: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateNotificationJugement({
      aiData: { dateNotificationJugement: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateNotificationJugement({ aiData: { dateNotificationJugement: null } })).toBeNull();
  });

  it('does NOT count appelCphEnvisage flag alone (it is a visibility trigger, not a form field)', () => {
    const input = { aiData: { appelCphEnvisage: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { dateNotificationJugement: '2026-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
