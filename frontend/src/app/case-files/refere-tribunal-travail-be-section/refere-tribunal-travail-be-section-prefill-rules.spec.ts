import { RefereTribunalTravailBePrefillRules as Rules } from './refere-tribunal-travail-be-section-prefill-rules';

describe('RefereTribunalTravailBePrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when all fields malformed / hors whitelist', () => {
    const input = {
      aiData: {
        motifUrgenceDetecte: 'INCONNU',          // hors whitelist
        dateFaitGenerateurUrgence: '15/03/2025', // non ISO
        perilImmediatPresume: false,             // false → null pré-fill
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only motifUrgenceDetecte is set (whitelist)', () => {
    const input = { aiData: { motifUrgenceDetecte: 'HARCELEMENT' } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeMotifUrgence(input)).toBe('HARCELEMENT');
  });

  it('returns 1 when only dateFaitGenerateurUrgence is set (ISO)', () => {
    const input = { aiData: { dateFaitGenerateurUrgence: '2025-03-15' } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateFaitGenerateur(input)).toBe('2025-03-15');
  });

  it('returns 1 when only perilImmediatPresume === true', () => {
    const input = { aiData: { perilImmediatPresume: true } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computePerilEnDemeure(input)).toBe(true);
  });

  // ── Cas 2 : deux champs ──────────────────────────────────────────────
  it('returns 2 when 2 fields are set', () => {
    const input = {
      aiData: {
        motifUrgenceDetecte: 'SALAIRE_IMPAYE',
        dateFaitGenerateurUrgence: '2025-04-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  // ── Cas 3 : nominal complet ──────────────────────────────────────────
  it('returns 3 when all instrumented fields are set', () => {
    const input = {
      aiData: {
        motifUrgenceDetecte: 'MODIFICATION_UNILATERALE',
        dateFaitGenerateurUrgence: '2025-04-01',
        perilImmediatPresume: true,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
    expect(Rules.computeMotifUrgence(input)).toBe('MODIFICATION_UNILATERALE');
    expect(Rules.computeDateFaitGenerateur(input)).toBe('2025-04-01');
    expect(Rules.computePerilEnDemeure(input)).toBe(true);
  });

  // ── motifUrgence : whitelist stricte ──────────────────────────────────
  describe('motifUrgence — whitelist 4 codes', () => {
    it('accepte les 4 codes officiels', () => {
      const codes = ['HARCELEMENT', 'SALAIRE_IMPAYE', 'MODIFICATION_UNILATERALE', 'AUTRE'];
      codes.forEach(c => {
        expect(Rules.computeMotifUrgence({ aiData: { motifUrgenceDetecte: c } as any })).toBe(c);
      });
    });

    it('normalise la casse (LLM peut renvoyer lower-case)', () => {
      expect(Rules.computeMotifUrgence({
        aiData: { motifUrgenceDetecte: 'harcelement' } as any,
      })).toBe('HARCELEMENT');
      expect(Rules.computeMotifUrgence({
        aiData: { motifUrgenceDetecte: '  salaire_impaye  ' } as any,
      })).toBe('SALAIRE_IMPAYE');
    });

    it('rejette les valeurs hors whitelist → null', () => {
      expect(Rules.computeMotifUrgence({
        aiData: { motifUrgenceDetecte: 'LICENCIEMENT_ABUSIF' } as any,
      })).toBeNull();
      expect(Rules.computeMotifUrgence({
        aiData: { motifUrgenceDetecte: 'DISCRIMINATION' } as any,
      })).toBeNull();
    });

    it('rejette valeurs non-string / vides → null', () => {
      expect(Rules.computeMotifUrgence({ aiData: { motifUrgenceDetecte: null } as any })).toBeNull();
      expect(Rules.computeMotifUrgence({ aiData: { motifUrgenceDetecte: 42 } as any })).toBeNull();
      expect(Rules.computeMotifUrgence({ aiData: { motifUrgenceDetecte: '' } as any })).toBeNull();
      expect(Rules.computeMotifUrgence({ aiData: { motifUrgenceDetecte: '   ' } as any })).toBeNull();
    });
  });

  // ── dateFaitGenerateur : ISO strict ──────────────────────────────────
  describe('dateFaitGenerateur — ISO strict', () => {
    it('ISO YYYY-MM-DD valide → conservé', () => {
      expect(Rules.computeDateFaitGenerateur({
        aiData: { dateFaitGenerateurUrgence: '2025-03-15' } as any,
      })).toBe('2025-03-15');
    });

    it('format non ISO → null', () => {
      expect(Rules.computeDateFaitGenerateur({
        aiData: { dateFaitGenerateurUrgence: '15/03/2025' } as any,
      })).toBeNull();
      expect(Rules.computeDateFaitGenerateur({
        aiData: { dateFaitGenerateurUrgence: '15-03-2025' } as any,
      })).toBeNull();
    });

    it('date impossible (mois > 12) → null', () => {
      expect(Rules.computeDateFaitGenerateur({
        aiData: { dateFaitGenerateurUrgence: '2025-13-99' } as any,
      })).toBeNull();
    });

    it('non-string / null / vide → null', () => {
      expect(Rules.computeDateFaitGenerateur({ aiData: {} as any })).toBeNull();
      expect(Rules.computeDateFaitGenerateur({
        aiData: { dateFaitGenerateurUrgence: null } as any,
      })).toBeNull();
      expect(Rules.computeDateFaitGenerateur({
        aiData: { dateFaitGenerateurUrgence: 20250315 } as any,
      })).toBeNull();
    });
  });

  // ── perilEnDemeure : true only ───────────────────────────────────────
  describe('perilEnDemeure — pré-fill positif uniquement', () => {
    it('true → true', () => {
      expect(Rules.computePerilEnDemeure({
        aiData: { perilImmediatPresume: true } as any,
      })).toBe(true);
    });

    it('false → null (par défaut décoché)', () => {
      expect(Rules.computePerilEnDemeure({
        aiData: { perilImmediatPresume: false } as any,
      })).toBeNull();
    });

    it('null / undefined / non-bool → null', () => {
      expect(Rules.computePerilEnDemeure({ aiData: {} as any })).toBeNull();
      expect(Rules.computePerilEnDemeure({
        aiData: { perilImmediatPresume: null } as any,
      })).toBeNull();
      expect(Rules.computePerilEnDemeure({
        aiData: { perilImmediatPresume: 'oui' } as any,
      })).toBeNull();
      expect(Rules.computePerilEnDemeure({
        aiData: { perilImmediatPresume: 1 } as any,
      })).toBeNull();
    });
  });
});
