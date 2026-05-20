import { Belgian40terPrefillRules as Rules } from './belgian-40ter-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 * SF-246-20 : computeLienFamilial lit be40terLienFamilial (whitelist 40ter — 5 valeurs distinctes).
 *             computeRevenusMensuelsNets lit be40terRevenusMensuelsNets (> 0, ≤ 30 000).
 *             computeDateDepotDemande lit dateDepotProcedure (champ typé, alias supprimé).
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };
const FR = { workspaceCountry: 'FRANCE' as const };

describe('Belgian40terPrefillRules — SF-246-20', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  // --- computeLienFamilial SF-246-20 : champ typé be40terLienFamilial ---

  it('computeLienFamilial retourne null quand be40terLienFamilial absent (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: {} })).toBeNull();
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40terLienFamilial: null } as any })).toBeNull();
  });

  it('computeLienFamilial retourne CONJOINT (valeur 40ter valide) (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40terLienFamilial: 'CONJOINT' } as any })).toBe('CONJOINT');
  });

  it('computeLienFamilial retourne PARTENAIRE_LEGAL_ENREGISTRE (valeur 40ter — pas 40bis) (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40terLienFamilial: 'PARTENAIRE_LEGAL_ENREGISTRE' } as any })).toBe('PARTENAIRE_LEGAL_ENREGISTRE');
  });

  it('computeLienFamilial retourne ASCENDANT_CHARGE_HANDICAP (valeur 40ter — pas 40bis) (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40terLienFamilial: 'ASCENDANT_CHARGE_HANDICAP' } as any })).toBe('ASCENDANT_CHARGE_HANDICAP');
  });

  it('computeLienFamilial retourne null pour PARTENAIRE_ENREGISTRE (valeur 40bis uniquement) (BE)', () => {
    // Invariant SF-246-20 : whitelists distinctes — code 40bis rejeté ici
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40terLienFamilial: 'PARTENAIRE_ENREGISTRE' } as any })).toBeNull();
  });

  it('computeLienFamilial retourne null pour ASCENDANT_CHARGE (valeur 40bis uniquement) (BE)', () => {
    // Invariant SF-246-20 : whitelists distinctes — code 40bis rejeté ici
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40terLienFamilial: 'ASCENDANT_CHARGE' } as any })).toBeNull();
  });

  it('ne lit PAS lienFamilialBe ni lienFamilial (anciens champs aspirationnels) (BE)', () => {
    // Invariant SF-246-20 : anciens alias aspirationnels ne sont plus lus
    expect(Rules.computeLienFamilial({ ...BE, aiData: { lienFamilialBe: 'CONJOINT' } as any })).toBeNull();
    expect(Rules.computeLienFamilial({ ...BE, aiData: { lienFamilial: 'CONJOINT' } as any })).toBeNull();
  });

  // --- computeRevenusMensuelsNets SF-246-20 : champ typé be40terRevenusMensuelsNets ---

  it('computeRevenusMensuelsNets retourne null quand absent (BE)', () => {
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: {} })).toBeNull();
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: null } as any })).toBeNull();
  });

  it('computeRevenusMensuelsNets retourne la valeur > 0 et ≤ 30 000 (BE)', () => {
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: 2500 } as any })).toBe(2500);
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: 1 } as any })).toBe(1);
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: 30000 } as any })).toBe(30000);
  });

  it('computeRevenusMensuelsNets retourne null pour 0 (BE)', () => {
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: 0 } as any })).toBeNull();
  });

  it('computeRevenusMensuelsNets retourne null pour valeur > 30 000 (BE)', () => {
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: 50000 } as any })).toBeNull();
  });

  it('computeRevenusMensuelsNets retourne null pour valeur négative ou NaN (BE)', () => {
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: -100 } as any })).toBeNull();
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { be40terRevenusMensuelsNets: NaN } as any })).toBeNull();
  });

  it('ne lit PAS revenusNetsMensuels ni revenusMensuelsNets (anciens alias aspirationnels) (BE)', () => {
    // Invariant SF-246-20 : anciens alias ne sont plus lus
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { revenusNetsMensuels: 2500 } as any })).toBeNull();
    expect(Rules.computeRevenusMensuelsNets({ ...BE, aiData: { revenusMensuelsNets: 2500 } as any })).toBeNull();
  });

  // --- computeDateDepotDemande SF-246-20 : champ typé dateDepotProcedure ---

  it('computeDateDepotDemande retourne la date passée (BE)', () => {
    expect(Rules.computeDateDepotDemande({ ...BE, aiData: { dateDepotProcedure: '2026-01-01' } })).toBe('2026-01-01');
  });

  it('computeDateDepotDemande retourne null pour date future (BE)', () => {
    expect(Rules.computeDateDepotDemande({ ...BE, aiData: { dateDepotProcedure: '2099-12-31' } })).toBeNull();
  });

  it('computeDateDepotDemande retourne null quand absent (BE)', () => {
    expect(Rules.computeDateDepotDemande({ ...BE, aiData: {} })).toBeNull();
  });

  // --- computePrefillCount SF-246-20 : max 4 champs ---

  it('returns 1 when only be40terLienFamilial set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { be40terLienFamilial: 'CONJOINT' } as any })).toBe(1);
  });

  it('returns 1 when only be40terRevenusMensuelsNets > 0 (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { be40terRevenusMensuelsNets: 1500 } as any })).toBe(1);
  });

  it('returns 1 when only dateDepotProcedure présent (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '2026-01-01' } })).toBe(1);
  });

  it('returns 4 when lienFamilial + regroupantBelge + revenus + dateDepot (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be40terLienFamilial: 'CONJOINT',
        regroupantBelge: true,
        be40terRevenusMensuelsNets: 2000,
        dateDepotProcedure: '2026-01-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('returns 2 when lienFamilial + revenus seulement (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be40terLienFamilial: 'DESCENDANT_MINEUR',
        be40terRevenusMensuelsNets: 3000,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 0 when revenus <= 0 ou > 30000 (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { be40terRevenusMensuelsNets: 0 } as any })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { be40terRevenusMensuelsNets: -100 } as any })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { be40terRevenusMensuelsNets: 50000 } as any })).toBe(0);
  });

  // --- gating BE-only ---

  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = {
      ...FR,
      aiData: {
        be40terLienFamilial: 'CONJOINT',
        regroupantBelge: true,
        be40terRevenusMensuelsNets: 2000,
        dateDepotProcedure: '2026-01-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeLienFamilial(input)).toBeNull();
    expect(Rules.computeRegroupantBelge(input)).toBeNull();
    expect(Rules.computeRevenusMensuelsNets(input)).toBeNull();
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    expect(Rules.computePrefillCount({ aiData: { be40terLienFamilial: 'CONJOINT', be40terRevenusMensuelsNets: 2000 } as any })).toBe(0);
  });
});
