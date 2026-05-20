import { RccBeConditionsPrefillRules as Rules } from './rcc-be-conditions-section-prefill-rules';

describe('RccBeConditionsPrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when all fields malformed / hors normes', () => {
    const input = {
      aiData: {
        dateNaissanceSalarie: '12/06/1965',  // non ISO
        anneesCarriereSalarie: 120,          // hors borne
        metierLourdDetecte: false,           // false → null
        entrepriseEnDifficulteDetectee: false, // false → null
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only dateNaissanceSalarie is set (ISO)', () => {
    const input = { aiData: { dateNaissanceSalarie: '1965-06-12' } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateNaissance(input)).toBe('1965-06-12');
  });

  it('returns 1 when only anneesCarriereSalarie is set (in range)', () => {
    const input = { aiData: { anneesCarriereSalarie: 38 } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeAnneesCarriere(input)).toBe(38);
  });

  it('returns 1 when only metierLourdDetecte === true', () => {
    const input = { aiData: { metierLourdDetecte: true } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeMetierLourd(input)).toBe(true);
  });

  it('returns 1 when only entrepriseEnDifficulteDetectee === true', () => {
    const input = { aiData: { entrepriseEnDifficulteDetectee: true } as any };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeEntrepriseEnDifficulte(input)).toBe(true);
  });

  // ── Cas 2 : deux champs ──────────────────────────────────────────────
  it('returns 2 when dateNaissance + anneesCarriere', () => {
    const input = {
      aiData: {
        dateNaissanceSalarie: '1964-03-15',
        anneesCarriereSalarie: 41,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  // ── Cas 3 : trois champs ─────────────────────────────────────────────
  it('returns 3 when 3 fields are set', () => {
    const input = {
      aiData: {
        dateNaissanceSalarie: '1965-01-01',
        anneesCarriereSalarie: 35,
        metierLourdDetecte: true,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  // ── Cas 4 : nominal complet ──────────────────────────────────────────
  it('returns 4 when all instrumented fields are set', () => {
    const input = {
      aiData: {
        dateNaissanceSalarie: '1965-06-12',
        anneesCarriereSalarie: 40,
        metierLourdDetecte: true,
        entrepriseEnDifficulteDetectee: true,
      } as any,
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
    expect(Rules.computeDateNaissance(input)).toBe('1965-06-12');
    expect(Rules.computeAnneesCarriere(input)).toBe(40);
    expect(Rules.computeMetierLourd(input)).toBe(true);
    expect(Rules.computeEntrepriseEnDifficulte(input)).toBe(true);
  });

  // ── dateNaissance : ISO strict ───────────────────────────────────────
  describe('dateNaissance — ISO strict', () => {
    it('ISO YYYY-MM-DD valide → conservé', () => {
      expect(Rules.computeDateNaissance({
        aiData: { dateNaissanceSalarie: '1962-12-31' } as any,
      })).toBe('1962-12-31');
    });

    it('format non ISO → null', () => {
      expect(Rules.computeDateNaissance({
        aiData: { dateNaissanceSalarie: '31/12/1962' } as any,
      })).toBeNull();
      expect(Rules.computeDateNaissance({
        aiData: { dateNaissanceSalarie: '31-12-1962' } as any,
      })).toBeNull();
    });

    it('date impossible (mois > 12) → null', () => {
      expect(Rules.computeDateNaissance({
        aiData: { dateNaissanceSalarie: '1965-13-01' } as any,
      })).toBeNull();
    });

    it('non-string / null / vide → null', () => {
      expect(Rules.computeDateNaissance({ aiData: {} as any })).toBeNull();
      expect(Rules.computeDateNaissance({
        aiData: { dateNaissanceSalarie: null } as any,
      })).toBeNull();
      expect(Rules.computeDateNaissance({
        aiData: { dateNaissanceSalarie: 19650612 } as any,
      })).toBeNull();
    });
  });

  // ── anneesCarriere : bornes 0-60 ─────────────────────────────────────
  describe('anneesCarriere — bornes [0, 60]', () => {
    it('accepte 0 (borne inférieure)', () => {
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: 0 } as any,
      })).toBe(0);
    });

    it('accepte 60 (borne supérieure)', () => {
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: 60 } as any,
      })).toBe(60);
    });

    it('rejette -1 (sous borne) → null', () => {
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: -1 } as any,
      })).toBeNull();
    });

    it('rejette 61 (au-dessus borne) → null', () => {
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: 61 } as any,
      })).toBeNull();
    });

    it('rejette les non-entiers (décimal) → null', () => {
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: 38.5 } as any,
      })).toBeNull();
    });

    it('rejette non-number / null → null', () => {
      expect(Rules.computeAnneesCarriere({ aiData: {} as any })).toBeNull();
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: '40' } as any,
      })).toBeNull();
      expect(Rules.computeAnneesCarriere({
        aiData: { anneesCarriereSalarie: null } as any,
      })).toBeNull();
    });
  });

  // ── metierLourd : true only ──────────────────────────────────────────
  describe('metierLourd — pré-fill positif uniquement', () => {
    it('true → true', () => {
      expect(Rules.computeMetierLourd({
        aiData: { metierLourdDetecte: true } as any,
      })).toBe(true);
    });

    it('false → null (par défaut décoché)', () => {
      expect(Rules.computeMetierLourd({
        aiData: { metierLourdDetecte: false } as any,
      })).toBeNull();
    });

    it('null / undefined / non-bool → null', () => {
      expect(Rules.computeMetierLourd({ aiData: {} as any })).toBeNull();
      expect(Rules.computeMetierLourd({
        aiData: { metierLourdDetecte: null } as any,
      })).toBeNull();
      expect(Rules.computeMetierLourd({
        aiData: { metierLourdDetecte: 'oui' } as any,
      })).toBeNull();
      expect(Rules.computeMetierLourd({
        aiData: { metierLourdDetecte: 1 } as any,
      })).toBeNull();
    });
  });

  // ── entrepriseEnDifficulte : true only ───────────────────────────────
  describe('entrepriseEnDifficulte — pré-fill positif uniquement', () => {
    it('true → true', () => {
      expect(Rules.computeEntrepriseEnDifficulte({
        aiData: { entrepriseEnDifficulteDetectee: true } as any,
      })).toBe(true);
    });

    it('false → null', () => {
      expect(Rules.computeEntrepriseEnDifficulte({
        aiData: { entrepriseEnDifficulteDetectee: false } as any,
      })).toBeNull();
    });

    it('null / undefined / non-bool → null', () => {
      expect(Rules.computeEntrepriseEnDifficulte({ aiData: {} as any })).toBeNull();
      expect(Rules.computeEntrepriseEnDifficulte({
        aiData: { entrepriseEnDifficulteDetectee: null } as any,
      })).toBeNull();
      expect(Rules.computeEntrepriseEnDifficulte({
        aiData: { entrepriseEnDifficulteDetectee: 'oui' } as any,
      })).toBeNull();
    });
  });
});
