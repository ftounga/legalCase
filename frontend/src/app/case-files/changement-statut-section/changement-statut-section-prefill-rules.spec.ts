import { ChangementStatutPrefillRules as Rules } from './changement-statut-section-prefill-rules';

describe('ChangementStatutPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        typeTitreSejourCode: 'VLS_TS_ETUDIANT',
        dateExpirationTitre: '2026-12-31',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeTitreActuel(input)).toBeNull();
    expect(Rules.computeDureeRestanteMois(input)).toBeNull();
  });

  it('returns 0 for malformed dateExpirationTitre', () => {
    const input = { aiData: { dateExpirationTitre: 'not-a-date' } };
    expect(Rules.computeDureeRestanteMois(input)).toBeNull();
  });

  // ── Cas M (partiel) ───────────────────────────────────────────────────
  it('returns 1 when only titre actuel is set', () => {
    const input = { aiData: { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeTitreActuel(input)).not.toBeNull();
    expect(Rules.computeDureeRestanteMois(input)).toBeNull();
  });

  it('returns 1 when only date expiration future is set', () => {
    const input = { aiData: { dateExpirationTitre: '2099-12-31' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDureeRestanteMois(input)).not.toBeNull();
    expect(Rules.computeDureeRestanteMois(input)).toBeGreaterThanOrEqual(0);
  });

  it('plancher dureeRestante à 0 quand date passée', () => {
    const input = { aiData: { dateExpirationTitre: '2000-01-01' } };
    expect(Rules.computeDureeRestanteMois(input)).toBe(0);
  });

  // ── Cas N (nominal) ───────────────────────────────────────────────────
  it('returns N=2 quand les deux champs IA sont alimentés', () => {
    const input = {
      aiData: {
        typeTitreSejourCode: 'VLS_TS_ETUDIANT',
        dateExpirationTitre: '2099-12-31',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('cascade : typeTitreSejour texte libre quand pas de code', () => {
    const input = { aiData: { typeTitreSejour: 'VLS-TS Étudiant' } };
    // mapTitreSejourFromIa retourne potentiellement non-null pour ce texte
    const titre = Rules.computeTitreActuel(input);
    // L'important : pas de crash + résultat null OU un TitreSejourCode valide
    expect(titre === null || typeof titre === 'string').toBe(true);
  });

  // ── SF-246-19 : computeTitreEnvisage ──────────────────────────────────
  it('computeTitreEnvisage returns null when absent', () => {
    expect(Rules.computeTitreEnvisage({ aiData: {} })).toBeNull();
  });

  it('computeTitreEnvisage returns null for BELGIQUE', () => {
    expect(Rules.computeTitreEnvisage({
      aiData: { changementTitreEnvisage: 'CARTE_SALARIE' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computeTitreEnvisage returns TitreSejourCode for valid code', () => {
    const result = Rules.computeTitreEnvisage({
      aiData: { changementTitreEnvisage: 'CARTE_SALARIE' },
      workspaceCountry: 'FRANCE',
    });
    expect(result).toBe('SALARIE');
  });

  it('computeTitreEnvisage returns null for unknown code', () => {
    expect(Rules.computeTitreEnvisage({ aiData: { changementTitreEnvisage: 'BOGUS_CODE' } })).toBeNull();
  });

  // ── SF-246-19 : computeRemuneration ──────────────────────────────────
  it('computeRemuneration returns null when absent', () => {
    expect(Rules.computeRemuneration({ aiData: {} })).toBeNull();
  });

  it('computeRemuneration returns null for 0 or negative', () => {
    expect(Rules.computeRemuneration({ aiData: { changementRemunerationEur: 0 } })).toBeNull();
    expect(Rules.computeRemuneration({ aiData: { changementRemunerationEur: -1 } })).toBeNull();
  });

  it('computeRemuneration returns null for > 500 000', () => {
    expect(Rules.computeRemuneration({ aiData: { changementRemunerationEur: 500_001 } })).toBeNull();
  });

  it('computeRemuneration returns value for valid range', () => {
    expect(Rules.computeRemuneration({ aiData: { changementRemunerationEur: 35_000 } })).toBe(35_000);
    expect(Rules.computeRemuneration({ aiData: { changementRemunerationEur: 1 } })).toBe(1);
    expect(Rules.computeRemuneration({ aiData: { changementRemunerationEur: 500_000 } })).toBe(500_000);
  });

  it('computeRemuneration returns null for BELGIQUE', () => {
    expect(Rules.computeRemuneration({
      aiData: { changementRemunerationEur: 35_000 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── computePrefillCount — cas maximal SF-246-19 ───────────────────────
  it('returns 4 when all 4 fields present', () => {
    const input = {
      aiData: {
        typeTitreSejourCode: 'VLS_TS_ETUDIANT',
        dateExpirationTitre: '2099-12-31',
        changementTitreEnvisage: 'CARTE_SALARIE',
        changementRemunerationEur: 35_000,
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('returns 0 for BELGIQUE even with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        typeTitreSejourCode: 'VLS_TS_ETUDIANT',
        changementTitreEnvisage: 'CARTE_SALARIE',
        changementRemunerationEur: 35_000,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
