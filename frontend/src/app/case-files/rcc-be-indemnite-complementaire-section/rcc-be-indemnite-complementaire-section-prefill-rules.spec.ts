import { RccBeIndemniteComplementairePrefillRules as Rules } from './rcc-be-indemnite-complementaire-section-prefill-rules';

describe('RccBeIndemniteComplementairePrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when all fields malformed / hors normes', () => {
    const input = {
      aiData: {
        remunerationNetteReferenceRccDetectee: 0,        // <= 0 rejeté
        allocationOnemMensuelleEstimee: -1,              // < 0 rejeté
        dateNaissanceSalarie: '12/06/1965',              // non ISO
        dateDebutRccEnvisagee: '01-01-2026',             // non ISO
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only remunerationNetteReferenceRccDetectee is set', () => {
    const input = {
      aiData: { remunerationNetteReferenceRccDetectee: 2500 } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRemunerationNetteReference(input)).toBe(2500);
  });

  it('returns 1 when only allocationOnemMensuelleEstimee is set (positive)', () => {
    const input = { aiData: { allocationOnemMensuelleEstimee: 1500 } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeAllocationOnemMensuelle(input)).toBe(1500);
  });

  it('returns 1 when allocationOnemMensuelleEstimee = 0 (≥ 0 toléré)', () => {
    const input = { aiData: { allocationOnemMensuelleEstimee: 0 } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeAllocationOnemMensuelle(input)).toBe(0);
  });

  it('returns 1 when only dateNaissanceSalarie is set (ISO)', () => {
    const input = { aiData: { dateNaissanceSalarie: '1965-06-12' } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateNaissanceSalarie(input)).toBe('1965-06-12');
  });

  it('returns 1 when only dateDebutRccEnvisagee is set (ISO)', () => {
    const input = { aiData: { dateDebutRccEnvisagee: '2026-09-01' } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateDebutRcc(input)).toBe('2026-09-01');
  });

  // ── Cas 2 / 3 ─────────────────────────────────────────────────────────
  it('returns 2 when remun + alloc onem', () => {
    const input = {
      aiData: {
        remunerationNetteReferenceRccDetectee: 2500,
        allocationOnemMensuelleEstimee: 1500,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 3 when 3 fields set', () => {
    const input = {
      aiData: {
        remunerationNetteReferenceRccDetectee: 2500,
        allocationOnemMensuelleEstimee: 1500,
        dateNaissanceSalarie: '1965-06-12',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  // ── Cas 4 : nominal complet ──────────────────────────────────────────
  it('returns 4 when all instrumented fields are set', () => {
    const input = {
      aiData: {
        remunerationNetteReferenceRccDetectee: 2500,
        allocationOnemMensuelleEstimee: 1500,
        dateNaissanceSalarie: '1965-06-12',
        dateDebutRccEnvisagee: '2026-09-01',
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
    expect(Rules.computeRemunerationNetteReference(input)).toBe(2500);
    expect(Rules.computeAllocationOnemMensuelle(input)).toBe(1500);
    expect(Rules.computeDateNaissanceSalarie(input)).toBe('1965-06-12');
    expect(Rules.computeDateDebutRcc(input)).toBe('2026-09-01');
  });

  // ── remunerationNetteReference : > 0 strict ──────────────────────────
  describe('remunerationNetteReference — > 0 strict', () => {
    it('accepte 0.01 (limite inférieure)', () => {
      expect(Rules.computeRemunerationNetteReference({
        aiData: { remunerationNetteReferenceRccDetectee: 0.01 } as any,
      })).toBe(0.01);
    });

    it('rejette 0 → null', () => {
      expect(Rules.computeRemunerationNetteReference({
        aiData: { remunerationNetteReferenceRccDetectee: 0 } as any,
      })).toBeNull();
    });

    it('rejette négatif → null', () => {
      expect(Rules.computeRemunerationNetteReference({
        aiData: { remunerationNetteReferenceRccDetectee: -100 } as any,
      })).toBeNull();
    });

    it('rejette non-number / null / NaN → null', () => {
      expect(Rules.computeRemunerationNetteReference({ aiData: {} as any })).toBeNull();
      expect(Rules.computeRemunerationNetteReference({
        aiData: { remunerationNetteReferenceRccDetectee: '2500' } as any,
      })).toBeNull();
      expect(Rules.computeRemunerationNetteReference({
        aiData: { remunerationNetteReferenceRccDetectee: null } as any,
      })).toBeNull();
      expect(Rules.computeRemunerationNetteReference({
        aiData: { remunerationNetteReferenceRccDetectee: NaN } as any,
      })).toBeNull();
    });
  });

  // ── allocationOnemMensuelle : ≥ 0 toléré ─────────────────────────────
  describe('allocationOnemMensuelle — ≥ 0 toléré', () => {
    it('accepte 0 (borne inférieure)', () => {
      expect(Rules.computeAllocationOnemMensuelle({
        aiData: { allocationOnemMensuelleEstimee: 0 } as any,
      })).toBe(0);
    });

    it('rejette négatif → null', () => {
      expect(Rules.computeAllocationOnemMensuelle({
        aiData: { allocationOnemMensuelleEstimee: -1 } as any,
      })).toBeNull();
    });

    it('rejette non-number → null', () => {
      expect(Rules.computeAllocationOnemMensuelle({ aiData: {} as any })).toBeNull();
      expect(Rules.computeAllocationOnemMensuelle({
        aiData: { allocationOnemMensuelleEstimee: '1500' } as any,
      })).toBeNull();
    });
  });

  // ── dates : ISO strict ───────────────────────────────────────────────
  describe('dates — ISO strict', () => {
    it('rejette format non ISO (dateNaissance)', () => {
      expect(Rules.computeDateNaissanceSalarie({
        aiData: { dateNaissanceSalarie: '31/12/1962' } as any,
      })).toBeNull();
    });

    it('rejette date impossible (mois > 12) — dateDebutRcc', () => {
      expect(Rules.computeDateDebutRcc({
        aiData: { dateDebutRccEnvisagee: '2026-13-01' } as any,
      })).toBeNull();
    });

    it('rejette non-string → null', () => {
      expect(Rules.computeDateDebutRcc({
        aiData: { dateDebutRccEnvisagee: 20260901 } as any,
      })).toBeNull();
    });
  });
});
