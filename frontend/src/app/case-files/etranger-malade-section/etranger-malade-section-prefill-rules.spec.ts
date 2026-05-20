import { EtrangerMaladePrefillRules as Rules } from './etranger-malade-section-prefill-rules';

describe('EtrangerMaladePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        etrangerMaladePathologie: 'Diabète type 1 instable',
        etrangerMaladeTraitementDisponible: false,
        etrangerMaladeAvisOFII: 'FAVORABLE',
        etrangerMalaDateAvisOFII: '2026-04-01',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only pathologiePrincipale is present', () => {
    const input = { aiData: { etrangerMaladePathologie: 'Cancer hépatique stade 4' } };
    expect(Rules.computePathologiePrincipale(input)).toBe('Cancer hépatique stade 4');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('rejects empty / whitespace / >500 char pathologie', () => {
    expect(Rules.computePathologiePrincipale({ aiData: { etrangerMaladePathologie: '' } })).toBeNull();
    expect(Rules.computePathologiePrincipale({ aiData: { etrangerMaladePathologie: '   ' } })).toBeNull();
    const long = 'a'.repeat(501);
    expect(Rules.computePathologiePrincipale({ aiData: { etrangerMaladePathologie: long } })).toBeNull();
  });

  it('returns boolean for traitementDisponiblePaysOrigine (true/false strict)', () => {
    expect(Rules.computeTraitementDisponiblePaysOrigine({
      aiData: { etrangerMaladeTraitementDisponible: true },
    })).toBe(true);
    expect(Rules.computeTraitementDisponiblePaysOrigine({
      aiData: { etrangerMaladeTraitementDisponible: false },
    })).toBe(false);
    // null / undefined / non-boolean values rejected
    expect(Rules.computeTraitementDisponiblePaysOrigine({
      aiData: { etrangerMaladeTraitementDisponible: null },
    })).toBeNull();
    expect(Rules.computeTraitementDisponiblePaysOrigine({
      aiData: { etrangerMaladeTraitementDisponible: 'true' as unknown as boolean },
    })).toBeNull();
  });

  it('returns avisOFII when whitelist FAVORABLE/DEFAVORABLE/EN_ATTENTE', () => {
    expect(Rules.computeAvisOFII({ aiData: { etrangerMaladeAvisOFII: 'FAVORABLE' } }))
      .toBe('FAVORABLE');
    expect(Rules.computeAvisOFII({ aiData: { etrangerMaladeAvisOFII: 'DEFAVORABLE' } }))
      .toBe('DEFAVORABLE');
    expect(Rules.computeAvisOFII({ aiData: { etrangerMaladeAvisOFII: 'EN_ATTENTE' } }))
      .toBe('EN_ATTENTE');
  });

  it('normalizes avisOFII case (favorable → FAVORABLE)', () => {
    expect(Rules.computeAvisOFII({
      aiData: { etrangerMaladeAvisOFII: 'favorable' as unknown as 'FAVORABLE' },
    })).toBe('FAVORABLE');
  });

  it('rejects avisOFII outside whitelist', () => {
    expect(Rules.computeAvisOFII({
      aiData: { etrangerMaladeAvisOFII: 'EN_COURS' as unknown as 'EN_ATTENTE' },
    })).toBeNull();
    expect(Rules.computeAvisOFII({ aiData: { etrangerMaladeAvisOFII: null } })).toBeNull();
  });

  it('returns dateAvisOFII when ISO date past or today', () => {
    expect(Rules.computeDateAvisOFII({ aiData: { etrangerMalaDateAvisOFII: '2026-04-01' } }))
      .toBe('2026-04-01');
  });

  it('rejects future or malformed dateAvisOFII', () => {
    expect(Rules.computeDateAvisOFII({ aiData: { etrangerMalaDateAvisOFII: '2099-12-31' } }))
      .toBeNull();
    expect(Rules.computeDateAvisOFII({ aiData: { etrangerMalaDateAvisOFII: 'not-a-date' } }))
      .toBeNull();
    expect(Rules.computeDateAvisOFII({ aiData: { etrangerMalaDateAvisOFII: '01/04/2026' } }))
      .toBeNull();
  });

  it('returns 4 when all 4 prefill fields are present', () => {
    const input = {
      aiData: {
        etrangerMaladePathologie: 'VIH stade C avec tuberculose résistante',
        etrangerMaladeTraitementDisponible: false,
        etrangerMaladeAvisOFII: 'DEFAVORABLE',
        etrangerMalaDateAvisOFII: '2026-04-01',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('does NOT count etrangerMaladeDetecte alone (signal global, pas champ saisissable)', () => {
    const input = { aiData: { etrangerMaladeDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
