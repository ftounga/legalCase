import { PourvoiCassationSocPrefillRules as Rules } from './pourvoi-cassation-soc-section-prefill-rules';

describe('PourvoiCassationSocPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateNotificationArretAppel: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotificationArret(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when dateNotificationArretAppel is a valid ISO date', () => {
    const input = { aiData: { dateNotificationArretAppel: '2026-01-15' } };
    expect(Rules.computeDateNotificationArret(input)).toBe('2026-01-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateNotificationArret({ aiData: { dateNotificationArretAppel: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateNotificationArret({ aiData: { dateNotificationArretAppel: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateNotificationArret({ aiData: { dateNotificationArretAppel: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateNotificationArret({
      aiData: { dateNotificationArretAppel: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateNotificationArret({ aiData: { dateNotificationArretAppel: null } })).toBeNull();
  });

  it('does NOT count pourvoiCassationSocEnvisage flag alone (it is a visibility trigger, not a form field)', () => {
    const input = { aiData: { pourvoiCassationSocEnvisage: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { dateNotificationArretAppel: '2026-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });
});
