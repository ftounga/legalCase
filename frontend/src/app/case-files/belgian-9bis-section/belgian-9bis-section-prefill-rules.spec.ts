import { Belgian9bisPrefillRules as Rules } from './belgian-9bis-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 * SF-246-20 : computeDateEntreeBelgique lit be9bisDateEntreeBelgique (ISO strict, non-future).
 *             computeDureePresenceMois lit be9bisDureePresenceMois (entier calculé backend).
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };
const FR = { workspaceCountry: 'FRANCE' as const };

describe('Belgian9bisPrefillRules — SF-246-20', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 0 when dateDepotProcedure missing or non-string (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: null } })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '' } })).toBe(0);
  });

  it('returns 1 when dateDepotProcedure is set in BE (M=N=1)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('computeDateDepotDemande returns string when set in BE', () => {
    expect(Rules.computeDateDepotDemande({ ...BE, aiData: { dateDepotProcedure: '2026-03-15' } })).toBe('2026-03-15');
  });

  // --- computeDateEntreeBelgique SF-246-20 : champ typé be9bisDateEntreeBelgique ---

  it('computeDateEntreeBelgique retourne null quand aiData absent (BE)', () => {
    expect(Rules.computeDateEntreeBelgique({ ...BE })).toBeNull();
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: null })).toBeNull();
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: {} })).toBeNull();
  });

  it('computeDateEntreeBelgique retourne null si be9bisDateEntreeBelgique absent (BE)', () => {
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: { dateDepotProcedure: '2019-01-01' } })).toBeNull();
  });

  it('computeDateEntreeBelgique retourne la date ISO valide (BE)', () => {
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: { be9bisDateEntreeBelgique: '2019-03-15' } as any })).toBe('2019-03-15');
  });

  it('computeDateEntreeBelgique retourne null pour format non-ISO (BE)', () => {
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: { be9bisDateEntreeBelgique: '15/03/2019' } as any })).toBeNull();
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: { be9bisDateEntreeBelgique: '2019-03' } as any })).toBeNull();
    expect(Rules.computeDateEntreeBelgique({ ...BE, aiData: { be9bisDateEntreeBelgique: '' } as any })).toBeNull();
  });

  // --- computeDureePresenceMois SF-246-20 : champ typé be9bisDureePresenceMois ---

  it('computeDureePresenceMois retourne null quand aiData absent (BE)', () => {
    expect(Rules.computeDureePresenceMois({ ...BE })).toBeNull();
    expect(Rules.computeDureePresenceMois({ ...BE, aiData: {} })).toBeNull();
  });

  it('computeDureePresenceMois retourne le nombre de mois quand typé (BE)', () => {
    expect(Rules.computeDureePresenceMois({ ...BE, aiData: { be9bisDureePresenceMois: 48 } as any })).toBe(48);
  });

  it('computeDureePresenceMois retourne null si be9bisDureePresenceMois est null ou NaN (BE)', () => {
    expect(Rules.computeDureePresenceMois({ ...BE, aiData: { be9bisDureePresenceMois: null } as any })).toBeNull();
    expect(Rules.computeDureePresenceMois({ ...BE, aiData: { be9bisDureePresenceMois: NaN } as any })).toBeNull();
  });

  it('computeDureePresenceMois retourne 0 (valeur valide pour entrée récente) (BE)', () => {
    expect(Rules.computeDureePresenceMois({ ...BE, aiData: { be9bisDureePresenceMois: 0 } as any })).toBe(0);
  });

  // --- computePrefillCount SF-246-20 : max 3 champs ---

  it('retourne 3 quand dateEntree + duree + dateDepot tous non-null (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be9bisDateEntreeBelgique: '2019-03-15',
        be9bisDureePresenceMois: 73,
        dateDepotProcedure: '2025-04-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('retourne 2 quand seulement dateEntree + duree (BE)', () => {
    const input = {
      ...BE,
      aiData: {
        be9bisDateEntreeBelgique: '2019-03-15',
        be9bisDureePresenceMois: 73,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  // --- gating BE-only ---

  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = { ...FR, aiData: { dateDepotProcedure: '2026-04-01', be9bisDateEntreeBelgique: '2019-03-15' } as any };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
    expect(Rules.computeDateEntreeBelgique(input)).toBeNull();
    expect(Rules.computeDureePresenceMois(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    const input = { aiData: { dateDepotProcedure: '2026-04-01', be9bisDateEntreeBelgique: '2019-03-15' } as any };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
