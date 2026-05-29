import { ProtectionTemporaireUkraineBePrefillRules as Rules } from './protection-temporaire-ukraine-be-section-prefill-rules';

describe('ProtectionTemporaireUkraineBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        ptUkraineDateArrivee: '2022-03-10',
        ptUkraineNationalite: true,
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateArrivee(input)).toBeNull();
    expect(Rules.computeNationaliteUkrainienne(input)).toBeNull();
  });

  it('returns 1 when only date arrivée present (BELGIQUE)', () => {
    const input = {
      aiData: { ptUkraineDateArrivee: '2022-03-10' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateArrivee(input)).toBe('2022-03-10');
    expect(Rules.computeNationaliteUkrainienne(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 when only nationalité=true present (BELGIQUE)', () => {
    const input = {
      aiData: { ptUkraineNationalite: true },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateArrivee(input)).toBeNull();
    expect(Rules.computeNationaliteUkrainienne(input)).toBe(true);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        ptUkraineDateArrivee: '2022-03-10',
        ptUkraineNationalite: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateArrivee(input)).toBe('2022-03-10');
    expect(Rules.computeNationaliteUkrainienne(input)).toBe(true);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('treats ptUkraineNationalite=false as NOT a prefill (null)', () => {
    const input = {
      aiData: { ptUkraineNationalite: false },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeNationaliteUkrainienne(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('rejects malformed / invalid arrival dates', () => {
    expect(Rules.computeDateArrivee({
      aiData: { ptUkraineDateArrivee: '10/03/2022' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateArrivee({
      aiData: { ptUkraineDateArrivee: '2022-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateArrivee({
      aiData: { ptUkraineDateArrivee: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields (residence / apatrides / membreFamille / titre)', () => {
    const input = {
      aiData: {
        ptUkraineDateArrivee: '2022-03-10',
        ptUkraineNationalite: true,
        // simulate AI accidentally returning these — must be ignored:
        residenceUkraineAvant24Fev2022: true,
        apatridesUkraine: true,
        membreFamilleProtege: true,
        titreSejourBE: 'ATTESTATION_IMMATRICULATION',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('does NOT count annexe13quinquies fields (other Immigration BE tool)', () => {
    const input = {
      aiData: {
        interdictionEntreeDateNotification: '2026-05-01',
        interdictionEntreeMotif: 'SEJOUR_IRREGULIER',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
