import { OqtfSansDelaiPrefillRules as Rules, ALLOWED_MOTIFS_SANS_DELAI } from './oqtf-sans-delai-section-prefill-rules';

describe('OqtfSansDelaiPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateHeureNotificationOqtfSansDelai: '2026-04-01T10:30',
        motifOqtfCode: [...ALLOWED_MOTIFS_SANS_DELAI][0],
        placementCraDetected: true,
        recoursFormeDetected: { reponse: 'OUI' },
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 for malformed datetime', () => {
    expect(Rules.computePrefillCount({ aiData: { dateHeureNotificationOqtfSansDelai: 'not-iso' } })).toBe(0);
  });

  it('returns 1 when only datetime valide', () => {
    const input = { aiData: { dateHeureNotificationOqtfSansDelai: '2026-04-01T10:30' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateHeureNotificationOqtf(input)).toBe('2026-04-01T10:30');
  });

  it('strips seconds when present in datetime', () => {
    const input = { aiData: { dateHeureNotificationOqtfSansDelai: '2026-04-01T10:30:45' } };
    expect(Rules.computeDateHeureNotificationOqtf(input)).toBe('2026-04-01T10:30');
  });

  it('returns 1 when only placementCra=true', () => {
    expect(Rules.computePrefillCount({ aiData: { placementCraDetected: true } })).toBe(1);
  });

  it('returns 1 when only placementCra=false (false posé)', () => {
    expect(Rules.computePrefillCount({ aiData: { placementCraDetected: false } })).toBe(1);
  });

  it('returns N=4 when all four sources alimente', () => {
    const someMotif = [...ALLOWED_MOTIFS_SANS_DELAI][0];
    const input = {
      aiData: {
        dateHeureNotificationOqtfSansDelai: '2026-04-01T10:30',
        motifOqtfCode: someMotif,
        placementCraDetected: true,
        recoursFormeDetected: { reponse: 'OUI' },
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});
