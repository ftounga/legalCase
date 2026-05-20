/**
 * F-237 SF-237-02 — Tests `TravailProcedurePrefillRules`.
 * SF-246-22 : mise à jour — suppression des casts `as any` sur les champs réels +
 * ajout des cas manquants du plan de test SF-246-22.
 */
import {
  TravailProcedurePrefillRules,
  computePrefillCount,
  computeTypeProcedure,
  computeDateDeclencheur,
} from './travail-procedure-section-prefill-rules';

describe('TravailProcedurePrefillRules', () => {
  it('cas 0 — retourne 0 quand vide', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
    expect(computePrefillCount({ aiData: undefined })).toBe(0);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computePrefillCount({ aiData: {} as any })).toBe(0);
  });

  it('cas M — partiel (date seule = 1/2, hors gating pays)', () => {
    expect(
      computePrefillCount({
        aiData: { dateDeclencheurProcedure: '2026-04-01' },
      }),
    ).toBe(1);
  });

  it('cas N — nominal 2/2 (FR + date)', () => {
    expect(
      computePrefillCount({
        aiData: {
          procedureTravailDetectee: 'PRUDHOMMES_FR',
          dateDeclencheurProcedure: '2026-04-01',
        },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(2);
  });

  // ── computeTypeProcedure — gating pays ─────────────────────────────────
  it('computeTypeProcedure — code FR + workspace FR → match', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe('PRUDHOMMES_FR');
  });

  it('computeTypeProcedure — code BE + workspace BE → match', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'TRIBUNAL_TRAVAIL_BE' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe('TRIBUNAL_TRAVAIL_BE');
  });

  it('computeTypeProcedure — code BE sur workspace FR → null (gating)', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'TRIBUNAL_TRAVAIL_BE' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — code FR + workspace BE → null (gating)', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — code inconnu → null', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'PAS_DANS_LE_SET_FR' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — code null → null', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: null },
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('computeTypeProcedure — default workspaceCountry = FRANCE', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' },
      }),
    ).toBe('PRUDHOMMES_FR');
  });

  // SF-246-22 : tous les 6 codes whitelistés (3 FR + 3 BE)
  it('computeTypeProcedure — APPEL_CA_SOCIALE_FR → match FR', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'APPEL_CA_SOCIALE_FR' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe('APPEL_CA_SOCIALE_FR');
  });

  it('computeTypeProcedure — CASSATION_SOCIALE_FR → match FR', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'CASSATION_SOCIALE_FR' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe('CASSATION_SOCIALE_FR');
  });

  it('computeTypeProcedure — COUR_TRAVAIL_BE → match BE', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'COUR_TRAVAIL_BE' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe('COUR_TRAVAIL_BE');
  });

  it('computeTypeProcedure — CASSATION_BE → match BE', () => {
    expect(
      computeTypeProcedure({
        aiData: { procedureTravailDetectee: 'CASSATION_BE' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe('CASSATION_BE');
  });

  // ── computeDateDeclencheur ─────────────────────────────────────────────
  it('computeDateDeclencheur — rejette non-ISO (format DD/MM/YYYY)', () => {
    expect(
      computeDateDeclencheur({ aiData: { dateDeclencheurProcedure: '01/04/2026' } }),
    ).toBeNull();
  });

  it('computeDateDeclencheur — rejette chaîne vide', () => {
    expect(
      computeDateDeclencheur({ aiData: { dateDeclencheurProcedure: '' } }),
    ).toBeNull();
  });

  it('computeDateDeclencheur — rejette null', () => {
    expect(
      computeDateDeclencheur({ aiData: { dateDeclencheurProcedure: null } }),
    ).toBeNull();
  });

  it('computeDateDeclencheur — accepte ISO valide', () => {
    expect(
      computeDateDeclencheur({ aiData: { dateDeclencheurProcedure: '2026-04-01' } }),
    ).toBe('2026-04-01');
  });

  it('computeDateDeclencheur — aiData absent → null', () => {
    expect(computeDateDeclencheur({})).toBeNull();
    expect(computeDateDeclencheur({ aiData: null })).toBeNull();
  });

  // ── computePrefillCount — cas 0 / 1 / 2 ──────────────────────────────
  it('computePrefillCount — 0 : aucun champ', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('computePrefillCount — 1 : type seul (FR + workspace FR)', () => {
    expect(
      computePrefillCount({
        aiData: { procedureTravailDetectee: 'PRUDHOMMES_FR' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('computePrefillCount — 2 : type + date (nominal)', () => {
    expect(
      computePrefillCount({
        aiData: {
          procedureTravailDetectee: 'PRUDHOMMES_FR',
          dateDeclencheurProcedure: '2026-03-01',
        },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(2);
  });

  it('computePrefillCount — 1 : code BE sur workspace FR + date → 1 (gating bloque type)', () => {
    expect(
      computePrefillCount({
        aiData: {
          procedureTravailDetectee: 'TRIBUNAL_TRAVAIL_BE',
          dateDeclencheurProcedure: '2026-03-01',
        },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('expose surface complète', () => {
    expect(TravailProcedurePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
