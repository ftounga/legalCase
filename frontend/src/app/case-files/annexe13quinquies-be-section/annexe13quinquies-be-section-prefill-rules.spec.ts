import { Annexe13quinquiesBePrefillRules as Rules } from './annexe13quinquies-be-section-prefill-rules';

describe('Annexe13quinquiesBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        interdictionEntreeDateNotification: '2026-05-01',
        interdictionEntreeMotif: 'SEJOUR_IRREGULIER',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateNotificationAnnexe(input)).toBeNull();
    expect(Rules.computeMotifInterdictionEntree(input)).toBeNull();
  });

  it('returns 1 when only 1 real field present (BELGIQUE)', () => {
    const input = {
      aiData: { interdictionEntreeDateNotification: '2026-05-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotificationAnnexe(input)).toBe('2026-05-01');
    expect(Rules.computeMotifInterdictionEntree(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        interdictionEntreeDateNotification: '2026-05-01',
        interdictionEntreeMotif: 'MENACE_ORDRE_PUBLIC',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateNotificationAnnexe(input)).toBe('2026-05-01');
    expect(Rules.computeMotifInterdictionEntree(input)).toBe('MENACE_ORDRE_PUBLIC');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('accepts the 5 motif whitelist values', () => {
    for (const v of [
      'SEJOUR_IRREGULIER',
      'MENACE_ORDRE_PUBLIC',
      'RAISONS_SECURITE_NATIONALE',
      'ATTEINTE_INTERET_UE',
      'DECISION_JUDICIAIRE',
    ]) {
      expect(Rules.computeMotifInterdictionEntree({
        aiData: { interdictionEntreeMotif: v },
        workspaceCountry: 'BELGIQUE',
      })).toBe(v);
    }
  });

  it('rejects motif outside whitelist', () => {
    expect(Rules.computeMotifInterdictionEntree({
      aiData: { interdictionEntreeMotif: 'AUTRE_MOTIF' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeMotifInterdictionEntree({
      aiData: { interdictionEntreeMotif: null },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('normalizes motif case (sejour_irregulier -> SEJOUR_IRREGULIER)', () => {
    expect(Rules.computeMotifInterdictionEntree({
      aiData: { interdictionEntreeMotif: ' sejour_irregulier ' as 'SEJOUR_IRREGULIER' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('SEJOUR_IRREGULIER');
  });

  it('rejects malformed / invalid notification dates', () => {
    expect(Rules.computeDateNotificationAnnexe({
      aiData: { interdictionEntreeDateNotification: '01/05/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateNotificationAnnexe({
      aiData: { interdictionEntreeDateNotification: '2026-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateNotificationAnnexe({
      aiData: { interdictionEntreeDateNotification: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields precedentSejour / recoursForme / dateRecours', () => {
    const input = {
      aiData: {
        interdictionEntreeDateNotification: '2026-05-01',
        interdictionEntreeMotif: 'SEJOUR_IRREGULIER',
        // simulate AI accidentally returning these — must be ignored:
        precedentSejour: true,
        recoursForme: true,
        dateRecours: '2026-05-10',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('does NOT count cce-annulation fields (other Immigration BE tool)', () => {
    const input = {
      aiData: {
        recoursCceDateNotification: '2026-05-01',
        recoursCceTypeDecision: 'REFUS_TITRE',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
