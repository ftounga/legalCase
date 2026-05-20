import { Belgian9terPrefillRules as Rules } from './belgian-9ter-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 * SF-246-20 : computeDateDebutSymptomes lit be9terDateDebutSymptomes (ISO strict, non-future).
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };
const FR = { workspaceCountry: 'FRANCE' as const };

describe('Belgian9terPrefillRules — SF-246-20', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 0 for non-boolean toggles (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { maladieGraveCertifiee: 'oui' } as any })).toBe(0);
  });

  // --- computeDateDebutSymptomes SF-246-20 : champ typé be9terDateDebutSymptomes ---

  it('computeDateDebutSymptomes retourne null quand be9terDateDebutSymptomes absent (BE)', () => {
    expect(Rules.computeDateDebutSymptomes({ ...BE, aiData: {} })).toBeNull();
    expect(Rules.computeDateDebutSymptomes({ ...BE, aiData: { be9terDateDebutSymptomes: null } as any })).toBeNull();
  });

  it('computeDateDebutSymptomes retourne la date ISO valide (BE)', () => {
    expect(Rules.computeDateDebutSymptomes({ ...BE, aiData: { be9terDateDebutSymptomes: '2021-07-10' } as any })).toBe('2021-07-10');
  });

  it('computeDateDebutSymptomes retourne null pour format non-ISO (BE)', () => {
    expect(Rules.computeDateDebutSymptomes({ ...BE, aiData: { be9terDateDebutSymptomes: '10/07/2021' } as any })).toBeNull();
    expect(Rules.computeDateDebutSymptomes({ ...BE, aiData: { be9terDateDebutSymptomes: '' } as any })).toBeNull();
  });

  it('ne lit PAS dateDebutSymptomes (ancien champ aspirationnel) (BE)', () => {
    // SF-246-20 invariant : l'ancien champ aspirationnel ne doit plus être lu
    expect(Rules.computeDateDebutSymptomes({ ...BE, aiData: { dateDebutSymptomes: '2021-07-10' } as any })).toBeNull();
  });

  it('returns 1 when only be9terDateDebutSymptomes set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { be9terDateDebutSymptomes: '2021-07-10' } as any })).toBe(1);
  });

  it('returns 1 when only maladieGrave alias set (tolérance) (BE)', () => {
    expect(Rules.computeMaladieGraveCertifiee({ ...BE, aiData: { maladieGrave: true } as any })).toBe(true);
    expect(Rules.computePrefillCount({ ...BE, aiData: { maladieGrave: true } as any })).toBe(1);
  });

  it('returns 1 when only menaceOrdrePublic=false (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { menaceOrdrePublic: false } as any })).toBe(1);
  });

  it('returns 2 when be9terDateDebutSymptomes + dateDepotProcedure (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be9terDateDebutSymptomes: '2021-07-10',
        dateDepotProcedure: '2022-04-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('canonical maladieGraveCertifiee wins over alias maladieGrave (BE)', () => {
    const input = {
      ...BE,
      aiData: { maladieGraveCertifiee: false, maladieGrave: true } as any,
    };
    expect(Rules.computeMaladieGraveCertifiee(input)).toBe(false);
  });

  // --- gating BE-only ---

  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = {
      ...FR,
      aiData: {
        be9terDateDebutSymptomes: '2021-07-10',
        maladieGraveCertifiee: true,
        soinsNecessairesDisponiblesBe: false,
        soinsInaccessiblesPaysOrigine: true,
        menaceOrdrePublic: false,
        dateDepotProcedure: '2022-04-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDebutSymptomes(input)).toBeNull();
    expect(Rules.computeMaladieGraveCertifiee(input)).toBeNull();
    expect(Rules.computeSoinsNecessairesDisponiblesBe(input)).toBeNull();
    expect(Rules.computeSoinsInaccessiblesPaysOrigine(input)).toBeNull();
    expect(Rules.computeMenaceOrdrePublic(input)).toBeNull();
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    expect(Rules.computePrefillCount({ aiData: { be9terDateDebutSymptomes: '2021-07-10' } as any })).toBe(0);
  });
});
