import { OqtfAvecDelaiPrefillRules as Rules } from './oqtf-avec-delai-section-prefill-rules';

describe('OqtfAvecDelaiPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateNotificationOqtf: '2026-01-15',
        motifOqtfCode: 'REFUS_TITRE',
        recoursFormeDetected: { reponse: 'OUI' },
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 for future date or malformed date', () => {
    expect(Rules.computePrefillCount({ aiData: { dateNotificationOqtf: '2099-12-31' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateNotificationOqtf: 'not-a-date' } })).toBe(0);
  });

  it('returns 0 for unknown motif', () => {
    expect(Rules.computePrefillCount({ aiData: { motifOqtfCode: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when only date is valid', () => {
    const input = { aiData: { dateNotificationOqtf: '2026-01-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 when only motif is valid', () => {
    const input = { aiData: { motifOqtfCode: 'REFUS_TITRE' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 when DetectedAnswer reponse=OUI', () => {
    const input = { aiData: { recoursFormeDetected: { reponse: 'OUI' } } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRecoursForme(input)).toBe(true);
  });

  it('returns 1 when DetectedAnswer reponse=NON (boolean false posé)', () => {
    const input = { aiData: { recoursFormeDetected: { reponse: 'NON' } } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRecoursForme(input)).toBe(false);
  });

  it('returns 0 for DetectedAnswer reponse=INCONNU', () => {
    expect(Rules.computeRecoursForme({ aiData: { recoursFormeDetected: { reponse: 'INCONNU' } } })).toBeNull();
  });

  it('returns N=3 when all three fields are present', () => {
    const input = {
      aiData: {
        dateNotificationOqtf: '2026-01-15',
        motifOqtfCode: 'REFUS_TITRE',
        recoursFormeDetected: { reponse: 'OUI' },
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
