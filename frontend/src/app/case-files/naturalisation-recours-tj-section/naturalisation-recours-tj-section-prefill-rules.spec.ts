import { NaturalisationRecoursTjPrefillRules as Rules } from './naturalisation-recours-tj-section-prefill-rules';

describe('NaturalisationRecoursTjPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { naturalisationVoie: 'MARIAGE', naturalisationDateRefus: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeVoieNaturalisation(input)).toBeNull();
    expect(Rules.computeDateRefusDeclaration(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 2 when voie + date are both valid (FRANCE)', () => {
    const input = { aiData: { naturalisationVoie: 'MARIAGE', naturalisationDateRefus: '2026-01-15' } };
    expect(Rules.computeVoieNaturalisation(input)).toBe('MARIAGE');
    expect(Rules.computeDateRefusDeclaration(input)).toBe('2026-01-15');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 1 when only voie is present', () => {
    expect(Rules.computePrefillCount({ aiData: { naturalisationVoie: 'ASCENDANT' } })).toBe(1);
  });

  it('returns 1 when only date is present', () => {
    expect(Rules.computePrefillCount({ aiData: { naturalisationDateRefus: '2026-03-01' } })).toBe(1);
  });

  it('accepts the three voie codes and trims them', () => {
    expect(Rules.computeVoieNaturalisation({ aiData: { naturalisationVoie: 'MARIAGE' } })).toBe('MARIAGE');
    expect(Rules.computeVoieNaturalisation({ aiData: { naturalisationVoie: 'ASCENDANT' } })).toBe('ASCENDANT');
    expect(Rules.computeVoieNaturalisation({ aiData: { naturalisationVoie: '  MINEUR_22_1  ' } })).toBe('MINEUR_22_1');
  });

  it('rejects unknown / non-string voie values', () => {
    expect(Rules.computeVoieNaturalisation({ aiData: { naturalisationVoie: 'AUTRE' } })).toBeNull();
    expect(Rules.computeVoieNaturalisation({ aiData: { naturalisationVoie: 'mariage' } })).toBeNull();
    expect(Rules.computeVoieNaturalisation({
      aiData: { naturalisationVoie: 1 as unknown as 'MARIAGE' },
    })).toBeNull();
    expect(Rules.computeVoieNaturalisation({ aiData: { naturalisationVoie: null } })).toBeNull();
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateRefusDeclaration({ aiData: { naturalisationDateRefus: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateRefusDeclaration({ aiData: { naturalisationDateRefus: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateRefusDeclaration({ aiData: { naturalisationDateRefus: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateRefusDeclaration({
      aiData: { naturalisationDateRefus: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateRefusDeclaration({ aiData: { naturalisationDateRefus: null } })).toBeNull();
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { naturalisationVoie: 'ASCENDANT', naturalisationDateRefus: '2026-03-10' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
