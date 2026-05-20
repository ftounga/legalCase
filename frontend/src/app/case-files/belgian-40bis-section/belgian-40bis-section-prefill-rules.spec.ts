import { Belgian40bisPrefillRules as Rules } from './belgian-40bis-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 * SF-246-20 : computeLienFamilial lit be40bisLienFamilial (whitelist 40bis — 5 valeurs distinctes).
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };
const FR = { workspaceCountry: 'FRANCE' as const };

describe('Belgian40bisPrefillRules — SF-246-20', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  // --- computeLienFamilial SF-246-20 : champ typé be40bisLienFamilial ---

  it('computeLienFamilial retourne null quand be40bisLienFamilial absent (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: {} })).toBeNull();
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: null } as any })).toBeNull();
  });

  it('computeLienFamilial retourne CONJOINT (valeur 40bis valide) (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'CONJOINT' } as any })).toBe('CONJOINT');
  });

  it('computeLienFamilial retourne PARTENAIRE_ENREGISTRE (valeur 40bis — pas 40ter) (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'PARTENAIRE_ENREGISTRE' } as any })).toBe('PARTENAIRE_ENREGISTRE');
  });

  it('computeLienFamilial retourne DESCENDANT_MINEUR (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'DESCENDANT_MINEUR' } as any })).toBe('DESCENDANT_MINEUR');
  });

  it('computeLienFamilial retourne DESCENDANT_MAJEUR_CHARGE (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'DESCENDANT_MAJEUR_CHARGE' } as any })).toBe('DESCENDANT_MAJEUR_CHARGE');
  });

  it('computeLienFamilial retourne ASCENDANT_CHARGE (valeur 40bis — pas 40ter) (BE)', () => {
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'ASCENDANT_CHARGE' } as any })).toBe('ASCENDANT_CHARGE');
  });

  it('computeLienFamilial retourne null pour PARTENAIRE_LEGAL_ENREGISTRE (valeur 40ter uniquement) (BE)', () => {
    // Invariant SF-246-20 : whitelists distinctes — code 40ter rejeté ici
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'PARTENAIRE_LEGAL_ENREGISTRE' } as any })).toBeNull();
  });

  it('computeLienFamilial retourne null pour ASCENDANT_CHARGE_HANDICAP (valeur 40ter uniquement) (BE)', () => {
    // Invariant SF-246-20 : whitelists distinctes — code 40ter rejeté ici
    expect(Rules.computeLienFamilial({ ...BE, aiData: { be40bisLienFamilial: 'ASCENDANT_CHARGE_HANDICAP' } as any })).toBeNull();
  });

  it('ne lit PAS lienFamilialBe ni lienFamilial (anciens champs aspirationnels) (BE)', () => {
    // Invariant SF-246-20 : anciens alias aspirationnels ne sont plus lus
    expect(Rules.computeLienFamilial({ ...BE, aiData: { lienFamilialBe: 'CONJOINT' } as any })).toBeNull();
    expect(Rules.computeLienFamilial({ ...BE, aiData: { lienFamilial: 'CONJOINT' } as any })).toBeNull();
  });

  // --- computePrefillCount SF-246-20 : max 3 champs ---

  it('returns 1 when only be40bisLienFamilial set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { be40bisLienFamilial: 'CONJOINT' } as any })).toBe(1);
  });

  it('returns 1 when only dateDepotProcedure is set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('returns 1 when only nationaliteUe is set (false) (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { nationaliteUe: false } })).toBe(1);
    expect(Rules.computeRegroupantCitoyenUe({ ...BE, aiData: { nationaliteUe: false } })).toBe(false);
  });

  it('returns 3 when lienFamilial + dateDepot + nationaliteUe tous présents (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be40bisLienFamilial: 'DESCENDANT_MINEUR',
        dateDepotProcedure: '2026-04-01',
        nationaliteUe: true,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('returns 2 when lienFamilial + dateDepot seulement (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be40bisLienFamilial: 'CONJOINT',
        dateDepotProcedure: '2026-04-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 0 when nationaliteUe is not boolean (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { nationaliteUe: 'true' } })).toBe(0);
  });

  // --- gating BE-only ---

  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = {
      ...FR,
      aiData: {
        be40bisLienFamilial: 'CONJOINT',
        dateDepotProcedure: '2026-04-01',
        nationaliteUe: true,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeLienFamilial(input)).toBeNull();
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
    expect(Rules.computeRegroupantCitoyenUe(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    expect(Rules.computePrefillCount({ aiData: { be40bisLienFamilial: 'CONJOINT', dateDepotProcedure: '2026-04-01' } as any })).toBe(0);
  });
});
