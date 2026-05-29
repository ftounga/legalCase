import { CceExtremeUrgenceBePrefillRules as Rules } from './cce-extreme-urgence-be-section-prefill-rules';

describe('CceExtremeUrgenceBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        recoursExtremeUrgenceDateActe: '2026-05-01',
        recoursExtremeUrgenceTypeActe: 'OQT_EXECUTE',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateActeExecutoire(input)).toBeNull();
    expect(Rules.computeTypeActe(input)).toBeNull();
  });

  it('returns 1 when only 1 real field present (BELGIQUE)', () => {
    const input = {
      aiData: { recoursExtremeUrgenceDateActe: '2026-05-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateActeExecutoire(input)).toBe('2026-05-01');
    expect(Rules.computeTypeActe(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        recoursExtremeUrgenceDateActe: '2026-05-01',
        recoursExtremeUrgenceTypeActe: 'TRANSFERT_DUBLIN',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateActeExecutoire(input)).toBe('2026-05-01');
    expect(Rules.computeTypeActe(input)).toBe('TRANSFERT_DUBLIN');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('accepts the 5 typeActe whitelist values', () => {
    for (const v of [
      'OQT_EXECUTE',
      'TRANSFERT_DUBLIN',
      'REFUS_ACCES_TERRITOIRE',
      'EXPULSION_IMMEDIATE',
      'AUTRE',
    ]) {
      expect(Rules.computeTypeActe({
        aiData: { recoursExtremeUrgenceTypeActe: v },
        workspaceCountry: 'BELGIQUE',
      })).toBe(v);
    }
  });

  it('rejects typeActe outside whitelist', () => {
    expect(Rules.computeTypeActe({
      aiData: { recoursExtremeUrgenceTypeActe: 'REFUS_CREDIT' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeTypeActe({
      aiData: { recoursExtremeUrgenceTypeActe: null },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('normalizes typeActe case (oqt_execute -> OQT_EXECUTE)', () => {
    expect(Rules.computeTypeActe({
      aiData: { recoursExtremeUrgenceTypeActe: ' oqt_execute ' as 'OQT_EXECUTE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('OQT_EXECUTE');
  });

  it('rejects malformed / invalid acte dates', () => {
    expect(Rules.computeDateActeExecutoire({
      aiData: { recoursExtremeUrgenceDateActe: '01/05/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateActeExecutoire({
      aiData: { recoursExtremeUrgenceDateActe: '2026-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateActeExecutoire({
      aiData: { recoursExtremeUrgenceDateActe: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields recoursForme / dateRecours', () => {
    const input = {
      aiData: {
        recoursExtremeUrgenceDateActe: '2026-05-01',
        recoursExtremeUrgenceTypeActe: 'OQT_EXECUTE',
        recoursForme: true,
        dateRecours: '2026-05-04',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('does NOT count cce-annulation 30j fields (other Immigration BE tool)', () => {
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
