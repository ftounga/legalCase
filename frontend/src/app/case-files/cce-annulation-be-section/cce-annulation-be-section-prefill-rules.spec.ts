import { CceAnnulationBePrefillRules as Rules } from './cce-annulation-be-section-prefill-rules';

describe('CceAnnulationBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        recoursCceDateNotification: '2026-05-01',
        recoursCceTypeDecision: 'REFUS_TITRE',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateNotificationDecision(input)).toBeNull();
    expect(Rules.computeTypeDecision(input)).toBeNull();
  });

  it('returns 1 when only 1 real field present (BELGIQUE)', () => {
    const input = {
      aiData: { recoursCceDateNotification: '2026-05-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotificationDecision(input)).toBe('2026-05-01');
    expect(Rules.computeTypeDecision(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        recoursCceDateNotification: '2026-05-01',
        recoursCceTypeDecision: 'OQT_ANNEXE13',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotificationDecision(input)).toBe('2026-05-01');
    expect(Rules.computeTypeDecision(input)).toBe('OQT_ANNEXE13');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('accepts the 7 typeDecision whitelist values', () => {
    for (const v of [
      'REFUS_TITRE',
      'REFUS_REGROUPEMENT',
      'REFUS_9BIS',
      'REFUS_9TER',
      'OQT_ANNEXE13',
      'DECISION_CGRA',
      'AUTRE',
    ]) {
      expect(Rules.computeTypeDecision({
        aiData: { recoursCceTypeDecision: v },
        workspaceCountry: 'BELGIQUE',
      })).toBe(v);
    }
  });

  it('rejects typeDecision outside whitelist', () => {
    expect(Rules.computeTypeDecision({
      aiData: { recoursCceTypeDecision: 'REFUS_CREDIT' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeTypeDecision({
      aiData: { recoursCceTypeDecision: null },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('normalizes typeDecision case (refus_titre -> REFUS_TITRE)', () => {
    expect(Rules.computeTypeDecision({
      aiData: { recoursCceTypeDecision: ' refus_titre ' as 'REFUS_TITRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('REFUS_TITRE');
  });

  it('rejects malformed / invalid notification dates', () => {
    expect(Rules.computeDateNotificationDecision({
      aiData: { recoursCceDateNotification: '01/05/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateNotificationDecision({
      aiData: { recoursCceDateNotification: '2026-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateNotificationDecision({
      aiData: { recoursCceDateNotification: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields recoursForme / dateRecours', () => {
    const input = {
      aiData: {
        recoursCceDateNotification: '2026-05-01',
        recoursCceTypeDecision: 'REFUS_TITRE',
        // simulate AI accidentally returning these — must be ignored:
        recoursForme: true,
        dateRecours: '2026-05-10',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('does NOT count regroupement-10ter fields (other Immigration BE tool)', () => {
    const input = {
      aiData: {
        be10terLienFamilial: 'CONJOINT',
        be10terTypeCarte: 'CARTE_B',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
