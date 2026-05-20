import { OutplacementBeObligatoire45PrefillRules as Rules } from './outplacement-be-obligatoire-45-section-prefill-rules';

describe('OutplacementBeObligatoire45PrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when fields are all malformed / empty', () => {
    const input = {
      aiData: {
        dateLicenciement: '15/03/2025',                    // non ISO
        dateNaissanceSalarie: 'invalide',                  // non ISO
        ancienneteSalarie: -1,                             // négatif
        motifLicenciementDetecte: 'AUTRE_VALEUR',          // hors whitelist
        offreOutplacementMentionnee: false,                // false ≠ true → null
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ ────────────────────────────────────────────
  it('returns 1 when only dateLicenciement is set (ISO)', () => {
    const input = { aiData: { dateLicenciement: '2025-03-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateLicenciement(input)).toBe('2025-03-15');
  });

  it('returns 1 when only motif is set (whitelist)', () => {
    const input = { aiData: { motifLicenciementDetecte: 'LICENCIEMENT_ECONOMIQUE' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeMotifLicenciement(input)).toBe('LICENCIEMENT_ECONOMIQUE');
  });

  // ── Cas 5 : nominal complet ───────────────────────────────────────────
  it('returns 5 when all 5 instrumented fields are set', () => {
    const input = {
      aiData: {
        dateLicenciement: '2025-03-15',
        dateNaissanceSalarie: '1975-06-12',
        ancienneteSalarie: 12.5,
        motifLicenciementDetecte: 'LICENCIEMENT_AUTRE',
        offreOutplacementMentionnee: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(5);
    expect(Rules.computeAncienneteAnnees(input)).toBe(12.5);
    expect(Rules.computeOffreOutplacementRecue(input)).toBe(true);
  });

  // ── Whitelist motif ────────────────────────────────────────────────────
  describe('motifLicenciement — whitelist stricte', () => {
    it('LICENCIEMENT_ECONOMIQUE → conservé', () => {
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: 'LICENCIEMENT_ECONOMIQUE' },
      })).toBe('LICENCIEMENT_ECONOMIQUE');
    });

    it('LICENCIEMENT_AUTRE → conservé', () => {
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: 'LICENCIEMENT_AUTRE' },
      })).toBe('LICENCIEMENT_AUTRE');
    });

    it('FAUTE_GRAVE → conservé', () => {
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: 'FAUTE_GRAVE' },
      })).toBe('FAUTE_GRAVE');
    });

    it('DEMISSION → conservé', () => {
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: 'DEMISSION' },
      })).toBe('DEMISSION');
    });

    it('valeur hors whitelist → null', () => {
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: 'AUTRE' },
      })).toBeNull();
    });

    it('casse incorrecte → null (pas de normalisation, parité backend stricte)', () => {
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: 'licenciement_autre' },
      })).toBeNull();
    });

    it('null / undefined / vide → null', () => {
      expect(Rules.computeMotifLicenciement({ aiData: {} })).toBeNull();
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: '' },
      })).toBeNull();
      expect(Rules.computeMotifLicenciement({
        aiData: { motifLicenciementDetecte: null as unknown as string },
      })).toBeNull();
    });
  });

  // ── offreOutplacementMentionnee : true only ──────────────────────────
  describe('offreOutplacementRecue — true only pré-fill', () => {
    it('true → true', () => {
      expect(Rules.computeOffreOutplacementRecue({
        aiData: { offreOutplacementMentionnee: true },
      })).toBe(true);
    });

    it('false → null (case décochée par défaut, pas de badge IA)', () => {
      expect(Rules.computeOffreOutplacementRecue({
        aiData: { offreOutplacementMentionnee: false },
      })).toBeNull();
    });

    it('null → null', () => {
      expect(Rules.computeOffreOutplacementRecue({
        aiData: { offreOutplacementMentionnee: null as unknown as boolean },
      })).toBeNull();
    });

    it('valeur non-boolean → null', () => {
      expect(Rules.computeOffreOutplacementRecue({
        aiData: { offreOutplacementMentionnee: 'oui' as unknown as boolean },
      })).toBeNull();
    });
  });

  // ── Dates ISO ────────────────────────────────────────────────────────
  it('dates non ISO → null (parité backend LocalDate)', () => {
    expect(Rules.computeDateLicenciement({
      aiData: { dateLicenciement: '15/03/2025' },
    })).toBeNull();
    expect(Rules.computeDateNaissanceSalarie({
      aiData: { dateNaissanceSalarie: '1975/06/12' },
    })).toBeNull();
    expect(Rules.computeDateLicenciement({
      aiData: { dateLicenciement: '2025-13-99' },
    })).toBeNull();
  });

  // ── ancienneté ≥ 0 ────────────────────────────────────────────────────
  it('ancienneté négative → null ; 0 toléré ; décimales OK', () => {
    expect(Rules.computeAncienneteAnnees({ aiData: { ancienneteSalarie: -0.5 } })).toBeNull();
    expect(Rules.computeAncienneteAnnees({ aiData: { ancienneteSalarie: 0 } })).toBe(0);
    expect(Rules.computeAncienneteAnnees({ aiData: { ancienneteSalarie: 0.5 } })).toBe(0.5);
    expect(Rules.computeAncienneteAnnees({ aiData: { ancienneteSalarie: 25.75 } })).toBe(25.75);
  });

  it('ancienneté non-number → null', () => {
    expect(Rules.computeAncienneteAnnees({
      aiData: { ancienneteSalarie: '12' as unknown as number },
    })).toBeNull();
    expect(Rules.computeAncienneteAnnees({
      aiData: { ancienneteSalarie: NaN },
    })).toBeNull();
    expect(Rules.computeAncienneteAnnees({
      aiData: { ancienneteSalarie: Number.POSITIVE_INFINITY },
    })).toBeNull();
  });
});
